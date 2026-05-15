package com.errorlog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static final String DB_URL = System.getenv("DATABASE_URL");
    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (DB_URL == null || DB_URL.isEmpty()) {
            throw new SQLException(
                "DATABASE_URL environment variable is not set."
            );
        }

        if (connection == null || connection.isClosed()) {
            String jdbcUrl = DB_URL.replace("postgresql://", "jdbc:postgresql://").replace("postgres://", "jdbc:postgresql://")
                                   + "?sslmode=require";
            connection = DriverManager.getConnection(jdbcUrl);
            System.out.println("Database connected successfully.");
        }

        return connection;
    }

    public static void initializeTable() {
        // Regular String — no text block, works with Java 11+
        String sql = "CREATE TABLE IF NOT EXISTS server_logs ("
                   + "error_id   SERIAL PRIMARY KEY,"
                   + "error_type VARCHAR(50)  NOT NULL,"
                   + "message    TEXT         NOT NULL,"
                   + "created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP"
                   + ")";

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'server_logs' ready.");
        } catch (SQLException e) {
            System.err.println("Failed to initialize table: " + e.getMessage());
        }
    }
}