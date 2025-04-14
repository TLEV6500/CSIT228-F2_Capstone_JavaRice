package com.example.javarice_capstone.javarice_capstone.database;

import com.example.javarice_capstone.javarice_capstone.datatypes.Player;
import com.example.javarice_capstone.javarice_capstone.datatypes.SerializableGameData;

import java.io.*;
import java.util.Hashtable;

public class DatabaseManager {
    private static final String storageDirName = "gamedata";
    private static final Hashtable<Class<? extends SerializableGameData>, Hashtable<Integer, String>> tables = new Hashtable<>();
    private static final Hashtable<Class<? extends SerializableGameData>, Integer> tableNextId = new Hashtable<>();

    // CREATE
    public static int saveData(SerializableGameData data) throws SerializationFailureException {
        int id = data.getId();
        try (ObjectOutputStream obj = new ObjectOutputStream(new FileOutputStream(formatDataStoreFileName(data)))) {
            System.out.println("Saving data " + data.getClass() + "...");
            obj.writeObject(data);
            System.out.println("Successfully saved data" + data.getClass());
        } catch (IOException e) {
            throw new SerializationFailureException(data, e);
        }
        return id;
    }

    private DatabaseManager() {}

    // READ
    public static <T extends SerializableGameData> T fetchData(Class<? extends T> classType, int id) throws DataFetchException, SerializationFailureException {
        T data = null;
        try (ObjectInputStream obj = new ObjectInputStream(new FileInputStream(getDataStoreFileName(classType, id)))) {
            System.out.println("Fetching data " + classType + " with id="+id+"...");
            data = (T) obj.readObject();
            System.out.println("Successfully saved data" + data.getClass());
        } catch(FileNotFoundException e) {
            throw new DataFetchException(classType, id, e);
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationFailureException(classType, e);
        }
        return data;
    }

    // UPDATE
    public static <T extends SerializableGameData> void updateData(Class<? extends T> classType, T newData, int... ids) throws SerializationFailureException {
        for (int id : ids) {
            try (ObjectOutputStream obj = new ObjectOutputStream(new FileOutputStream(getDataStoreFileName(classType, id)))) {
                System.out.println("Updating data " + newData.getClass() + "...");
                obj.writeObject(newData);
                System.out.println("Updated data " + newData.getClass() + " for id=" + id);
            } catch(FileNotFoundException e) {
                System.err.println(DataFetchException.formatMessage(classType, id) + ". Ignoring id...");
            } catch (IOException e) {
                throw new SerializationFailureException(newData, e);
            }
        }
    }

    // DELETE
    public static <T extends SerializableGameData> void deleteData(Class<? extends T> classType, int... ids) throws SerializationFailureException {
        String curFileName;
        for (int id : ids) {
            curFileName = tables.get(classType).remove(id);
            System.out.println("Deleting data store file " + curFileName + "...");
            new File(curFileName).delete();
        }
        System.out.println("Successfully deleted " + ids.length + " rows");
    }

    private static String getDataStoreFileName(Class<? extends SerializableGameData> classType, int id) {
        return getTable(classType).get(id);
    }

    private static Hashtable<Integer, String> getTable(Class<? extends SerializableGameData> classType) {
        return tables.get(classType);
    }

    private static String formatDataStoreFileName(SerializableGameData data) {
        return storageDirName + "/" + data.getClass() + "/" + data.getId() + ".txt";
    }
    private static String formatDataStoreFileName(Class<? extends SerializableGameData> classType, int id) {
        return storageDirName + "/" + classType + "/" + id + ".txt";
    }

    public static class SerializationFailureException extends Exception {
        SerializationFailureException(SerializableGameData data, Exception cause) {
            super("Failed to serialize/deserialize data of type " + data.getClass(), cause);
        }
        SerializationFailureException(Class<? extends Serializable> classType, Exception cause) {
            super("Failed to serialize/deserialize data of type " + classType, cause);
        }
    }

    public static class DataFetchException extends Exception {
        public static String formatMessage(Class<? extends Serializable> classType, int id) {
            return "Failed to fetch " + classType + " of id=" + id;
        }
        DataFetchException(Class<? extends Serializable> classType, int id, Exception cause) {
            super(formatMessage(classType, id), cause);
        }
    }
}
