package com.example.javarice_capstone.javarice_capstone.database;

import com.example.javarice_capstone.javarice_capstone.datatypes.SerializableGameData;

import java.io.*;
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

    public static class DatabaseException extends Exception {
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
        public DatabaseException(String message) {
            super(message);
        }
    }

    public static byte[] serialize(SerializableGameData data) throws DatabaseException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(data);
            oos.flush();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new DatabaseException(
                    "Failed to serialize " + data.getClass().getSimpleName(),
                    e
            );
        }
    }

    public static <T extends SerializableGameData> T deserialize(byte[] bytes, Class<T> classType) throws DatabaseException {
        T data = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            data = (T) ois.readObject();
            return data;

        } catch (IOException | ClassNotFoundException e) {
            assert data != null;
            throw new DatabaseException(
                    "Failed to deserialize " + data.getClass().getSimpleName(),
                    e
            );
        }
    }

    @FunctionalInterface
    public interface Transaction<R,X extends Exception> {
        R apply(Connection db) throws X;
    }
}