package com.spendly.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Automatically initializes database schema and seed data if not already initialized.
 */
public class DBInitializer {
    private static final Logger LOGGER = Logger.getLogger(DBInitializer.class.getName());

    public static void initializeDatabase() {
        try {
            // First check if database exists or create it
            ensureDatabaseExists();

            // Check if 'users' table exists
            try (Connection conn = DBConnection.getConnection()) {
                if (!tableExists(conn, "users")) {
                    LOGGER.info("Tables not found. Running database.sql initialization...");
                    executeSqlScript(conn);
                    LOGGER.info("Database schema & seed data initialized successfully!");
                } else {
                    LOGGER.info("Database schema already exists and is ready.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database auto-initialization note: " + e.getMessage());
        }
    }

    private static void ensureDatabaseExists() {
        String baseJdbcUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
        String[] passwords = new String[]{"root", "", "password", "admin", "123456", "root123"};

        for (String pwd : passwords) {
            try (Connection conn = DriverManager.getConnection(baseJdbcUrl, "root", pwd);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS spendly_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
                LOGGER.info("Verified spendly_db database exists on MySQL.");
                return;
            } catch (SQLException ignored) {
            }
        }
    }

    private static boolean tableExists(Connection conn, String tableName) {
        try {
            ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null);
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private static void executeSqlScript(Connection conn) {
        File sqlFile = new File("database.sql");
        if (!sqlFile.exists()) {
            sqlFile = new File("../database.sql");
        }

        try (BufferedReader reader = sqlFile.exists() 
                ? new BufferedReader(new FileReader(sqlFile))
                : new BufferedReader(new InputStreamReader(
                    DBInitializer.class.getClassLoader().getResourceAsStream("database.sql") != null
                        ? DBInitializer.class.getClassLoader().getResourceAsStream("database.sql")
                        : InputStream.nullInputStream()
                  ))) {

            StringBuilder sb = new StringBuilder();
            String line;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("USE spendly_db;");
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*")) {
                        continue;
                    }
                    sb.append(line).append(" ");
                    if (line.endsWith(";")) {
                        String sql = sb.toString().trim();
                        if (sql.endsWith(";")) {
                            sql = sql.substring(0, sql.length() - 1);
                        }
                        if (!sql.isEmpty() && !sql.toUpperCase().startsWith("CREATE DATABASE") && !sql.toUpperCase().startsWith("USE")) {
                            try {
                                stmt.execute(sql);
                            } catch (SQLException e) {
                                LOGGER.warning("SQL execution note: " + e.getMessage() + " for: " + sql);
                            }
                        }
                        sb.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not execute database.sql script automatically", e);
        }
    }
}
