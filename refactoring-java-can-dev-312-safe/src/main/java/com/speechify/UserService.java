package com.speechify;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class UserService {
    private static final String DB_FILE = "db.json";
    private static final int MIN_AGE = 21;
    private static final double DEFAULT_CREDIT_LIMIT = 10000.0;
    private static final double IMPORTANT_CLIENT_MULTIPLIER = 2.0;
    private static final String VERY_IMPORTANT_CLIENT = "VeryImportantClient";
    private static final String IMPORTANT_CLIENT = "ImportantClient";
    
    private final ObjectMapper objectMapper;
    private ClientRepository clientRepository;

    public UserService() {
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<Boolean> addUser(
            String firstname,
            String surname,
            String email,
            LocalDate dateOfBirth,
            String clientId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate input
                if (!isValidUserInput(firstname, surname, email)) {
                    return false;
                }
                
                if (!isAgeValid(dateOfBirth)) {
                    return false;
                }
                
                // Read database
                ObjectNode root = readDatabase();
                if (root == null) {
                    return false;
                }
                
                ArrayNode users = (ArrayNode) root.get("users");
                
                // Check email uniqueness
                if (!isEmailUnique(users, email)) {
                    return false;
                }
                
                // Get client
                clientRepository = new ClientRepository();
                Client client = clientRepository.getById(clientId).join();
                if (client == null) {
                    System.err.println("Client not found");
                    return false;
                }
                
                // Create and save user
                User user = createUser(client, dateOfBirth, email, firstname, surname);
                users.add(objectMapper.valueToTree(user));
                
                return saveDatabase(root);
                
            } catch (IOException e) {
                System.err.println("Database operation failed: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> updateUser(User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (user == null) {
                    return false;
                }

                ObjectNode root = readDatabase();
                if (root == null) {
                    return false;
                }

                ArrayNode users = (ArrayNode) root.get("users");

                // Find and update user
                for (int i = 0; i < users.size(); i++) {
                    ObjectNode userNode = (ObjectNode) users.get(i);
                    if (userNode.get("id").asText().equals(user.getId())) {
                        users.set(i, objectMapper.valueToTree(user));
                        return saveDatabase(root);
                    }
                }
                return false;
            } catch (IOException e) {
                System.err.println("Database operation failed: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<List<User>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ObjectNode root = readDatabase();
                if (root == null) {
                    return new ArrayList<>();
                }

                ArrayNode users = (ArrayNode) root.get("users");
                List<User> userList = new ArrayList<>();

                for (int i = 0; i < users.size(); i++) {
                    User user = objectMapper.treeToValue(users.get(i), User.class);
                    userList.add(user);
                }
                
                return userList;
            } catch (IOException e) {
                System.err.println("Database operation failed: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    public CompletableFuture<User> getUserByEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ObjectNode root = readDatabase();
                if (root == null) {
                    return null;
                }

                ArrayNode users = (ArrayNode) root.get("users");

                for (int i = 0; i < users.size(); i++) {
                    ObjectNode userNode = (ObjectNode) users.get(i);
                    if (userNode.get("email").asText().equals(email)) {
                        User user = objectMapper.treeToValue(userNode, User.class);
                        return user;
                    }
                }
                return null;
            } catch (IOException e) {
                System.err.println("Database operation failed: " + e.getMessage());
                return null;
            }
        });
    }
    
    // Extracted helper methods
    private boolean isValidUserInput(String firstname, String surname, String email) {
        return firstname != null && surname != null && email != null;
    }
    
    private boolean isAgeValid(LocalDate dateOfBirth) {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return age >= MIN_AGE;
    }
    
    private boolean isEmailUnique(ArrayNode users, String email) {
        for (int i = 0; i < users.size(); i++) {
            ObjectNode userNode = (ObjectNode) users.get(i);
            if (userNode.get("email").asText().equals(email)) {
                return false;
            }
        }
        return true;
    }
    
    private ObjectNode readDatabase() throws IOException {
        File dbFile = new File(DB_FILE);
        if (!dbFile.exists()) {
            return null;
        }
        return (ObjectNode) objectMapper.readTree(dbFile);
    }
    
    private boolean saveDatabase(ObjectNode root) {
        try {
            File dbFile = new File(DB_FILE);
            objectMapper.writeValue(dbFile, root);
            return true;
        } catch (IOException e) {
            System.err.println("Database save failed: " + e.getMessage());
            return false;
        }
    }
    
    private User createUser(Client client, LocalDate dateOfBirth, String email, 
                           String firstname, String surname) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setClient(client);
        user.setDateOfBirth(dateOfBirth);
        user.setEmail(email);
        user.setFirstname(firstname);
        user.setSurname(surname);
        
        setCreditLimitBasedOnClient(user, client);
        return user;
    }
    
    private void setCreditLimitBasedOnClient(User user, Client client) {
        String clientName = client.getName();
        if (VERY_IMPORTANT_CLIENT.equals(clientName)) {
            user.setHasCreditLimit(false);
        } else if (IMPORTANT_CLIENT.equals(clientName)) {
            user.setHasCreditLimit(true);
            user.setCreditLimit(DEFAULT_CREDIT_LIMIT * IMPORTANT_CLIENT_MULTIPLIER);
        } else {
            user.setHasCreditLimit(true);
            user.setCreditLimit(DEFAULT_CREDIT_LIMIT);
        }
    }
} 