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
            throw new SQLException("DATABASE_URL environment variable is not set.");
        }

        if (connection == null || connection.isClosed()) {
            String jdbcUrl = buildJdbcUrl(DB_URL);
            System.out.println("Connecting with URL: " + jdbcUrl);
            connection = DriverManager.getConnection(jdbcUrl);
            System.out.println("Database connected successfully.");
        }

        return connection;
    }

    private static String buildJdbcUrl(String rawUrl) {
        // Step 1 — normalize prefix to jdbc:postgresql://
        String url = rawUrl
            .replace("postgresql://", "jdbc:postgresql://")
            .replace("postgres://",   "jdbc:postgresql://");

        // Step 2 — split off any existing query string
        String base  = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
        String query = url.contains("?") ? url.substring(url.indexOf("?") + 1) : "";

        // Step 3 — inject :5432 port if missing
        // URL format: jdbc:postgresql://user:pass@host/dbname
        // We need:    jdbc:postgresql://user:pass@host:5432/dbname
        String prefix = "jdbc:postgresql://";
        String rest   = base.substring(prefix.length()); // user:pass@host/dbname

        int atSign    = rest.lastIndexOf("@");
        String credentials = rest.substring(0, atSign);       // user:pass
        String hostAndDb   = rest.substring(atSign + 1);      // host/dbname

        // Check if port already exists (host:port/dbname)
        String hostPart = hostAndDb.contains("/")
            ? hostAndDb.substring(0, hostAndDb.indexOf("/"))
            : hostAndDb;

        if (!hostPart.contains(":")) {
            // No port — inject 5432
            hostAndDb = hostPart + ":5432" + hostAndDb.substring(hostPart.length());
        }

        // Step 4 — rebuild clean URL with sslmode=require
        boolean isInternal = !hostPart.contains(".");
        String sslParam = isInternal ? "sslmode=disable" : "sslmode=require";
        String finalUrl = prefix + credentials + "@" + hostAndDb + "?" + sslParam;
        return finalUrl;
    }

    public static void initializeTable() {
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