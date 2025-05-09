package com.example.javarice_capstone.javarice_capstone.ui.helpers;

import javafx.scene.Node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {
    private static NodeRegistry instance;
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private NodeRegistry() {}

    public static NodeRegistry getInstance() {
        if (instance == null) instance = new NodeRegistry();
        return instance;
    }

    public void register(Node node) {
        if (node.getId() != null) {
            nodes.put(node.getId(), node);
        }
        System.err.println("Cannot register node because id is null");
    }

    public <T extends Node> T findNodeById(String id) {
        return (T) nodes.get(id);
    }
}