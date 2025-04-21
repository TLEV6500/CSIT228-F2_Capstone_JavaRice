package com.example.javarice_capstone.javarice_capstone.database;

import com.example.javarice_capstone.javarice_capstone.datatypes.SerializableGameData;

import java.io.Serializable;
import java.util.List;

public interface DatabaseManager {
    // CREATE
    int saveData(SerializableGameData data) throws SerializationFailureException;

    // READ
    <T extends SerializableGameData> T fetchData(Class<? extends T> classType, int id) throws DataFetchException, SerializationFailureException;
    <T extends SerializableGameData> List<T> fetchAll(Class<? extends T> classType) throws DataFetchException, SerializationFailureException;

    // UPDATE
    <T extends SerializableGameData> void updateData(Class<? extends T> classType, T newData, int... ids) throws SerializationFailureException;

    // DELETE
    <T extends SerializableGameData> void deleteData(Class<? extends T> classType, int... ids) throws SerializationFailureException;

//    void saveAllIds(Class<SerializableGameData> classType) throws SerializationFailureException;
//    List<Integer> getAllIds(Class<SerializableGameData> classType);

    class SerializationFailureException extends Exception {
        SerializationFailureException(SerializableGameData data, Exception cause) {
            super("Failed to serialize/deserialize data of type " + data.getClass(), cause);
        }
        SerializationFailureException(Class<? extends SerializableGameData> classType, Exception cause) {
            super("Failed to serialize/deserialize data of type " + classType, cause);
        }
        SerializationFailureException(Class<? extends SerializableGameData> classType) {
            super("Failed to serialize/deserialize data of type " + classType);
        }
    }

    class DataFetchException extends Exception {
        public static String formatMessage(Class<? extends SerializableGameData> classType, int id) {
            return "Failed to fetch " + classType + " of id=" + id;
        }
        public static String formatMessage(Class<? extends SerializableGameData> classType) {
            return "Failed to fetch data for " + classType;
        }
        public static String formatMessage(String fileName) {
            return "Failed to fetch " + fileName;
        }
        DataFetchException(Class<? extends SerializableGameData> classType, int id, Exception cause) {
            super(formatMessage(classType, id), cause);
        }
        DataFetchException(Class<? extends SerializableGameData> classType) {
            super(formatMessage(classType));
        }
        DataFetchException(String fileName, Exception cause) {
            super(fileName, cause);
        }
    }
}
