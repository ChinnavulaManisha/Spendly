package com.spendly.dao;

import com.spendly.config.DBConnection;
import com.spendly.model.RecurringPayment;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Recurring Payments & Subscriptions.
 */
public class RecurringPaymentDAO {

    /**
     * Gets all recurring payments for a user.
     */
    public List<RecurringPayment> getAllForUser(int userId) throws SQLException {
        List<RecurringPayment> list = new ArrayList<>();
        String sql = "SELECT r.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color " +
                     "FROM recurring_payments r " +
                     "LEFT JOIN categories c ON r.category_id = c.id " +
                     "WHERE r.user_id = ? " +
                     "ORDER BY r.next_due_date ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                RecurringPayment rp = mapRow(rs);
                rp.computeMetrics();
                list.add(rp);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Gets upcoming recurring payments within the next N days.
     */
    public List<RecurringPayment> getUpcomingPayments(int userId, int days) throws SQLException {
        List<RecurringPayment> list = new ArrayList<>();
        String sql = "SELECT r.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color " +
                     "FROM recurring_payments r " +
                     "LEFT JOIN categories c ON r.category_id = c.id " +
                     "WHERE r.user_id = ? AND r.status = 'ACTIVE' " +
                     "AND r.next_due_date BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL ? DAY) " +
                     "ORDER BY r.next_due_date ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, days > 0 ? days : 15);
            rs = stmt.executeQuery();

            while (rs.next()) {
                RecurringPayment rp = mapRow(rs);
                rp.computeMetrics();
                list.add(rp);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Calculates total monthly commitment for active recurring payments.
     */
    public BigDecimal getTotalMonthlyRecurring(int userId) throws SQLException {
        String sql = "SELECT SUM(" +
                     "  CASE " +
                     "    WHEN billing_cycle = 'MONTHLY' THEN amount " +
                     "    WHEN billing_cycle = 'WEEKLY' THEN amount * 4 " +
                     "    WHEN billing_cycle = 'QUARTERLY' THEN amount / 3 " +
                     "    WHEN billing_cycle = 'YEARLY' THEN amount / 12 " +
                     "    ELSE amount " +
                     "  END" +
                     ") AS total_monthly " +
                     "FROM recurring_payments " +
                     "WHERE user_id = ? AND status = 'ACTIVE'";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal sum = rs.getBigDecimal("total_monthly");
                return sum != null ? sum : BigDecimal.ZERO;
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets a recurring payment by ID.
     */
    public RecurringPayment getById(int recurringId, int userId) throws SQLException {
        String sql = "SELECT r.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color " +
                     "FROM recurring_payments r " +
                     "LEFT JOIN categories c ON r.category_id = c.id " +
                     "WHERE r.id = ? AND r.user_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, recurringId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                RecurringPayment rp = mapRow(rs);
                rp.computeMetrics();
                return rp;
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Creates a new recurring payment.
     */
    public RecurringPayment create(RecurringPayment rp) throws SQLException {
        String sql = "INSERT INTO recurring_payments (user_id, title, merchant, amount, category_id, billing_cycle, next_due_date, payment_method, upi_id, auto_pay, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, rp.getUserId());
            stmt.setString(2, rp.getTitle());
            stmt.setString(3, rp.getMerchant());
            stmt.setBigDecimal(4, rp.getAmount());
            stmt.setInt(5, rp.getCategoryId());
            stmt.setString(6, rp.getBillingCycle() != null ? rp.getBillingCycle() : "MONTHLY");
            stmt.setDate(7, rp.getNextDueDate());
            stmt.setString(8, rp.getPaymentMethod() != null ? rp.getPaymentMethod() : "UPI AutoPay");
            stmt.setString(9, rp.getUpiId());
            stmt.setBoolean(10, rp.isAutoPay());
            stmt.setString(11, rp.getStatus() != null ? rp.getStatus() : "ACTIVE");

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return getById(id, rp.getUserId());
                }
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Updates an existing recurring payment.
     */
    public boolean update(RecurringPayment rp) throws SQLException {
        String sql = "UPDATE recurring_payments SET title = ?, merchant = ?, amount = ?, category_id = ?, " +
                     "billing_cycle = ?, next_due_date = ?, payment_method = ?, upi_id = ?, auto_pay = ?, status = ? " +
                     "WHERE id = ? AND user_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, rp.getTitle());
            stmt.setString(2, rp.getMerchant());
            stmt.setBigDecimal(3, rp.getAmount());
            stmt.setInt(4, rp.getCategoryId());
            stmt.setString(5, rp.getBillingCycle());
            stmt.setDate(6, rp.getNextDueDate());
            stmt.setString(7, rp.getPaymentMethod());
            stmt.setString(8, rp.getUpiId());
            stmt.setBoolean(9, rp.isAutoPay());
            stmt.setString(10, rp.getStatus());
            stmt.setInt(11, rp.getId());
            stmt.setInt(12, rp.getUserId());

            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    /**
     * Deletes a recurring payment.
     */
    public boolean delete(int recurringId, int userId) throws SQLException {
        String sql = "DELETE FROM recurring_payments WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, recurringId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    private RecurringPayment mapRow(ResultSet rs) throws SQLException {
        RecurringPayment rp = new RecurringPayment();
        rp.setId(rs.getInt("id"));
        rp.setUserId(rs.getInt("user_id"));
        rp.setTitle(rs.getString("title"));
        rp.setMerchant(rs.getString("merchant"));
        rp.setAmount(rs.getBigDecimal("amount"));
        rp.setCategoryId(rs.getInt("category_id"));
        rp.setCategoryName(rs.getString("category_name"));
        rp.setCategoryIcon(rs.getString("category_icon"));
        rp.setCategoryColor(rs.getString("category_color"));
        rp.setBillingCycle(rs.getString("billing_cycle"));
        rp.setNextDueDate(rs.getDate("next_due_date"));
        rp.setPaymentMethod(rs.getString("payment_method"));
        rp.setUpiId(rs.getString("upi_id"));
        rp.setAutoPay(rs.getBoolean("auto_pay"));
        rp.setStatus(rs.getString("status"));
        rp.setCreatedAt(rs.getTimestamp("created_at"));
        return rp;
    }
}
