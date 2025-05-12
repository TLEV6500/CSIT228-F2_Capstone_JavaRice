package com.example.javarice_capstone.javarice_capstone.database.local;

import com.example.javarice_capstone.javarice_capstone.database.DatabaseManager;
import com.example.javarice_capstone.javarice_capstone.datatypes.SerializableGameData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

public class LocalDatabaseManager extends DatabaseManager {
    private final String storageDirName = "gamedata";
    private final Map<Class<? extends SerializableGameData>, Map<Integer, String>> tables = new HashMap<>();
    private static volatile LocalDatabaseManager instance;
    private final Object lock = new Object();

    private LocalDatabaseManager() {
        initializeStorageDir();
    }

    public static LocalDatabaseManager getInstance() {
        if (instance == null) {
            synchronized (LocalDatabaseManager.class) {
                if (instance == null) {
                    instance = new LocalDatabaseManager();
                }
            }
        }
        return instance;
    }

    @Override
    public void close() throws Exception {
        logout();
    }

    @Override
    public void logout() throws UnsupportedOperationException {
        synchronized (lock) {
            tables.clear();
        }
    }

    // CREATE
    @Override
    public int saveData(SerializableGameData data) throws DatabaseException {
        synchronized (lock) {
            Path filePath = getDataFilePath(data);
            try {
                Files.createDirectories(filePath.getParent());
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                )) {
                    oos.writeObject(data);
                    getTable(data.getClass()).put(data.getId(), filePath.toString());
                    return data.getId();
                }
            } catch (IOException e) {
                throw new DatabaseException("Failed to save " + data.getClass().getSimpleName(), e);
            }
        }
    }

    // READ
    @Override
    public <T extends SerializableGameData> T fetchData(Class<T> classType, int id) throws DatabaseException {
        synchronized (lock) {
            Path filePath = Path.of(getTable(classType).get(id));
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(filePath))) {
                return classType.cast(ois.readObject());
            } catch (IOException | ClassNotFoundException | NullPointerException e) {
                throw new DatabaseException("Failed to fetch " + classType.getSimpleName() + " with id=" + id, e);
            }
        }
    }

    // READ ALL
    @Override
    public <T extends SerializableGameData> List<T> fetchAll(Class<T> classType) throws DatabaseException {
        synchronized (lock) {
            Map<Integer, String> table = getTable(classType);
            if (table.isEmpty()) {
                // If no saved records in-memory, scan directory
                Path classDir = Path.of(storageDirName, classType.getSimpleName());
                if (!Files.exists(classDir)) {
                    return Collections.emptyList();
                }

                try (Stream<Path> stream = Files.list(classDir)) {
                    List<Path> files = stream
                            .filter(p -> p.toString().endsWith(".dat"))
                            .toList();

                    if (files.isEmpty()) {
                        return Collections.emptyList();
                    }

                    // Load files and populate in-memory table
                    for (Path file : files) {
                        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
                            T data = classType.cast(ois.readObject());
                            table.put(data.getId(), file.toString());
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    throw new DatabaseException("Failed to scan directory for " + classType.getSimpleName(), e);
                }
            }

            // Return all the records from the table
            List<T> result = new ArrayList<>();
            for (String filePath : table.values()) {
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Path.of(filePath)))) {
                    result.add(classType.cast(ois.readObject()));
                } catch (IOException | ClassNotFoundException e) {
                    throw new DatabaseException("Failed to load " + classType.getSimpleName() + " from " + filePath, e);
                }
            }
            return result;
        }
    }

    // UPDATE
    @Override
    public <T extends SerializableGameData> void updateData(T newData, int... ids) throws DatabaseException {
        synchronized (lock) {
            for (int id : ids) {
                Path filePath = getDataFilePath(newData.getClass(), id);
                if (!Files.exists(filePath)) {
                    throw new DatabaseException("No data found for ID " + id);
                }

                try (ObjectOutputStream oos = new ObjectOutputStream(
                        Files.newOutputStream(filePath, StandardOpenOption.TRUNCATE_EXISTING))
                ) {
                    oos.writeObject(newData);
                } catch (IOException e) {
                    throw new DatabaseException("Failed to update " + newData.getClass().getSimpleName(), e);
                }
            }
        }
    }

    // DELETE
    @Override
    public <T extends SerializableGameData> void deleteData(Class<T> classType, int... ids) throws DatabaseException {
        synchronized (lock) {
            Map<Integer, String> table = getTable(classType);
            for (int id : ids) {
                String filePath = table.remove(id);
                if (filePath != null) {
                    try {
                        Files.deleteIfExists(Path.of(filePath));
                    } catch (IOException e) {
                        throw new DatabaseException("Failed to delete file: " + filePath, e);
                    }
                }
            }
        }
    }

    // Helper method for update/delete operations
    private Path getDataFilePath(Class<? extends SerializableGameData> classType, int id) {
        return Path.of(storageDirName, classType.getSimpleName(), id + ".dat");
    }

    private Path getDataFilePath(SerializableGameData data) {
        return Path.of(storageDirName, data.getClass().getSimpleName(), data.getId() + ".dat");
    }

    private Map<Integer, String> getTable(Class<? extends SerializableGameData> classType) {
        return tables.computeIfAbsent(classType, k -> new HashMap<>());
    }

    private void initializeStorageDir() {
        synchronized (lock) {
            try {
                Files.createDirectories(Path.of(storageDirName));
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize storage directory", e);
            }
        }
    }
}