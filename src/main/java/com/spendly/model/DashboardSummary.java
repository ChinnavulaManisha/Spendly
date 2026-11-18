package com.spendly.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Encapsulates the main overview metrics and dashboard summary state.
 */
public class DashboardSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal totalBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal monthlyBudget;
    private BigDecimal remainingBudget;
    private BigDecimal netSavings;
    private double budgetUsagePercentage;
    private double savingsRate;
    
    private int transactionCount;
    private int activeGoalsCount;
    private int unreadNotificationsCount;

    private List<Transaction> recentTransactions;
    private SpendingInsight insights;

    public DashboardSummary() {
        this.totalBalance = BigDecimal.ZERO;
        this.totalIncome = BigDecimal.ZERO;
        this.totalExpenses = BigDecimal.ZERO;
        this.monthlyBudget = BigDecimal.ZERO;
        this.remainingBudget = BigDecimal.ZERO;
        this.netSavings = BigDecimal.ZERO;
        this.budgetUsagePercentage = 0.0;
        this.savingsRate = 0.0;
    }

    // Getters and Setters
    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public BigDecimal getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(BigDecimal remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public BigDecimal getNetSavings() {
        return netSavings;
    }

    public void setNetSavings(BigDecimal netSavings) {
        this.netSavings = netSavings;
    }

    public double getBudgetUsagePercentage() {
        return budgetUsagePercentage;
    }

    public void setBudgetUsagePercentage(double budgetUsagePercentage) {
        this.budgetUsagePercentage = budgetUsagePercentage;
    }

    public double getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(double savingsRate) {
        this.savingsRate = savingsRate;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public int getActiveGoalsCount() {
        return activeGoalsCount;
    }

    public void setActiveGoalsCount(int activeGoalsCount) {
        this.activeGoalsCount = activeGoalsCount;
    }

    public int getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }

    public void setUnreadNotificationsCount(int unreadNotificationsCount) {
        this.unreadNotificationsCount = unreadNotificationsCount;
    }

    public List<Transaction> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<Transaction> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }

    public SpendingInsight getInsights() {
        return insights;
    }

    public void setInsights(SpendingInsight insights) {
        this.insights = insights;
    }
}
