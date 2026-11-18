package com.spendly.dao;

import com.spendly.config.DBConnection;
import com.spendly.model.User;
import com.spendly.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for User entities.
 */
public class UserDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    /**
     * Registers a new user with secure password hash & salt.
     */
    public User register(String fullName, String email, String plainPassword, String currency) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password_hash, salt, currency, theme_preference) VALUES (?, ?, ?, ?, ?, ?)";
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(plainPassword, salt);
        String curr = (currency != null && !currency.trim().isEmpty()) ? currency : "USD";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, fullName);
            stmt.setString(2, email.toLowerCase().trim());
            stmt.setString(3, passwordHash);
            stmt.setString(4, salt);
            stmt.setString(5, curr);
            stmt.setString(6, "dark");

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int userId = rs.getInt(1);
                    User user = new User(userId, fullName, email.toLowerCase().trim(), curr, "dark");
                    user.setSalt(salt);
                    user.setPasswordHash(passwordHash);

                    // Create default payment account for new user
                    createInitialAccount(conn, userId, fullName);
                    return user;
                }
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    private void createInitialAccount(Connection conn, int userId, String fullName) {
        String sql = "INSERT INTO payment_accounts (user_id, account_type, provider_name, identifier_mask, account_holder_name, balance, is_default) " +
                     "VALUES (?, 'UPI', 'Google Pay UPI', ?, ?, 1000.00, TRUE)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            String upiHandle = fullName.toLowerCase().replaceAll("[^a-z0-9]", "") + "@okhdfcbank";
            stmt.setInt(1, userId);
            stmt.setString(2, upiHandle);
            stmt.setString(3, fullName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to create initial payment account for user " + userId, e);
        }
    }

    /**
     * Authenticates user with email and password.
     */
    public User authenticate(String email, String plainPassword) throws SQLException {
        String sql = "SELECT id, full_name, email, password_hash, salt, currency, theme_preference, avatar_url, created_at FROM users WHERE email = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email.toLowerCase().trim());
            rs = stmt.executeQuery();

            if (rs.next()) {
                String storedSalt = rs.getString("salt");
                String storedHash = rs.getString("password_hash");

                if (PasswordUtil.verifyPassword(plainPassword, storedSalt, storedHash)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setFullName(rs.getString("full_name"));
                    user.setEmail(rs.getString("email"));
                    user.setCurrency(rs.getString("currency"));
                    user.setThemePreference(rs.getString("theme_preference"));
                    user.setAvatarUrl(rs.getString("avatar_url"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    return user;
                }
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Finds user by ID.
     */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT id, full_name, email, currency, theme_preference, avatar_url, created_at FROM users WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setFullName(rs.getString("full_name"));
                user.setEmail(rs.getString("email"));
                user.setCurrency(rs.getString("currency"));
                user.setThemePreference(rs.getString("theme_preference"));
                user.setAvatarUrl(rs.getString("avatar_url"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                return user;
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Checks if email already exists.
     */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT id FROM users WHERE email = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email.toLowerCase().trim());
            rs = stmt.executeQuery();
            return rs.next();
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
    }

    /**
     * Updates theme preference (dark/light).
     */
    public boolean updateThemePreference(int userId, String theme) throws SQLException {
        String sql = "UPDATE users SET theme_preference = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, theme != null ? theme : "dark");
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    /**
     * Resets / updates user password (for Forgot Password / Profile).
     */
    public boolean resetPassword(String email, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE email = ?";
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(newPassword, salt);

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, passwordHash);
            stmt.setString(2, salt);
            stmt.setString(3, email.toLowerCase().trim());
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    /**
     * Updates user profile (full name, currency).
     */
    public boolean updateProfile(int userId, String fullName, String currency) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, currency = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, fullName);
            stmt.setString(2, currency);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }
}
