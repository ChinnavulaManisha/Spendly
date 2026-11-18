package com.spendly.service;

import com.spendly.config.DBConnection;
import com.spendly.dao.BudgetDAO;
import com.spendly.dao.GoalDAO;
import com.spendly.dao.NotificationDAO;
import com.spendly.dao.RecurringPaymentDAO;
import com.spendly.dao.TransactionDAO;
import com.spendly.model.Budget;
import com.spendly.model.DashboardSummary;
import com.spendly.model.Goal;
import com.spendly.model.SpendingInsight;
import com.spendly.model.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for calculating smart fintech analytics, KPIs, trends, and dashboard insights.
 */
public class AnalyticsService {

    private final TransactionDAO transactionDAO;
    private final BudgetDAO budgetDAO;
    private final GoalDAO goalDAO;
    private final RecurringPaymentDAO recurringPaymentDAO;
    private final NotificationDAO notificationDAO;

    public AnalyticsService() {
        this.transactionDAO = new TransactionDAO();
        this.budgetDAO = new BudgetDAO();
        this.goalDAO = new GoalDAO();
        this.recurringPaymentDAO = new RecurringPaymentDAO();
        this.notificationDAO = new NotificationDAO();
    }

    /**
     * Builds comprehensive dashboard summary data.
     */
    public DashboardSummary getDashboardSummary(int userId) throws SQLException {
        DashboardSummary summary = new DashboardSummary();
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        // 1. Overall Balance & Totals
        Map<String, BigDecimal> totals = transactionDAO.getFinancialTotals(userId);
        summary.setTotalIncome(totals.getOrDefault("totalIncome", BigDecimal.ZERO));
        summary.setTotalExpenses(totals.getOrDefault("totalExpenses", BigDecimal.ZERO));
        summary.setTotalBalance(totals.getOrDefault("totalBalance", BigDecimal.ZERO));

        BigDecimal netSavings = summary.getTotalIncome().subtract(summary.getTotalExpenses());
        summary.setNetSavings(netSavings);

        if (summary.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            double rate = netSavings.divide(summary.getTotalIncome(), 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            summary.setSavingsRate(Math.max(0.0, rate));
        }

        // 2. Monthly Budget & Remaining
        List<Budget> budgets = budgetDAO.getAllForUserMonth(userId, month, year);
        BigDecimal totalBudgetAmount = BigDecimal.ZERO;
        BigDecimal totalBudgetSpent = BigDecimal.ZERO;

        for (Budget b : budgets) {
            if (b.getCategoryId() == null) {
                // Overall budget
                totalBudgetAmount = b.getAmount();
                totalBudgetSpent = b.getSpentAmount();
                break;
            } else {
                totalBudgetAmount = totalBudgetAmount.add(b.getAmount());
                totalBudgetSpent = totalBudgetSpent.add(b.getSpentAmount());
            }
        }

        summary.setMonthlyBudget(totalBudgetAmount);
        summary.setRemainingBudget(totalBudgetAmount.subtract(totalBudgetSpent));
        if (totalBudgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            summary.setBudgetUsagePercentage(
                    totalBudgetSpent.divide(totalBudgetAmount, 4, RoundingMode.HALF_UP).doubleValue() * 100.0
            );
        }

        // 3. Counts & Active Goals
        List<Goal> goals = goalDAO.getAllForUser(userId);
        int activeGoals = (int) goals.stream().filter(g -> "ACTIVE".equalsIgnoreCase(g.getStatus())).count();
        summary.setActiveGoalsCount(activeGoals);

        summary.setUnreadNotificationsCount(notificationDAO.getUnreadCount(userId));

        // 4. Recent Transactions
        summary.setRecentTransactions(transactionDAO.getRecent(userId, 6));

        // 5. Smart Insights
        summary.setInsights(calculateSpendingInsights(userId, month, year));

        return summary;
    }

    /**
     * Calculates smart automated financial insights.
     */
    public SpendingInsight calculateSpendingInsights(int userId, int month, int year) throws SQLException {
        SpendingInsight insight = new SpendingInsight();
        LocalDate now = LocalDate.now();

        // 1. Highest Spending Category
        List<Map<String, Object>> catBreakdown = transactionDAO.getCategorySpendingBreakdown(userId, month, year);
        BigDecimal totalMonthExpense = BigDecimal.ZERO;
        for (Map<String, Object> c : catBreakdown) {
            totalMonthExpense = totalMonthExpense.add((BigDecimal) c.get("totalAmount"));
        }

        if (!catBreakdown.isEmpty()) {
            Map<String, Object> highest = catBreakdown.get(0);
            insight.setHighestCategoryName((String) highest.get("categoryName"));
            BigDecimal highestAmt = (BigDecimal) highest.get("totalAmount");
            insight.setHighestCategoryAmount(highestAmt);
            if (totalMonthExpense.compareTo(BigDecimal.ZERO) > 0) {
                double pct = highestAmt.divide(totalMonthExpense, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
                insight.setHighestCategoryPercent(Math.round(pct * 10.0) / 10.0);
            }
        } else {
            insight.setHighestCategoryName("None");
            insight.setHighestCategoryAmount(BigDecimal.ZERO);
            insight.setHighestCategoryPercent(0.0);
        }

        // 2. Largest Single Expense Transaction
        String largestTxSql = "SELECT merchant, amount, transaction_date FROM transactions " +
                              "WHERE user_id = ? AND type = 'EXPENSE' AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ? " +
                              "ORDER BY amount DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(largestTxSql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    insight.setLargestMerchant(rs.getString("merchant"));
                    insight.setLargestExpenseAmount(rs.getBigDecimal("amount"));
                    insight.setLargestExpenseDate(rs.getDate("transaction_date").toString());
                } else {
                    insight.setLargestMerchant("No expenses yet");
                    insight.setLargestExpenseAmount(BigDecimal.ZERO);
                }
            }
        }

        // 3. Average Daily Spending
        int dayOfMonth = Math.max(1, now.getDayOfMonth());
        if (totalMonthExpense.compareTo(BigDecimal.ZERO) > 0) {
            insight.setAverageDailySpending(totalMonthExpense.divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP));
        }

        // 4. Recurring Subscriptions
        BigDecimal recurringMonthly = recurringPaymentDAO.getTotalMonthlyRecurring(userId);
        insight.setTotalRecurringMonthly(recurringMonthly);
        insight.setActiveSubscriptionsCount(recurringPaymentDAO.getAllForUser(userId).size());

        // 5. Month-over-Month Comparison
        BigDecimal currentMonthExp = getMonthExpenseTotal(userId, month, year);
        int prevMonth = (month == 1) ? 12 : month - 1;
        int prevYear = (month == 1) ? year - 1 : year;
        BigDecimal prevMonthExp = getMonthExpenseTotal(userId, prevMonth, prevYear);

        if (prevMonthExp.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = currentMonthExp.subtract(prevMonthExp);
            double changePct = diff.divide(prevMonthExp, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            insight.setMonthlyComparisonPercent(Math.round(changePct * 10.0) / 10.0);

            if (changePct > 5.0) {
                insight.setSpendingTrend("INCREASED");
                insight.setTrendDescription(String.format("Spending is +%.1f%% higher than last month", Math.abs(changePct)));
            } else if (changePct < -5.0) {
                insight.setSpendingTrend("DECREASED");
                insight.setTrendDescription(String.format("Great job! Spending is -%.1f%% lower than last month", Math.abs(changePct)));
            } else {
                insight.setSpendingTrend("STABLE");
                insight.setTrendDescription("Spending is consistent with last month");
            }
        } else {
            insight.setSpendingTrend("STABLE");
            insight.setMonthlyComparisonPercent(0.0);
            insight.setTrendDescription("Baseline spending tracked for this period");
        }

        // 6. Top Merchants
        insight.setTopMerchants(getTopMerchants(userId, month, year));

        // 7. Smart AI Recommendations
        List<String> recommendations = new ArrayList<>();
        if (insight.getHighestCategoryPercent() > 40.0) {
            recommendations.add("Consider reducing " + insight.getHighestCategoryName() + " costs, which currently take " + insight.getHighestCategoryPercent() + "% of your budget.");
        }
        if (recurringMonthly.compareTo(BigDecimal.valueOf(100)) > 0) {
            recommendations.add("You have $" + recurringMonthly + " in active recurring subscriptions. Review unneeded SaaS services.");
        }
        if (insight.getAverageDailySpending().compareTo(BigDecimal.valueOf(50)) > 0) {
            recommendations.add("Your daily burn rate is $" + insight.getAverageDailySpending() + "/day. Setting daily limits can save up to 15%.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Your financial health looks steady! Keep maintaining consistent savings goals.");
        }
        insight.setSmartRecommendations(recommendations);

        return insight;
    }

    private BigDecimal getMonthExpenseTotal(int userId, int month, int year) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                     "WHERE user_id = ? AND type = 'EXPENSE' AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private List<Map<String, Object>> getTopMerchants(int userId, int month, int year) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT merchant, SUM(amount) AS total_spent, COUNT(id) AS tx_count " +
                     "FROM transactions " +
                     "WHERE user_id = ? AND type = 'EXPENSE' AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ? " +
                     "GROUP BY merchant ORDER BY total_spent DESC LIMIT 5";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("merchant", rs.getString("merchant"));
                    m.put("totalSpent", rs.getBigDecimal("total_spent"));
                    m.put("txCount", rs.getInt("tx_count"));
                    list.add(m);
                }
            }
        }
        return list;
    }
}
