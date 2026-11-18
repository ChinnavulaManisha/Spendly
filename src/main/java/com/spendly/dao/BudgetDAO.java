package com.spendly.dao;

import com.spendly.config.DBConnection;
import com.spendly.model.Budget;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Budgets with live spending metrics calculations.
 */
public class BudgetDAO {

    /**
     * Gets all budgets for a user in a given month/year with computed spent amounts.
     */
    public List<Budget> getAllForUserMonth(int userId, int month, int year) throws SQLException {
        List<Budget> list = new ArrayList<>();
        String sql = "SELECT b.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color, " +
                     "(CASE " +
                     "  WHEN b.category_id IS NULL THEN " +
                     "    (SELECT COALESCE(SUM(t.amount), 0) FROM transactions t " +
                     "     WHERE t.user_id = b.user_id AND t.type = 'EXPENSE' " +
                     "     AND MONTH(t.transaction_date) = b.month_val AND YEAR(t.transaction_date) = b.year_val) " +
                     "  ELSE " +
                     "    (SELECT COALESCE(SUM(t.amount), 0) FROM transactions t " +
                     "     WHERE t.user_id = b.user_id AND t.category_id = b.category_id AND t.type = 'EXPENSE' " +
                     "     AND MONTH(t.transaction_date) = b.month_val AND YEAR(t.transaction_date) = b.year_val) " +
                     "END) AS spent_amount " +
                     "FROM budgets b " +
                     "LEFT JOIN categories c ON b.category_id = c.id " +
                     "WHERE b.user_id = ? AND b.month_val = ? AND b.year_val = ? " +
                     "ORDER BY (b.category_id IS NULL) DESC, b.amount DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Budget b = mapRow(rs);
                b.computeMetrics();
                list.add(b);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Gets a budget by its ID.
     */
    public Budget getById(int budgetId, int userId) throws SQLException {
        String sql = "SELECT b.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color, " +
                     "(CASE " +
                     "  WHEN b.category_id IS NULL THEN " +
                     "    (SELECT COALESCE(SUM(t.amount), 0) FROM transactions t " +
                     "     WHERE t.user_id = b.user_id AND t.type = 'EXPENSE' " +
                     "     AND MONTH(t.transaction_date) = b.month_val AND YEAR(t.transaction_date) = b.year_val) " +
                     "  ELSE " +
                     "    (SELECT COALESCE(SUM(t.amount), 0) FROM transactions t " +
                     "     WHERE t.user_id = b.user_id AND t.category_id = b.category_id AND t.type = 'EXPENSE' " +
                     "     AND MONTH(t.transaction_date) = b.month_val AND YEAR(t.transaction_date) = b.year_val) " +
                     "END) AS spent_amount " +
                     "FROM budgets b " +
                     "LEFT JOIN categories c ON b.category_id = c.id " +
                     "WHERE b.id = ? AND b.user_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, budgetId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Budget b = mapRow(rs);
                b.computeMetrics();
                return b;
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Creates a new budget.
     */
    public Budget create(Budget b) throws SQLException {
        String sql = "INSERT INTO budgets (user_id, category_id, name, amount, period, month_val, year_val, alert_threshold) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, b.getUserId());
            if (b.getCategoryId() != null && b.getCategoryId() > 0) {
                stmt.setInt(2, b.getCategoryId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setString(3, b.getName());
            stmt.setBigDecimal(4, b.getAmount());
            stmt.setString(5, b.getPeriod() != null ? b.getPeriod() : "MONTHLY");
            stmt.setInt(6, b.getMonthVal());
            stmt.setInt(7, b.getYearVal());
            stmt.setInt(8, b.getAlertThreshold() > 0 ? b.getAlertThreshold() : 80);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return getById(id, b.getUserId());
                }
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Updates an existing budget.
     */
    public boolean update(Budget b) throws SQLException {
        String sql = "UPDATE budgets SET category_id = ?, name = ?, amount = ?, alert_threshold = ? WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            if (b.getCategoryId() != null && b.getCategoryId() > 0) {
                stmt.setInt(1, b.getCategoryId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, b.getName());
            stmt.setBigDecimal(3, b.getAmount());
            stmt.setInt(4, b.getAlertThreshold());
            stmt.setInt(5, b.getId());
            stmt.setInt(6, b.getUserId());

            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    /**
     * Deletes a budget.
     */
    public boolean delete(int budgetId, int userId) throws SQLException {
        String sql = "DELETE FROM budgets WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, budgetId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    private Budget mapRow(ResultSet rs) throws SQLException {
        Budget b = new Budget();
        b.setId(rs.getInt("id"));
        b.setUserId(rs.getInt("user_id"));
        int cId = rs.getInt("category_id");
        b.setCategoryId(rs.wasNull() ? null : cId);
        b.setName(rs.getString("name"));
        b.setAmount(rs.getBigDecimal("amount"));
        b.setPeriod(rs.getString("period"));
        b.setMonthVal(rs.getInt("month_val"));
        b.setYearVal(rs.getInt("year_val"));
        b.setAlertThreshold(rs.getInt("alert_threshold"));
        b.setCategoryName(rs.getString("category_name"));
        b.setCategoryIcon(rs.getString("category_icon"));
        b.setCategoryColor(rs.getString("category_color"));
        b.setSpentAmount(rs.getBigDecimal("spent_amount"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        b.setUpdatedAt(rs.getTimestamp("updated_at"));
        return b;
    }
}
