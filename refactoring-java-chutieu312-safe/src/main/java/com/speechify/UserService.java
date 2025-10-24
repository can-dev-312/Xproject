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
    private static final String VeryImportantClient = "VeryImportantClient";
    private static final String ImportantClient = "ImportantClient";
    private static final double default_credit_limit = 10000;
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

                // check user_valid
                if(!userValid(firstname, surname, email)) return false;

                // read_database
                ObjectNode root = readDatabase();
                if (root == null) return false;
                
                ArrayNode users = (ArrayNode) root.get("users");

                // check email_unique
                if (!emailUnique(users, email))  return false;
     
                // check age_valid
                if (!ageValid(dateOfBirth)) return false;
                
                // Get client
                clientRepository = new ClientRepository();
                Client client = clientRepository.getById(clientId).join();
                if (client == null) {
                    System.err.println("Client not found");
                    return false;
                }

                // create_user
                User user = createUser(client, dateOfBirth, email, firstname, surname);

                // Add user to ussers
                users.add(objectMapper.valueToTree(user));

                // save_database
                saveDatabase(root);

                return true;

            } catch (IOException e) {
                System.err.println("Database operation failed: " + e.getMessage());
                return false;
            }
        });
    }

    private boolean userValid(String firstname, String surname, String email) {
        return !(firstname == null || surname == null || email == null) ;
    }

    private ObjectNode readDatabase() throws IOException {
        File dbFile = new File(DB_FILE);
        if (!dbFile.exists()) {
            return null;
        }
        return (ObjectNode) objectMapper.readTree(dbFile);
    }

    private void saveDatabase(ObjectNode root) throws IOException{
        File dbFile = new File(DB_FILE);
        objectMapper.writeValue(dbFile, root);
    }

    private boolean emailUnique(ArrayNode users, String email){
        for (int i = 0; i < users.size(); i++) {
            ObjectNode userNode = (ObjectNode) users.get(i);
            if (userNode.get("email").asText().equals(email)) {
                return false;
            }
        }
        return true;
    }

    private boolean ageValid(LocalDate dateOfBirth){
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return age >= MIN_AGE;
    }

    private User createUser(Client client, LocalDate dateOfBirth, String email, 
                                                    String firstname, String surname){
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setClient(client);
        user.setDateOfBirth(dateOfBirth);
        user.setEmail(email);
        user.setFirstname(firstname);
        user.setSurname(surname);
        setCreditLimit(user,client);

        return user;
    }

    private void setCreditLimit(User user, Client client){
        if (VeryImportantClient.equals(client.getName())) {
            user.setHasCreditLimit(false);
        } else if (ImportantClient.equals(client.getName())) {
            user.setHasCreditLimit(true);
            user.setCreditLimit(default_credit_limit * 2);
        } else {
            user.setHasCreditLimit(true);
            user.setCreditLimit(default_credit_limit);
        }
    }


    public CompletableFuture<Boolean> updateUser(User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (user == null) {
                    return false;
                }

                ObjectNode root = readDatabase();
                if (root == null) return false;

                ArrayNode users = (ArrayNode) root.get("users");

                // Find and update user
                for (int i = 0; i < users.size(); i++) {
                    ObjectNode userNode = (ObjectNode) users.get(i);
                    if (userNode.get("id").asText().equals(user.getId())) {
                        users.set(i, objectMapper.valueToTree(user));
                        saveDatabase(root);
                        return true;
                    }
                }
                return false;
            } catch (IOException e) {
                return false;
            }
        });
    }

    public CompletableFuture<List<User>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ObjectNode root = readDatabase();
                if (root == null) return new ArrayList<>();

                ArrayNode users = (ArrayNode) root.get("users");

                List<User> userList = new ArrayList<>();

                for (int i = 0; i < users.size(); i++) {
                    User user = objectMapper.treeToValue(users.get(i), User.class);
                    userList.add(user);
                }
                return userList;
            } catch (IOException e) {
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
                        return objectMapper.treeToValue(userNode, User.class);
                    }
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        });
    }
} 