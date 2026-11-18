package com.spendly.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Goal Entity representing savings goals with target dates and progress.
 */
public class Goal implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String title;
    private BigDecimal targetAmount;
    private BigDecimal savedAmount;
    private Date targetDate;
    private String categoryName;
    private String icon;
    private String color;
    private String status; // ACTIVE, COMPLETED, PAUSED
    private String notes;
    private double progressPercentage; // Computed
    private long daysRemaining; // Computed
    private BigDecimal remainingAmount; // Computed
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Goal() {
        this.targetAmount = BigDecimal.ZERO;
        this.savedAmount = BigDecimal.ZERO;
        this.status = "ACTIVE";
        this.icon = "piggy-bank";
        this.color = "#10b981";
    }

    public void computeMetrics() {
        if (this.targetAmount != null && this.targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (this.savedAmount == null) this.savedAmount = BigDecimal.ZERO;
            this.remainingAmount = this.targetAmount.subtract(this.savedAmount);
            if (this.remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
                this.remainingAmount = BigDecimal.ZERO;
            }
            this.progressPercentage = Math.min(100.0, this.savedAmount.divide(this.targetAmount, 4, RoundingMode.HALF_UP).doubleValue() * 100.0);
            if (this.progressPercentage >= 100.0) {
                this.status = "COMPLETED";
            }
        } else {
            this.progressPercentage = 0.0;
            this.remainingAmount = BigDecimal.ZERO;
        }

        if (this.targetDate != null) {
            LocalDate today = LocalDate.now();
            LocalDate target = this.targetDate.toLocalDate();
            this.daysRemaining = ChronoUnit.DAYS.between(today, target);
            if (this.daysRemaining < 0) this.daysRemaining = 0;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getSavedAmount() {
        return savedAmount;
    }

    public void setSavedAmount(BigDecimal savedAmount) {
        this.savedAmount = savedAmount;
    }

    public Date getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
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
