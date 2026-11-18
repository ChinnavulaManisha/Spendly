package com.spendly.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * User Entity representing an authenticated Spendly user.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String fullName;
    private String email;
    private String passwordHash;
    private String salt;
    private String currency;
    private String themePreference;
    private String avatarUrl;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public User() {
        this.currency = "USD";
        this.themePreference = "dark";
    }

    public User(int id, String fullName, String email, String currency, String themePreference) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.currency = currency != null ? currency : "USD";
        this.themePreference = themePreference != null ? themePreference : "dark";
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public void setThemePreference(String themePreference) {
        this.themePreference = themePreference;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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
