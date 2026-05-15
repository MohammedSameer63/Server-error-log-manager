package com.errorlog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBConnection {

    // NOT static final — read fresh every time so Docker env vars are picked up
    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {

        // Read every time — never cache at class load
        String rawUrl = System.getenv("DATABASE_URL");

        System.out.println("DATABASE_URL set: " + (rawUrl != null && !rawUrl.isEmpty()));

        if (rawUrl == null || rawUrl.isEmpty()) {
            throw new SQLException("DATABASE_URL environment variable is not set.");
        }

        if (connection == null || connection.isClosed()) {
            connection = createConnection(rawUrl);
        }

        return connection;
    }

    private static Connection createConnection(String rawUrl) throws SQLException {

        // Strip any existing query params
        String clean = rawUrl.contains("?")
            ? rawUrl.substring(0, rawUrl.indexOf("?"))
            : rawUrl;

        // Normalize scheme to jdbc:postgresql://
        String jdbc = clean
            .replace("postgresql://", "jdbc:postgresql://")
            .replace("postgres://",   "jdbc:postgresql://");

        // Parse: jdbc:postgresql://user:pass@host(:port)/dbname
        String withoutScheme = jdbc.substring("jdbc:postgresql://".length());
        int at       = withoutScheme.lastIndexOf("@");
        String userInfo  = withoutScheme.substring(0, at);          // user:pass
        String hostAndDb = withoutScheme.substring(at + 1);         // host/dbname  or host:port/dbname

        String host = hostAndDb.contains("/")
            ? hostAndDb.substring(0, hostAndDb.indexOf("/"))
            : hostAndDb;
        String dbName = hostAndDb.contains("/")
            ? hostAndDb.substring(hostAndDb.indexOf("/") + 1)
            : "";

        // Inject default port if missing
        String hostWithPort = host.contains(":") ? host : host + ":5432";

        // Split user:pass
        String user = userInfo.contains(":") ? userInfo.substring(0, userInfo.indexOf(":")) : userInfo;
        String pass = userInfo.contains(":") ? userInfo.substring(userInfo.indexOf(":") + 1) : "";

        // Final JDBC URL
        String finalUrl = "jdbc:postgresql://" + hostWithPort + "/" + dbName;

        // Use Properties to pass credentials and SSL separately
        // This avoids any URL encoding issues with special chars in password
        Properties props = new Properties();
        props.setProperty("user",     user);
        props.setProperty("password", pass);

        // Internal Render hosts have no dots in hostname
        boolean isInternal = !host.replace(":" + (host.contains(":") ? host.split(":")[1] : ""), "").contains(".");
        props.setProperty("sslmode", isInternal ? "disable" : "require");

        System.out.println("Final JDBC URL : " + finalUrl);
        System.out.println("User           : " + user);
        System.out.println("Host           : " + hostWithPort);
        System.out.println("Database       : " + dbName);
        System.out.println("SSL mode       : " + props.getProperty("sslmode"));

        try {
            Connection conn = DriverManager.getConnection(finalUrl, props);
            System.out.println("✅ Database connected successfully!");
            return conn;
        } catch (SQLException e) {
            System.err.println("❌ Connection FAILED!");
            System.err.println("   Message   : " + e.getMessage());
            System.err.println("   SQL State : " + e.getSQLState());
            System.err.println("   Error Code: " + e.getErrorCode());
            throw e;
        }
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
            System.out.println("✅ Table 'server_logs' ready.");
        } catch (SQLException e) {
            System.err.println("❌ FAILED to initialize table: " + e.getMessage());
            System.err.println("   SQL State : " + e.getSQLState());
            System.err.println("   Error Code: " + e.getErrorCode());
        }
    }
}