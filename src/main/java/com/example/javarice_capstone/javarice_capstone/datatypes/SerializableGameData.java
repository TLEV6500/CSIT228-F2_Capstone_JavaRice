package com.example.javarice_capstone.javarice_capstone.datatypes;

import java.io.Serializable;
import java.sql.*;

public interface SerializableGameData extends Serializable {
    int getId();
    int getNextId();
    String[] getDataFields();
//    SQLType[] getDataFieldTypes();

    /**
     * Extract data from resultSet and assign them to their respective fields in the object
     * @param resultSet Retrieved from an SQL Query
     * @return number of fields assigned
     * @implNote fields of Object type or supertype should be assigned with care, as they are not yet accounted for. Best to override this method for specific assignments
     */
    default int extractDataFrom(ResultSet resultSet) {
        int count = 0;
        try {
            for (String field : getDataFields()) {
                var val = resultSet.getObject(field, SerializableGameData.getDataFieldType(this, field));
                boolean s = SerializableGameData.setDataField(this, field, val);
                if (!s) continue;
                ++count;
            }
        } catch (SQLException e) {
            System.err.println("Failed to extract data.");
            System.err.println(e.getMessage());
        }
        return count;
    }

    static boolean setDataField(SerializableGameData data, String fieldName, Object value) {
        try {
            data.getClass().getField(fieldName).set(data, value);
            return true;
        } catch (NoSuchFieldException e) {
            System.err.println("Cannot find fieldName " + fieldName + " for " + data.getClass());
        } catch (IllegalAccessException e) {
            System.err.println("Cannot reassign to inaccessible or final field of " + data.getClass());
        }
        return false;
    }

    static Class<?> getDataFieldType(SerializableGameData data, String fieldName)  {
        try {
            return data.getClass().getField(fieldName).getType();
        } catch (NoSuchFieldException e) {
            System.err.println("Cannot find fieldName " + fieldName + " for " + data.getClass());
        }
        return null;
    }

    default int setAllIntFields(int... values) {
        return 0;
    }

    default int setAllDoubleFields(double... values) {
        return 0;
    }

    default int setAllCharFields(char... values) {
        return 0;
    }

    default int setAllStringFields(char... values) {
        return 0;
    }

    default int setAllObjectFields(Object... values) {
        return 0;
    }

}
