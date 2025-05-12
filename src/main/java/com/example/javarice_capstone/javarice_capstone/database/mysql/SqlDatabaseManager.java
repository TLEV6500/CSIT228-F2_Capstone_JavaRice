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
    private static volatile SqlDatabaseManager instance;
    private static Connection connection;
    private static boolean useDedicatedTables;
    private static String databaseName = "JavaRiceUnoDb";
    private static final String metadataTableName = "metadata";
    private static final String gameDataTableName = "game_data";
    private static final Set<String> initializedTables = ConcurrentHashMap.newKeySet();
    private final PreparedStatement dedicatedTableSaveStmt;
    private final PreparedStatement gameDataTableSaveStmt;

    public static void initialize(Connection connection, String databaseName, boolean useDedicatedTables) throws DatabaseException {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        SqlDatabaseManager.databaseName = databaseName;
        SqlDatabaseManager.connection = connection;
        SqlDatabaseManager.useDedicatedTables = useDedicatedTables;
        if (instance == null) {
            instance = new SqlDatabaseManager();
        }
    }

    public static SqlDatabaseManager getInstance() {
        if (connection == null) throw new IllegalStateException(SqlDatabaseManager.class.getSimpleName() + " must be first be initialized by calling `initialize()` with appropriate the arguments");
        return instance;
    }

    private SqlDatabaseManager() throws DatabaseException {
        try {
            if (useDedicatedTables) {
                dedicatedTableSaveStmt = connection.prepareStatement(
                        "INSERT INTO ? (id, data) VALUES (?, ?) " +
                                "ON DUPLICATE KEY UPDATE data = VALUES(data)");
                gameDataTableSaveStmt = null;
            } else {
                gameDataTableSaveStmt = connection.prepareStatement(
                        "INSERT INTO game_data (id, data_type, content) " +
                                "VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE content = VALUES(content)");
                dedicatedTableSaveStmt = null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to prepare SQL statements for dedicated or gamedata tables", e);
        }
        initializeDatabase();
    }

    @Override
    public void close() throws Exception {
        logout();
    }

    @Override
    public void logout() throws DatabaseException, UnsupportedOperationException {
        synchronized (this) {
            try {
                initializedTables.clear();

                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to close database connection: " + e.getMessage());
            }
        }
    }

    @Override
    public int saveData(SerializableGameData data) throws DatabaseException {
        try {
            if (useDedicatedTables) {
                String tableName = data.getClass().getSimpleName();
                ensureTableForType(data.getClass());

                synchronized (dedicatedTableSaveStmt) {
                    dedicatedTableSaveStmt.setString(1, tableName);
                    dedicatedTableSaveStmt.setInt(2, data.getId());
                    dedicatedTableSaveStmt.setBytes(3, serialize(data));
                    dedicatedTableSaveStmt.executeUpdate();
                }
            } else {
                synchronized (gameDataTableSaveStmt) {
                    gameDataTableSaveStmt.setInt(1, data.getId());
                    gameDataTableSaveStmt.setString(2, data.getClass().getName());
                    gameDataTableSaveStmt.setBytes(3, serialize(data));
                    gameDataTableSaveStmt.executeUpdate();
                }
            }
            return data.getId();
        } catch (SQLException e) {
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
                    T data = deserialize(serializedData, classType);
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

    public void initializeDatabase() throws DatabaseException {
        int dbInitRes = executeTransaction((db)-> {
            Statement stmt = db.createStatement();
            return stmt.executeUpdate("CREATE DATABASE " + databaseName);
        });
        System.out.println("Created database " + databaseName + " with result " + dbInitRes);
        initializeCoreTables();
    }

    private void initializeCoreTables() {
        synchronized (this) {
            try {
                if (!tableExists(metadataTableName)) {
                    createMetadataTable();
                }
                if (!useDedicatedTables && !tableExists(gameDataTableName)) {
                    createGameDataTable();
                }
                initializedTables.add(metadataTableName);
                initializedTables.add(gameDataTableName);
            } catch (SQLException e) {
                throw new RuntimeException("Database initialization failed", e);
            }
        }
    }

    private void createGameDataTable() throws SQLException {
        String sql = """
        CREATE TABLE IF NOT EXISTS game_data (
            id INT NOT NULL,
            data_type VARCHAR(255) NOT NULL,
            content BLOB NOT NULL,
            version INT DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (id, data_type)
        )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createMetadataTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS metadata (
                type_name VARCHAR(255) PRIMARY KEY,
                table_name VARCHAR(100) NOT NULL,
                schema_version INT NOT NULL,
                UNIQUE(table_name)
            )""";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void ensureTableForType(Class<? extends SerializableGameData> classType)
            throws SQLException {
        String tableName = classType.getSimpleName();
        if (!initializedTables.contains(tableName)) {
            synchronized (this) {
                if (!initializedTables.contains(tableName)) {
                    if (useDedicatedTables) {
                        createDedicatedTable(classType);
                    }
                    else if (!tableExists(gameDataTableName)) {
                        createGameDataTable();
                    }
                    registerTypeInMetadata(classType);
                    initializedTables.add(tableName);
                }
            }
        }
    }

    private void createDedicatedTable(Class<? extends SerializableGameData> classType)
            throws SQLException {
        String tableName = classType.getSimpleName();
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id INT PRIMARY KEY,
                data BLOB NOT NULL,
                version INT DEFAULT 1,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )""", tableName);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

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

    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData dbMeta = connection.getMetaData();
        try (ResultSet rs = dbMeta.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
}