package com.spendly.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * RecurringPayment Entity for recurring subscriptions, bills, and utilities.
 */
public class RecurringPayment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String title;
    private String merchant;
    private BigDecimal amount;
    private int categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private String billingCycle; // MONTHLY, WEEKLY, QUARTERLY, YEARLY
    private Date nextDueDate;
    private String paymentMethod;
    private String upiId;
    private boolean autoPay;
    private String status; // ACTIVE, PAUSED, CANCELLED
    private long daysUntilDue; // Computed
    private boolean isDueSoon; // Computed (<= 5 days)
    private Timestamp createdAt;

    public RecurringPayment() {
        this.amount = BigDecimal.ZERO;
        this.billingCycle = "MONTHLY";
        this.autoPay = true;
        this.status = "ACTIVE";
        this.paymentMethod = "UPI AutoPay";
    }

    public void computeMetrics() {
        if (this.nextDueDate != null) {
            LocalDate today = LocalDate.now();
            LocalDate due = this.nextDueDate.toLocalDate();
            this.daysUntilDue = ChronoUnit.DAYS.between(today, due);
            this.isDueSoon = this.daysUntilDue >= 0 && this.daysUntilDue <= 5;
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

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
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

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public Date getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(Date nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public boolean isAutoPay() {
        return autoPay;
    }

    public void setAutoPay(boolean autoPay) {
        this.autoPay = autoPay;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDaysUntilDue() {
        return daysUntilDue;
    }

    public void setDaysUntilDue(long daysUntilDue) {
        this.daysUntilDue = daysUntilDue;
    }

    public boolean isDueSoon() {
        return isDueSoon;
    }

    public void setDueSoon(boolean dueSoon) {
        isDueSoon = dueSoon;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
