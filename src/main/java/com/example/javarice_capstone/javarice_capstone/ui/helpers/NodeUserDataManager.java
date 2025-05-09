package com.example.javarice_capstone.javarice_capstone.ui.helpers;

import javafx.scene.Node;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class NodeUserDataManager {
    private static final Map<Class<? extends Node>, Map<String, Object>> userDataMap = new HashMap<>();

    public NodeUserDataManager() {

    }

    public void setUserDataFor(Node node, Map<String, Object> userData) {
        Class<? extends Node> clazz = node.getClass();
        node.setUserData(userData);
        userDataMap.put(clazz, userData);
    }

    public Map<String, ?> getUserDataFor(Node node) {
        Class<? extends Node> clazz = node.getClass();
        return userDataMap.get(clazz);
    }

    public Class<?> getFieldType(Node node, String key) {
        return userDataMap.get(node.getClass()).get(key).getClass();
    }

    @SafeVarargs
    public final <T> boolean setUserDataFieldsFor(Node node, Pair<String, T>... pairs) {
        var map = userDataMap.get(node.getClass());
        if (map == null) return false;
        for (var p : pairs) {
            map.put(p.getKey(), p.getValue());
        }
        return true;
    }

    public record ObjectTypePair<T extends Class<V>, V>(T type, V value) {
        public boolean isEmpty() {
            return type == null || value == null;
        }
    }
}
