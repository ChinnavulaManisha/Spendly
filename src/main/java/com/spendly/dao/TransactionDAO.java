package com.spendly.dao;

import com.spendly.config.DBConnection;
import com.spendly.model.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Transactions with filtering, sorting, searching, and chart aggregation.
 */
public class TransactionDAO {

    /**
     * Creates a new transaction.
     */
    public Transaction create(Transaction tx) throws SQLException {
        String sql = "INSERT INTO transactions (user_id, type, amount, category_id, merchant, payment_method, upi_id, account_id, transaction_date, transaction_time, notes, is_recurring, is_synced_demo) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, tx.getUserId());
            stmt.setString(2, tx.getType());
            stmt.setBigDecimal(3, tx.getAmount());
            stmt.setInt(4, tx.getCategoryId());
            stmt.setString(5, tx.getMerchant());
            stmt.setString(6, tx.getPaymentMethod());
            stmt.setString(7, tx.getUpiId());
            if (tx.getAccountId() != null) {
                stmt.setInt(8, tx.getAccountId());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }
            stmt.setDate(9, tx.getTransactionDate() != null ? tx.getTransactionDate() : new Date(System.currentTimeMillis()));
            stmt.setTime(10, tx.getTransactionTime() != null ? tx.getTransactionTime() : new Time(System.currentTimeMillis()));
            stmt.setString(11, tx.getNotes());
            stmt.setBoolean(12, tx.isRecurring());
            stmt.setBoolean(13, tx.isSyncedDemo());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    tx.setId(rs.getInt(1));
                    return getById(tx.getId(), tx.getUserId());
                }
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Updates an existing transaction.
     */
    public boolean update(Transaction tx) throws SQLException {
        String sql = "UPDATE transactions SET type = ?, amount = ?, category_id = ?, merchant = ?, payment_method = ?, " +
                     "upi_id = ?, account_id = ?, transaction_date = ?, transaction_time = ?, notes = ?, is_recurring = ? " +
                     "WHERE id = ? AND user_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, tx.getType());
            stmt.setBigDecimal(2, tx.getAmount());
            stmt.setInt(3, tx.getCategoryId());
            stmt.setString(4, tx.getMerchant());
            stmt.setString(5, tx.getPaymentMethod());
            stmt.setString(6, tx.getUpiId());
            if (tx.getAccountId() != null) {
                stmt.setInt(7, tx.getAccountId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }
            stmt.setDate(8, tx.getTransactionDate());
            stmt.setTime(9, tx.getTransactionTime());
            stmt.setString(10, tx.getNotes());
            stmt.setBoolean(11, tx.isRecurring());
            stmt.setInt(12, tx.getId());
            stmt.setInt(13, tx.getUserId());

            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    /**
     * Deletes a transaction ensuring user ownership.
     */
    public boolean delete(int transactionId, int userId) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, transactionId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            DBConnection.close(null, stmt, conn);
        }
    }

    /**
     * Finds transaction by ID and User ID.
     */
    public Transaction getById(int transactionId, int userId) throws SQLException {
        String sql = "SELECT t.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color, p.provider_name AS account_provider " +
                     "FROM transactions t " +
                     "LEFT JOIN categories c ON t.category_id = c.id " +
                     "LEFT JOIN payment_accounts p ON t.account_id = p.id " +
                     "WHERE t.id = ? AND t.user_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, transactionId);
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
     * Gets recent transactions for a user.
     */
    public List<Transaction> getRecent(int userId, int limit) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT t.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color, p.provider_name AS account_provider " +
                     "FROM transactions t " +
                     "LEFT JOIN categories c ON t.category_id = c.id " +
                     "LEFT JOIN payment_accounts p ON t.account_id = p.id " +
                     "WHERE t.user_id = ? " +
                     "ORDER BY t.transaction_date DESC, t.transaction_time DESC, t.id DESC " +
                     "LIMIT ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, limit > 0 ? limit : 5);
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
     * Filtered and sorted search query for transactions table.
     */
    public List<Transaction> getFiltered(int userId, String search, String type, Integer categoryId,
                                         String paymentMethod, Date startDate, Date endDate,
                                         BigDecimal minAmount, BigDecimal maxAmount, String sortBy) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT t.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color, p.provider_name AS account_provider " +
                "FROM transactions t " +
                "LEFT JOIN categories c ON t.category_id = c.id " +
                "LEFT JOIN payment_accounts p ON t.account_id = p.id " +
                "WHERE t.user_id = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(t.merchant) LIKE ? OR LOWER(t.notes) LIKE ? OR LOWER(t.upi_id) LIKE ? OR LOWER(c.name) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (type != null && !type.trim().isEmpty() && !type.equalsIgnoreCase("ALL")) {
            sql.append("AND t.type = ? ");
            params.add(type.toUpperCase());
        }

        if (categoryId != null && categoryId > 0) {
            sql.append("AND t.category_id = ? ");
            params.add(categoryId);
        }

        if (paymentMethod != null && !paymentMethod.trim().isEmpty() && !paymentMethod.equalsIgnoreCase("ALL")) {
            sql.append("AND LOWER(t.payment_method) = ? ");
            params.add(paymentMethod.trim().toLowerCase());
        }

        if (startDate != null) {
            sql.append("AND t.transaction_date >= ? ");
            params.add(startDate);
        }

        if (endDate != null) {
            sql.append("AND t.transaction_date <= ? ");
            params.add(endDate);
        }

        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) > 0) {
            sql.append("AND t.amount >= ? ");
            params.add(minAmount);
        }

        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) > 0) {
            sql.append("AND t.amount <= ? ");
            params.add(maxAmount);
        }

        // Sorting
        if ("oldest".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY t.transaction_date ASC, t.transaction_time ASC, t.id ASC");
        } else if ("highest".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY t.amount DESC, t.transaction_date DESC");
        } else if ("lowest".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY t.amount ASC, t.transaction_date DESC");
        } else { // default "newest"
            sql.append("ORDER BY t.transaction_date DESC, t.transaction_time DESC, t.id DESC");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
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
     * Gets category spending totals for Chart.js Donut Chart.
     */
    public List<Map<String, Object>> getCategorySpendingBreakdown(int userId, Integer month, Integer year) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.id, c.name, c.color, c.icon, SUM(t.amount) AS total_amount, COUNT(t.id) AS tx_count " +
                "FROM transactions t " +
                "JOIN categories c ON t.category_id = c.id " +
                "WHERE t.user_id = ? AND t.type = 'EXPENSE' "
        );

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (month != null && month > 0) {
            sql.append("AND MONTH(t.transaction_date) = ? ");
            params.add(month);
        }
        if (year != null && year > 0) {
            sql.append("AND YEAR(t.transaction_date) = ? ");
            params.add(year);
        }

        sql.append("GROUP BY c.id, c.name, c.color, c.icon ORDER BY total_amount DESC");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("categoryId", rs.getInt("id"));
                row.put("categoryName", rs.getString("name"));
                row.put("color", rs.getString("color"));
                row.put("icon", rs.getString("icon"));
                row.put("totalAmount", rs.getBigDecimal("total_amount"));
                row.put("txCount", rs.getInt("tx_count"));
                result.add(row);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return result;
    }

    /**
     * Gets monthly Income vs Expense comparison totals for the given year (Chart.js Bar Chart).
     */
    public List<Map<String, Object>> getMonthlyCashFlow(int userId, int year) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT MONTH(transaction_date) AS month_num, " +
                     "SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) AS total_income, " +
                     "SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS total_expense " +
                     "FROM transactions " +
                     "WHERE user_id = ? AND YEAR(transaction_date) = ? " +
                     "GROUP BY MONTH(transaction_date) " +
                     "ORDER BY month_num ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, year);
            rs = stmt.executeQuery();

            Map<Integer, Map<String, Object>> monthMap = new HashMap<>();
            while (rs.next()) {
                int m = rs.getInt("month_num");
                Map<String, Object> data = new HashMap<>();
                data.put("month", m);
                data.put("income", rs.getBigDecimal("total_income"));
                data.put("expense", rs.getBigDecimal("total_expense"));
                monthMap.put(m, data);
            }

            // Fill all 12 months
            String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            for (int i = 1; i <= 12; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("monthNumber", i);
                item.put("monthName", monthNames[i - 1]);
                if (monthMap.containsKey(i)) {
                    item.put("income", monthMap.get(i).get("income"));
                    item.put("expense", monthMap.get(i).get("expense"));
                } else {
                    item.put("income", BigDecimal.ZERO);
                    item.put("expense", BigDecimal.ZERO);
                }
                result.add(item);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return result;
    }

    /**
     * Gets daily spending trend for the last N days (Chart.js Line Chart).
     */
    public List<Map<String, Object>> getSpendingTrend(int userId, int days) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT transaction_date, SUM(amount) AS daily_expense " +
                     "FROM transactions " +
                     "WHERE user_id = ? AND type = 'EXPENSE' AND transaction_date >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) " +
                     "GROUP BY transaction_date " +
                     "ORDER BY transaction_date ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, days > 0 ? days : 30);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("date", rs.getDate("transaction_date").toString());
                map.put("amount", rs.getBigDecimal("daily_expense"));
                result.add(map);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return result;
    }

    /**
     * Aggregates total balance, total income, total expense for a user.
     */
    public Map<String, BigDecimal> getFinancialTotals(int userId) throws SQLException {
        Map<String, BigDecimal> totals = new HashMap<>();
        String sql = "SELECT " +
                     "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS total_income, " +
                     "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS total_expense " +
                     "FROM transactions WHERE user_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal income = rs.getBigDecimal("total_income");
                BigDecimal expense = rs.getBigDecimal("total_expense");
                BigDecimal balance = income.subtract(expense);

                totals.put("totalIncome", income);
                totals.put("totalExpenses", expense);
                totals.put("totalBalance", balance);
            } else {
                totals.put("totalIncome", BigDecimal.ZERO);
                totals.put("totalExpenses", BigDecimal.ZERO);
                totals.put("totalBalance", BigDecimal.ZERO);
            }
        } finally {
            DBConnection.close(rs, stmt, conn);
        }
        return totals;
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setId(rs.getInt("id"));
        tx.setUserId(rs.getInt("user_id"));
        tx.setType(rs.getString("type"));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setCategoryId(rs.getInt("category_id"));
        tx.setCategoryName(rs.getString("category_name"));
        tx.setCategoryIcon(rs.getString("category_icon"));
        tx.setCategoryColor(rs.getString("category_color"));
        tx.setMerchant(rs.getString("merchant"));
        tx.setPaymentMethod(rs.getString("payment_method"));
        tx.setUpiId(rs.getString("upi_id"));
        int accId = rs.getInt("account_id");
        tx.setAccountId(rs.wasNull() ? null : accId);
        tx.setAccountProvider(rs.getString("account_provider"));
        tx.setTransactionDate(rs.getDate("transaction_date"));
        tx.setTransactionTime(rs.getTime("transaction_time"));
        tx.setNotes(rs.getString("notes"));
        tx.setRecurring(rs.getBoolean("is_recurring"));
        tx.setSyncedDemo(rs.getBoolean("is_synced_demo"));
        tx.setCreatedAt(rs.getTimestamp("created_at"));
        tx.setUpdatedAt(rs.getTimestamp("updated_at"));
        return tx;
    }
}
