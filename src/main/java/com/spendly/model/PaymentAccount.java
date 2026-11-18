package com.spendly.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * PaymentAccount Entity representing connected UPI IDs, bank accounts, or cards.
 */
public class PaymentAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String accountType; // UPI, BANK_ACCOUNT, CREDIT_CARD, DEBIT_CARD, WALLET, CASH
    private String providerName; // Google Pay, PhonePe, Paytm, HDFC, Chase, etc.
    private String identifierMask; // alex@okaxis, •••• 4242, AC ••••9102
    private String accountHolderName;
    private BigDecimal balance;
    private boolean isDefault;
    private boolean isVerified;
    private Timestamp lastSyncedAt;
    private Timestamp createdAt;

    public PaymentAccount() {
        this.balance = BigDecimal.ZERO;
        this.accountType = "UPI";
        this.isDefault = false;
        this.isVerified = true;
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

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getIdentifierMask() {
        return identifierMask;
    }

    public void setIdentifierMask(String identifierMask) {
        this.identifierMask = identifierMask;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public Timestamp getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Timestamp lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
