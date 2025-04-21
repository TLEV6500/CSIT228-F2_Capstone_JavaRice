package com.example.javarice_capstone.javarice_capstone.database;

import com.example.javarice_capstone.javarice_capstone.datatypes.SerializableGameData;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

public class LocalDatabaseManager implements DatabaseManager {
    private final String storageDirName = "gamedata";
    private final Hashtable<Class<? extends SerializableGameData>, Hashtable<Integer, String>> tables = new Hashtable<>();
    private final Hashtable<Class<? extends SerializableGameData>, Integer> tableNextId = new Hashtable<>();
    private static LocalDatabaseManager instance = null;
    private LocalDatabaseManager() {}

    private void newInstanceIfNull() {
        if (instance == null) instance = new LocalDatabaseManager();
    }

    // CREATE
    public int saveData(SerializableGameData data) throws SerializationFailureException {
        newInstanceIfNull();
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


    // READ
    public <T extends SerializableGameData> T fetchData(Class<? extends T> classType, int id) throws DataFetchException, SerializationFailureException {
        newInstanceIfNull();
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

    public <T extends SerializableGameData> List<T> fetchAll(Class<? extends T> classType) throws DataFetchException, SerializationFailureException {
        newInstanceIfNull();
        List<T> dataList = new ArrayList<>();
        Collection<String> fileNameList = getTable(classType).values();
        if (fileNameList.isEmpty()) {
            fileNameList = new ArrayList<>(List.of(getFileNamesFromTable(classType)));
            if (fileNameList.isEmpty()) throw new DataFetchException(classType);
        }
        for (String fileName : fileNameList) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
                dataList.add((T) ois.readObject());
            } catch (FileNotFoundException e) {
                throw new DataFetchException(fileName, e);
            } catch (IOException | ClassNotFoundException e) {
                throw new SerializationFailureException(classType, e);
            }
        }
        return dataList;
    }

    // UPDATE
    public <T extends SerializableGameData> void updateData(Class<? extends T> classType, T newData, int... ids) throws SerializationFailureException {
        newInstanceIfNull();
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
    public <T extends SerializableGameData> void deleteData(Class<? extends T> classType, int... ids) throws SerializationFailureException {
        newInstanceIfNull();
        String curFileName;
        for (int id : ids) {
            curFileName = tables.get(classType).remove(id);
            System.out.println("Deleting data store file " + curFileName + "...");
            if (!new File(curFileName).delete()) throw new SerializationFailureException(classType);
        }
        System.out.println("Successfully deleted " + ids.length + " rows");
    }

    private String getDataStoreFileName(Class<? extends SerializableGameData> classType, int id) {
        return getTable(classType).get(id);
    }

    private Hashtable<Integer, String> getTable(Class<? extends SerializableGameData> classType) {
        return tables.get(classType);
    }

    private String formatDataStoreFileName(SerializableGameData data) {
        return storageDirName + "/" + data.getClass() + "/" + data.getId() + ".txt";
    }
    private String formatDataStoreFileName(Class<? extends SerializableGameData> classType, int id) {
        return storageDirName + "/" + classType + "/" + id + ".txt";
    }

    private <T extends SerializableGameData> String[] getFileNamesFromTable(Class<? extends T> classType) {
        String dirName = storageDirName + "/" + classType;
        File dir = new File(dirName);
        return dir.list();
    }
}
