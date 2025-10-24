package com.speechify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ClientRepository {
    private static final String DB_FILE = "db.json";
    private static final String ALL_CLIENTS_CACHE_KEY = "ALL_CLIENTS";
    private static final int CLIENT_CACHE_SIZE = 50;
    private static final int ALL_ITEMS_CACHE_SIZE = 1;
    
    private final ObjectMapper objectMapper;
    private final LRUCache<Client> clientIdCache;
    private final LRUCache<List<Client>> allClientsCache;

    public ClientRepository() {
        this.objectMapper = new ObjectMapper();
        this.clientIdCache = LRUCacheProvider.createLRUCache(new CacheLimits(CLIENT_CACHE_SIZE));
        this.allClientsCache = LRUCacheProvider.createLRUCache(new CacheLimits(ALL_ITEMS_CACHE_SIZE));
    }

    public CompletableFuture<Client> getById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            Client cached = clientIdCache.get(id);
            if (cached != null) {
                return cached;
            }
            
            try {
                File dbFile = new File(DB_FILE);
                if (!dbFile.exists()) {
                    return null;
                }

                ObjectNode root = (ObjectNode) objectMapper.readTree(dbFile);
                ArrayNode clients = (ArrayNode) root.get("clients");
                
                for (int i = 0; i < clients.size(); i++) {
                    ObjectNode clientNode = (ObjectNode) clients.get(i);
                    if (clientNode.get("id").asText().equals(id)) {
                        Client client = new Client();
                        client.setId(clientNode.get("id").asText());
                        client.setName(clientNode.get("name").asText());
                        // Cache the result
                        clientIdCache.set(id, client);
                        return client;
                    }
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        });
    }

    public CompletableFuture<List<Client>> getAll() {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            List<Client> cached = allClientsCache.get(ALL_CLIENTS_CACHE_KEY);
            if (cached != null) {
                return cached;
            }
            
            try {
                File dbFile = new File(DB_FILE);
                if (!dbFile.exists()) {
                    return new ArrayList<>();
                }

                ObjectNode root = (ObjectNode) objectMapper.readTree(dbFile);
                ArrayNode clients = (ArrayNode) root.get("clients");
                List<Client> clientList = new ArrayList<>();

                for (int i = 0; i < clients.size(); i++) {
                    ObjectNode clientNode = (ObjectNode) clients.get(i);
                    Client client = new Client();
                    client.setId(clientNode.get("id").asText());
                    client.setName(clientNode.get("name").asText());
                    clientList.add(client);
                    // Also cache individual clients by ID
                   clientIdCache.set(client.getId(), client); 
                }
                
                // Cache the complete list
                allClientsCache.set(ALL_CLIENTS_CACHE_KEY, clientList);
                return clientList;
            } catch (IOException e) {
                return new ArrayList<>();
            }
        });
    }
} 