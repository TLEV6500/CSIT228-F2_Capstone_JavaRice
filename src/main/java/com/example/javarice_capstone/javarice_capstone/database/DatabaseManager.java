package com.example.javarice_capstone.javarice_capstone.database;

import com.example.javarice_capstone.javarice_capstone.datatypes.SerializableGameData;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


public abstract class DatabaseManager implements AutoCloseable {

    public abstract void logout() throws Exception;

    // CRUD Operations
    public abstract int saveData(SerializableGameData data) throws DatabaseException;
    public abstract <T extends SerializableGameData> T fetchData(Class<T> classType, int id) throws DatabaseException;
    public abstract <T extends SerializableGameData> List<T> fetchAll(Class<T> classType) throws DatabaseException;
    public abstract <T extends SerializableGameData> void updateData(T newData, int... ids) throws DatabaseException;
    public abstract <T extends SerializableGameData> void deleteData(Class<T> classType, int... ids) throws DatabaseException;

    // Transactions
    public <R> R executeTransaction(Transaction<R, SQLException> transaction) throws UnsupportedOperationException, DatabaseException {
        throw new UnsupportedOperationException("Transactions not supported by this implementation");
    }

    public static class DatabaseException extends Exception {
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
        public DatabaseException(String message) {
            super(message);
        }
    }

    @FunctionalInterface
    public interface Transaction<R,X extends Exception> {
        R apply(Connection db) throws X;
    }
}