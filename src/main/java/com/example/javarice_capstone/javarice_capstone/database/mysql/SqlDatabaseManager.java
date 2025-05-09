package com.example.javarice_capstone.javarice_capstone.database.mysql;

import com.example.javarice_capstone.javarice_capstone.database.DatabaseManager;
import com.example.javarice_capstone.javarice_capstone.database.local.LocalDatabaseManager;
import com.example.javarice_capstone.javarice_capstone.datatypes.SerializableGameData;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SqlDatabaseManager extends DatabaseManager {
    private final Connection connection;
    private final Set<String> initializedTables = ConcurrentHashMap.newKeySet();
    private final boolean useDedicatedTables = true; // Configurable
    private PreparedStatement dedicatedTableSaveStmt;
    private PreparedStatement gameDataTableSaveStmt;

    public SqlDatabaseManager(Connection connection) throws SQLException {
        this.connection = connection;
        initializeCoreTables();
        if (useDedicatedTables) {
            dedicatedTableSaveStmt = connection.prepareStatement(
            """
            CREATE TABLE game_data (
                type_name VARCHAR(255) PRIMARY KEY,
                table_name VARCHAR(100) NOT NULL,
                schema_version INT NOT NULL,
                UNIQUE(table_name)
            """);
        } else {
            gameDataTableSaveStmt = connection.prepareStatement("...");
        }
    }

    @Override
    public void close() throws Exception {
        logout();
    }

    @Override
    public void logout() {
        synchronized (this) {
            try {
                // 1. Clear in-memory state
                initializedTables.clear();

                // 2. Return connection to pool (if using pooling)
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Failed to close database connection: " + e.getMessage());
            }
        }
    }

    @Override
    public int saveData(SerializableGameData data) throws DatabaseException {
        try {
            ensureTableForType(data.getClass());
            String tableName = data.getClass().getSimpleName();
            String sql = String.format("""
                INSERT INTO %s (id, data) 
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE data = VALUES(data)
                """, tableName);

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, data.getId());
                stmt.setBytes(2, LocalDatabaseManager.serialize(data));
                stmt.executeUpdate();
                return data.getId();
            }
        } catch (SQLException | DatabaseException e) {
            throw new DatabaseException("Failed to save " + data.getClass().getSimpleName(), e);
        }
    }

    @Override
    public <T extends SerializableGameData> T fetchData(Class<T> classType, int id) throws DatabaseException {
        String sql = "SELECT content FROM game_data WHERE id = ? AND type = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setString(2, classType.getSimpleName());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return deserialize(rs.getBytes("content"), classType);
            }
            throw new DatabaseException("No data found for " + classType.getSimpleName() + " with id=" + id);
        } catch (SQLException e) {
            throw new DatabaseException("SQL fetch failed", e);
        }
    }

    @Override
        public <T extends SerializableGameData> List<T> fetchAll(Class<T> classType) throws DatabaseException {
        try {
            ensureTableForType(classType);
            String tableName = classType.getSimpleName();
            String sql = "SELECT id, data FROM " + tableName;

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    byte[] serializedData = rs.getBytes("data");
                    T data = LocalDatabaseManager.deserialize(serializedData, classType);
                    results.add(data);
                }
                return results;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all " + classType.getSimpleName(), e);
        }
    }
    @Override
    public <T extends SerializableGameData> void updateData(T newData, int... ids) throws DatabaseException {
        try {
            ensureTableForType(newData.getClass());
            String tableName = newData.getClass().getSimpleName();
            String sql = "UPDATE " + tableName + " SET data = ?, version = version + 1 WHERE id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);

                for (int id : ids) {
                    stmt.setBytes(1, LocalDatabaseManager.serialize(newData));
                    stmt.setInt(2, id);
                    stmt.addBatch();
                }

                int[] updateCounts = stmt.executeBatch();
                connection.commit();

                // Verify updates
                for (int count : updateCounts) {
                    if (count == 0) {
                        throw new DatabaseException("No record updated for ID: " + ids[0]);
                    }
                }
            } catch (SQLException e) {
                connection.rollback();
                throw new DatabaseException("Failed to update " + tableName, e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException | DatabaseException e) {
            throw new DatabaseException("Update operation failed", e);
        }
    }

    @Override
    public <T extends SerializableGameData> void deleteData(Class<T> classType, int... ids) throws DatabaseException {
        try {
            ensureTableForType(classType);
            String tableName = classType.getSimpleName();
            String sql = "DELETE FROM " + tableName + " WHERE id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);

                for (int id : ids) {
                    stmt.setInt(1, id);
                    stmt.addBatch();
                }

                stmt.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new DatabaseException("Failed to delete from " + tableName, e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Delete operation failed", e);
        }
    }

    @Override
    public <R> R executeTransaction(Transaction<R,SQLException> transaction) throws DatabaseException {
        try {
            connection.setAutoCommit(false);
            R result = transaction.apply(connection);
            connection.commit();
            return result;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                throw new DatabaseException("Rollback failed", ex);
            }
            throw new DatabaseException("Transaction failed", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {}
        }
    }

    private void initializeCoreTables() {
        synchronized (this) {
            try {
                if (!tableExists("metadata")) {
                    createMetadataTable();
                }
                initializedTables.add("metadata");
            } catch (SQLException e) {
                throw new RuntimeException("Database initialization failed", e);
            }
        }
    }

    private void createMetadataTable() throws SQLException {
        String sql = """
            CREATE TABLE metadata (
                type_name VARCHAR(255) PRIMARY KEY,
                table_name VARCHAR(100) NOT NULL,
                schema_version INT NOT NULL,
                UNIQUE(table_name)
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // Lazy initialization for new data types
    private void ensureTableForType(Class<? extends SerializableGameData> classType)
            throws SQLException {
        String tableName = classType.getSimpleName();
        if (!initializedTables.contains(tableName)) {
            synchronized (this) {
                if (!initializedTables.contains(tableName)) {
                    if (useDedicatedTables) {
                        createDedicatedTable(classType);
                    }
                    registerTypeInMetadata(classType);
                    initializedTables.add(tableName);
                }
            }
        }
    }

    // Create a dedicated table for a specific class
    private void createDedicatedTable(Class<? extends SerializableGameData> classType)
            throws SQLException {
        String tableName = classType.getSimpleName();
        String sql = String.format("""
            CREATE TABLE %s (
                id INT PRIMARY KEY,
                data BLOB NOT NULL,
                version INT DEFAULT 1,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """, tableName);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // Register the type in metadata table
    private void registerTypeInMetadata(Class<? extends SerializableGameData> classType)
            throws SQLException {
        String sql = """
            INSERT INTO metadata (type_name, table_name, schema_version)
            VALUES (?, ?, 1)
            ON DUPLICATE KEY UPDATE schema_version = schema_version
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, classType.getName());
            stmt.setString(2, classType.getSimpleName());
            stmt.executeUpdate();
        }
    }

    // Check if a table exists
    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData dbMeta = connection.getMetaData();
        try (ResultSet rs = dbMeta.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private byte[] serialize(SerializableGameData data) throws DatabaseException {
        return LocalDatabaseManager.serialize(data);
    }

    private <T extends SerializableGameData> T deserialize(byte[] data, Class<T> classType) throws DatabaseException {
         return LocalDatabaseManager.deserialize(data,classType);
    }
}