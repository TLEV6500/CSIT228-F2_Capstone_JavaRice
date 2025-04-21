package com.example.javarice_capstone.javarice_capstone.database;

import com.example.javarice_capstone.javarice_capstone.datatypes.SerializableGameData;

import java.io.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.sql.*;
import java.util.function.Function;

public class MySQLDatabaseManager implements DatabaseManager {
    private static String username;
    private static String password;
    private static String dbUrl;

    private static MySQLDatabaseManager instance = null;
    private MySQLDatabaseManager(String dbUrl, String username, String password) {
        this.username = username;
        this.password = password;
        this.dbUrl = dbUrl;
    }

    private void newInstanceIfNull() {
        if (dbUrl == null || username == null || password == null) throw new IllegalStateException("Username, password, or database url for MySQL DB not defined.");
        if (instance == null) instance = new MySQLDatabaseManager(dbUrl, username, password);
    }

    private <T> T beginTransaction(Function<Connection, T> action) {
        try (Connection db = DriverManager.getConnection(dbUrl, username, password)) {
            return action.apply(db);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int saveData(SerializableGameData data) throws SerializationFailureException {
        newInstanceIfNull();
        return 0;
    }

    @Override
    public <T extends SerializableGameData> T fetchData(Class<? extends T> classType, int id) throws DataFetchException, SerializationFailureException {
        newInstanceIfNull();
        return null;
    }

    @Override
    public <T extends SerializableGameData> List<T> fetchAll(Class<? extends T> classType) throws DataFetchException, SerializationFailureException {
        newInstanceIfNull();
        return null;
    }

    @Override
    public <T extends SerializableGameData> void updateData(Class<? extends T> classType, T newData, int... ids) throws SerializationFailureException {
        newInstanceIfNull();

    }

    @Override
    public <T extends SerializableGameData> void deleteData(Class<? extends T> classType, int... ids) throws SerializationFailureException {
        newInstanceIfNull();

    }
}
