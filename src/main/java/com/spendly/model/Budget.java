package com.spendly.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

/**
 * Budget Entity for tracking spending against monthly or category limits.
 */
public class Budget implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private Integer categoryId; // null for overall budget
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private String name;
    private BigDecimal amount; // Budget limit
    private BigDecimal spentAmount; // Dynamically computed
    private BigDecimal remainingAmount; // Dynamically computed
    private double percentageUsed; // Dynamically computed
    private String period; // MONTHLY, YEARLY, CUSTOM
    private int monthVal;
    private int yearVal;
    private int alertThreshold; // Percentage, e.g., 80
    private boolean isExceeded;
    private boolean isNearLimit;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Budget() {
        this.amount = BigDecimal.ZERO;
        this.spentAmount = BigDecimal.ZERO;
        this.remainingAmount = BigDecimal.ZERO;
        this.percentageUsed = 0.0;
        this.period = "MONTHLY";
        this.alertThreshold = 80;
    }

    public void computeMetrics() {
        if (this.amount != null && this.amount.compareTo(BigDecimal.ZERO) > 0) {
            if (this.spentAmount == null) this.spentAmount = BigDecimal.ZERO;
            this.remainingAmount = this.amount.subtract(this.spentAmount);
            this.percentageUsed = this.spentAmount.divide(this.amount, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            this.isExceeded = this.percentageUsed >= 100.0;
            this.isNearLimit = this.percentageUsed >= this.alertThreshold && !this.isExceeded;
        } else {
            this.remainingAmount = BigDecimal.ZERO;
            this.percentageUsed = 0.0;
            this.isExceeded = false;
            this.isNearLimit = false;
        }
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public double getPercentageUsed() {
        return percentageUsed;
    }

    public void setPercentageUsed(double percentageUsed) {
        this.percentageUsed = percentageUsed;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public int getMonthVal() {
        return monthVal;
    }

    public void setMonthVal(int monthVal) {
        this.monthVal = monthVal;
    }

    public int getYearVal() {
        return yearVal;
    }

    public void setYearVal(int yearVal) {
        this.yearVal = yearVal;
    }

    public int getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(int alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public boolean isExceeded() {
        return isExceeded;
    }

    public void setExceeded(boolean exceeded) {
        isExceeded = exceeded;
    }

    public boolean isNearLimit() {
        return isNearLimit;
    }

    public void setNearLimit(boolean nearLimit) {
        isNearLimit = nearLimit;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
