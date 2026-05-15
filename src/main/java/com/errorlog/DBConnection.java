package com.errorlog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    // ─────────────────────────────────────────────────
    // Render provides DATABASE_URL as an environment
    // variable automatically when you link a PostgreSQL
    // database to your web service
    // ─────────────────────────────────────────────────
    private static final String DB_URL = System.getenv("DATABASE_URL");

    // Single shared connection for the whole app
    private static Connection connection = null;

    // ─────────────────────────────────────────────────
    // Get connection — creates one if not exists
    // This is the Singleton pattern:
    //   only ONE connection object is ever created
    //   every class reuses the same one
    // ─────────────────────────────────────────────────
    public static Connection getConnection() throws SQLException {
        if (DB_URL == null || DB_URL.isEmpty()) {
            throw new SQLException(
                "DATABASE_URL environment variable is not set. " +
                "Link a PostgreSQL database on Render."
            );
        }

        if (connection == null || connection.isClosed()) {
            // Render's DATABASE_URL starts with "postgres://"
            // JDBC needs "jdbc:postgresql://" — so we convert it
            String jdbcUrl = DB_URL.replace("postgres://", "jdbc:postgresql://")
                                   + "?sslmode=require";
            connection = DriverManager.getConnection(jdbcUrl);
            System.out.println("Database connected successfully.");
        }

        return connection;
    }

    // ─────────────────────────────────────────────────
    // Create the table on first run if it doesn't exist
    // Called once when the server starts in Main.java
    // ─────────────────────────────────────────────────
    public static void initializeTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS server_logs (
                error_id   SERIAL PRIMARY KEY,
                error_type VARCHAR(50)  NOT NULL,
                message    TEXT         NOT NULL,
                created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
            );
        """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'server_logs' ready.");
        } catch (SQLException e) {
            System.err.println("Failed to initialize table: " + e.getMessage());
        }
    }
}
