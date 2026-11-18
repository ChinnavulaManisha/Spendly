package com.spendly.dao;

import com.spendly.config.DBConnection;
import com.spendly.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Categories.
 */
public class CategoryDAO {

    /**
     * Gets all default categories plus custom categories created by the specific user.
     */
    public List<Category> getAllForUser(int userId) throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, user_id, name, type, icon, color, is_custom, created_at " +
                     "FROM categories " +
                     "WHERE user_id IS NULL OR user_id = ? " +
                     "ORDER BY (user_id IS NOT NULL), type DESC, name ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Category c = mapRow(rs);
                list.add(c);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Finds a category by its ID.
     */
    public Category getById(int categoryId) throws SQLException {
        String sql = "SELECT id, user_id, name, type, icon, color, is_custom, created_at FROM categories WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, categoryId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Creates a new custom category for a user.
     */
    public Category createCustomCategory(int userId, String name, String type, String icon, String color) throws SQLException {
        String sql = "INSERT INTO categories (user_id, name, type, icon, color, is_custom) VALUES (?, ?, ?, ?, ?, TRUE)";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, userId);
            stmt.setString(2, name.trim());
            stmt.setString(3, (type != null && type.equalsIgnoreCase("INCOME")) ? "INCOME" : "EXPENSE");
            stmt.setString(4, (icon != null && !icon.trim().isEmpty()) ? icon : "tag");
            stmt.setString(5, (color != null && !color.trim().isEmpty()) ? color : "#6366f1");

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new Category(id, userId, name.trim(), type, icon, color, true);
                }
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Deletes a custom category owned by the user.
     */
    public boolean deleteCustomCategory(int categoryId, int userId) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ? AND user_id = ? AND is_custom = TRUE";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, categoryId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        int uId = rs.getInt("user_id");
        c.setUserId(rs.wasNull() ? null : uId);
        c.setName(rs.getString("name"));
        c.setType(rs.getString("type"));
        c.setIcon(rs.getString("icon"));
        c.setColor(rs.getString("color"));
        c.setCustom(rs.getBoolean("is_custom"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }
}
