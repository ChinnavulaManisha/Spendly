package com.spendly.dao;

import com.spendly.config.DBConnection;
import com.spendly.model.PaymentAccount;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Payment Accounts and UPI IDs.
 */
public class PaymentAccountDAO {

    /**
     * Gets all payment accounts and UPI handles for a user.
     */
    public List<PaymentAccount> getAllForUser(int userId) throws SQLException {
        List<PaymentAccount> list = new ArrayList<>();
        String sql = "SELECT * FROM payment_accounts WHERE user_id = ? ORDER BY is_default DESC, created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Gets account by ID.
     */
    public PaymentAccount getById(int accountId, int userId) throws SQLException {
        String sql = "SELECT * FROM payment_accounts WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, accountId);
            stmt.setInt(2, userId);
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
     * Creates a new payment account or UPI handle.
     */
    public PaymentAccount create(PaymentAccount acc) throws SQLException {
        String sql = "INSERT INTO payment_accounts (user_id, account_type, provider_name, identifier_mask, account_holder_name, balance, is_default, is_verified) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, acc.getUserId());
            stmt.setString(2, acc.getAccountType() != null ? acc.getAccountType() : "UPI");
            stmt.setString(3, acc.getProviderName());
            stmt.setString(4, acc.getIdentifierMask());
            stmt.setString(5, acc.getAccountHolderName());
            stmt.setBigDecimal(6, acc.getBalance() != null ? acc.getBalance() : BigDecimal.ZERO);
            stmt.setBoolean(7, acc.isDefault());
            stmt.setBoolean(8, acc.isVerified());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return getById(id, acc.getUserId());
                }
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Updates the last synced timestamp of an account.
     */
    public boolean updateLastSynced(int accountId, int userId) throws SQLException {
        String sql = "UPDATE payment_accounts SET last_synced_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, accountId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    /**
     * Deletes a payment account.
     */
    public boolean delete(int accountId, int userId) throws SQLException {
        String sql = "DELETE FROM payment_accounts WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, accountId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    private PaymentAccount mapRow(ResultSet rs) throws SQLException {
        PaymentAccount a = new PaymentAccount();
        a.setId(rs.getInt("id"));
        a.setUserId(rs.getInt("user_id"));
        a.setAccountType(rs.getString("account_type"));
        a.setProviderName(rs.getString("provider_name"));
        a.setIdentifierMask(rs.getString("identifier_mask"));
        a.setAccountHolderName(rs.getString("account_holder_name"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setDefault(rs.getBoolean("is_default"));
        a.setVerified(rs.getBoolean("is_verified"));
        a.setLastSyncedAt(rs.getTimestamp("last_synced_at"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        return a;
    }
}
