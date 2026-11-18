package com.spendly.dao;

import com.spendly.config.DBConnection;
import com.spendly.model.Goal;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Savings Goals.
 */
public class GoalDAO {

    /**
     * Gets all goals for a user.
     */
    public List<Goal> getAllForUser(int userId) throws SQLException {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE user_id = ? ORDER BY status ASC, target_date ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Goal g = mapRow(rs);
                g.computeMetrics();
                list.add(g);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Gets a goal by ID.
     */
    public Goal getById(int goalId, int userId) throws SQLException {
        String sql = "SELECT * FROM goals WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, goalId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Goal g = mapRow(rs);
                g.computeMetrics();
                return g;
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Creates a new savings goal.
     */
    public Goal create(Goal g) throws SQLException {
        String sql = "INSERT INTO goals (user_id, title, target_amount, saved_amount, target_date, category_name, icon, color, status, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, g.getUserId());
            stmt.setString(2, g.getTitle());
            stmt.setBigDecimal(3, g.getTargetAmount());
            stmt.setBigDecimal(4, g.getSavedAmount() != null ? g.getSavedAmount() : BigDecimal.ZERO);
            stmt.setDate(5, g.getTargetDate());
            stmt.setString(6, g.getCategoryName() != null ? g.getCategoryName() : "General");
            stmt.setString(7, g.getIcon() != null ? g.getIcon() : "piggy-bank");
            stmt.setString(8, g.getColor() != null ? g.getColor() : "#10b981");
            stmt.setString(9, g.getStatus() != null ? g.getStatus() : "ACTIVE");
            stmt.setString(10, g.getNotes());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return getById(id, g.getUserId());
                }
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Updates an existing savings goal.
     */
    public boolean update(Goal g) throws SQLException {
        String sql = "UPDATE goals SET title = ?, target_amount = ?, saved_amount = ?, target_date = ?, " +
                     "category_name = ?, icon = ?, color = ?, status = ?, notes = ? " +
                     "WHERE id = ? AND user_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, g.getTitle());
            stmt.setBigDecimal(2, g.getTargetAmount());
            stmt.setBigDecimal(3, g.getSavedAmount());
            stmt.setDate(4, g.getTargetDate());
            stmt.setString(5, g.getCategoryName());
            stmt.setString(6, g.getIcon());
            stmt.setString(7, g.getColor());
            stmt.setString(8, g.getStatus());
            stmt.setString(9, g.getNotes());
            stmt.setInt(10, g.getId());
            stmt.setInt(11, g.getUserId());

            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    /**
     * Adds funds to an existing savings goal.
     */
    public boolean addFunds(int goalId, int userId, BigDecimal depositAmount) throws SQLException {
        String sql = "UPDATE goals SET saved_amount = saved_amount + ?, " +
                     "status = CASE WHEN saved_amount + ? >= target_amount THEN 'COMPLETED' ELSE status END " +
                     "WHERE id = ? AND user_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBigDecimal(1, depositAmount);
            stmt.setBigDecimal(2, depositAmount);
            stmt.setInt(3, goalId);
            stmt.setInt(4, userId);

            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    /**
     * Deletes a savings goal.
     */
    public boolean delete(int goalId, int userId) throws SQLException {
        String sql = "DELETE FROM goals WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, goalId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    private Goal mapRow(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        g.setUserId(rs.getInt("user_id"));
        g.setTitle(rs.getString("title"));
        g.setTargetAmount(rs.getBigDecimal("target_amount"));
        g.setSavedAmount(rs.getBigDecimal("saved_amount"));
        g.setTargetDate(rs.getDate("target_date"));
        g.setCategoryName(rs.getString("category_name"));
        g.setIcon(rs.getString("icon"));
        g.setColor(rs.getString("color"));
        g.setStatus(rs.getString("status"));
        g.setNotes(rs.getString("notes"));
        g.setCreatedAt(rs.getTimestamp("created_at"));
        g.setUpdatedAt(rs.getTimestamp("updated_at"));
        return g;
    }
}
