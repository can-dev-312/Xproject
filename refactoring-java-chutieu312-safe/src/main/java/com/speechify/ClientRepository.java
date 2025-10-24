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
    private static final int CLIENT_CACHE_SIZE = 10;
    private static final int ALL_CLIENT_CACHE_SIZE = 1;
    private static final String ALL_CLIENT_CACHE_KEY = "ALL CLIENTS";
    private final ObjectMapper objectMapper;
    private final LRUCache<Client> clientCache;
    private final LRUCache<List<Client>> allClientCache;
    

    public ClientRepository() {
        this.objectMapper = new ObjectMapper();
        this.clientCache = LRUCacheProvider.createLRUCache(new CacheLimits(CLIENT_CACHE_SIZE));
        this.allClientCache = LRUCacheProvider.createLRUCache(new CacheLimits(ALL_CLIENT_CACHE_SIZE));;
    }

    public CompletableFuture<Client> getById(String id) {
        return CompletableFuture.supplyAsync(() -> {

            // check cache hit ?
            Client clientFromCache = clientCache.get(id);
            if (clientFromCache != null) {
                return clientFromCache;
            }

            // cache miss
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
                        // cache the new client
                        clientCache.set(id, client);
                        
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

            // check cache hit ?
            List<Client> allClientsFromCache = allClientCache.get(ALL_CLIENT_CACHE_KEY);
            if (allClientsFromCache != null) {
                return allClientsFromCache;
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
                    
                }
                // cache the all client
                allClientCache.set(ALL_CLIENT_CACHE_KEY, clientList);
                
                return clientList;

            } catch (IOException e) {
                return new ArrayList<>();
            }
        });
    }
} 