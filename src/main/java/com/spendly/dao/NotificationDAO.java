package com.spendly.dao;

import com.spendly.config.DBConnection;
import com.spendly.model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User Notifications.
 */
public class NotificationDAO {

    public List<Notification> getAllForUser(int userId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 20";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Notification n = mapRow(rs);
                list.add(n);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return list;
    }

    public int getUnreadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return 0;
    }

    public Notification create(Notification n) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, title, message, type, is_read, action_url) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, n.getUserId());
            stmt.setString(2, n.getTitle());
            stmt.setString(3, n.getMessage());
            stmt.setString(4, n.getType() != null ? n.getType() : "INFO");
            stmt.setBoolean(5, n.isRead());
            stmt.setString(6, n.getActionUrl());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    n.setId(rs.getInt(1));
                    return n;
                }
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    public boolean markAsRead(int notificationId, int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, notificationId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    public boolean markAllAsRead(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setRead(rs.getBoolean("is_read"));
        n.setActionUrl(rs.getString("action_url"));
        n.setCreatedAt(rs.getTimestamp("created_at"));
        return n;
    }
}
