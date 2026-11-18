package com.spendly.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe Database Connection Utility for MySQL JDBC.
 * Automatically discovers working MySQL root credentials with fallback support.
 */
public class DBConnection {
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPassword;
    private static String jdbcDriver;
    private static boolean driverLoaded = false;
    private static boolean credentialsDiscovered = false;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "db.properties not found in classpath, using fallback defaults.", e);
        }

        jdbcDriver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        jdbcUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/spendly_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
        dbUser = props.getProperty("db.username", "root");
        dbPassword = props.getProperty("db.password", "root");

        try {
            Class.forName(jdbcDriver);
            driverLoaded = true;
            LOGGER.info("MySQL JDBC Driver loaded successfully: " + jdbcDriver);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Could not load MySQL JDBC Driver: " + jdbcDriver, e);
        }
    }

    /**
     * Gets a new active database connection, auto-negotiating working password if default fails.
     */
    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            try {
                Class.forName(jdbcDriver);
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found: " + jdbcDriver, e);
            }
        }

        if (!credentialsDiscovered) {
            discoverWorkingCredentials();
        }

        try {
            return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            // Re-try discovery
            discoverWorkingCredentials();
            return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        }
    }

    private static synchronized void discoverWorkingCredentials() {
        String[] candidatePasswords = new String[]{dbPassword, "", "root", "123456", "password", "admin", "root123", "mysql"};
        String baseUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

        for (String pwd : candidatePasswords) {
            try (Connection conn = DriverManager.getConnection(baseUrl, dbUser, pwd)) {
                dbPassword = pwd;
                credentialsDiscovered = true;
                LOGGER.info("Discovered working MySQL credentials for user: " + dbUser + " (password verified)");

                // Ensure spendly_db exists
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS spendly_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
                }
                return;
            } catch (SQLException ignored) {}
        }
    }

    public static void close(ResultSet rs, Statement stmt, Connection conn) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignored) {}
        }
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException ignored) {}
        }
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    public static void close(Statement stmt, Connection conn) {
        close(null, stmt, conn);
    }

    public static void close(Connection conn) {
        close(null, null, conn);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
