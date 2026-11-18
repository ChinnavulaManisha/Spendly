package com.spendly.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates AI-style smart financial calculations and analytics insights.
 */
public class SpendingInsight implements Serializable {
    private static final long serialVersionUID = 1L;

    private String highestCategoryName;
    private BigDecimal highestCategoryAmount;
    private double highestCategoryPercent;
    
    private String largestMerchant;
    private BigDecimal largestExpenseAmount;
    private String largestExpenseDate;

    private BigDecimal averageDailySpending;
    private double savingsRatePercent;
    private double budgetUsagePercent;
    private BigDecimal totalRecurringMonthly;
    private int activeSubscriptionsCount;

    private String spendingTrend; // "DECREASED", "INCREASED", "STABLE"
    private double monthlyComparisonPercent; // e.g. -8.5% compared to last month
    private String trendDescription;

    private List<Map<String, Object>> topMerchants; // merchant, amount, txCount
    private List<String> smartRecommendations;

    public SpendingInsight() {
        this.highestCategoryAmount = BigDecimal.ZERO;
        this.largestExpenseAmount = BigDecimal.ZERO;
        this.averageDailySpending = BigDecimal.ZERO;
        this.totalRecurringMonthly = BigDecimal.ZERO;
        this.savingsRatePercent = 0.0;
        this.budgetUsagePercent = 0.0;
        this.monthlyComparisonPercent = 0.0;
        this.spendingTrend = "STABLE";
    }

    // Getters and Setters
    public String getHighestCategoryName() {
        return highestCategoryName;
    }

    public void setHighestCategoryName(String highestCategoryName) {
        this.highestCategoryName = highestCategoryName;
    }

    public BigDecimal getHighestCategoryAmount() {
        return highestCategoryAmount;
    }

    public void setHighestCategoryAmount(BigDecimal highestCategoryAmount) {
        this.highestCategoryAmount = highestCategoryAmount;
    }

    public double getHighestCategoryPercent() {
        return highestCategoryPercent;
    }

    public void setHighestCategoryPercent(double highestCategoryPercent) {
        this.highestCategoryPercent = highestCategoryPercent;
    }

    public String getLargestMerchant() {
        return largestMerchant;
    }

    public void setLargestMerchant(String largestMerchant) {
        this.largestMerchant = largestMerchant;
    }

    public BigDecimal getLargestExpenseAmount() {
        return largestExpenseAmount;
    }

    public void setLargestExpenseAmount(BigDecimal largestExpenseAmount) {
        this.largestExpenseAmount = largestExpenseAmount;
    }

    public String getLargestExpenseDate() {
        return largestExpenseDate;
    }

    public void setLargestExpenseDate(String largestExpenseDate) {
        this.largestExpenseDate = largestExpenseDate;
    }

    public BigDecimal getAverageDailySpending() {
        return averageDailySpending;
    }

    public void setAverageDailySpending(BigDecimal averageDailySpending) {
        this.averageDailySpending = averageDailySpending;
    }

    public double getSavingsRatePercent() {
        return savingsRatePercent;
    }

    public void setSavingsRatePercent(double savingsRatePercent) {
        this.savingsRatePercent = savingsRatePercent;
    }

    public double getBudgetUsagePercent() {
        return budgetUsagePercent;
    }

    public void setBudgetUsagePercent(double budgetUsagePercent) {
        this.budgetUsagePercent = budgetUsagePercent;
    }

    public BigDecimal getTotalRecurringMonthly() {
        return totalRecurringMonthly;
    }

    public void setTotalRecurringMonthly(BigDecimal totalRecurringMonthly) {
        this.totalRecurringMonthly = totalRecurringMonthly;
    }

    public int getActiveSubscriptionsCount() {
        return activeSubscriptionsCount;
    }

    public void setActiveSubscriptionsCount(int activeSubscriptionsCount) {
        this.activeSubscriptionsCount = activeSubscriptionsCount;
    }

    public String getSpendingTrend() {
        return spendingTrend;
    }

    public void setSpendingTrend(String spendingTrend) {
        this.spendingTrend = spendingTrend;
    }

    public double getMonthlyComparisonPercent() {
        return monthlyComparisonPercent;
    }

    public void setMonthlyComparisonPercent(double monthlyComparisonPercent) {
        this.monthlyComparisonPercent = monthlyComparisonPercent;
    }

    public String getTrendDescription() {
        return trendDescription;
    }

    public void setTrendDescription(String trendDescription) {
        this.trendDescription = trendDescription;
    }

    public List<Map<String, Object>> getTopMerchants() {
        return topMerchants;
    }

    public void setTopMerchants(List<Map<String, Object>> topMerchants) {
        this.topMerchants = topMerchants;
    }

    public List<String> getSmartRecommendations() {
        return smartRecommendations;
    }

    public void setSmartRecommendations(List<String> smartRecommendations) {
        this.smartRecommendations = smartRecommendations;
    }
}
