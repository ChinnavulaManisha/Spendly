package com.spendly.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Notification Entity for budget warnings, payment alerts, and updates.
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String title;
    private String message;
    private String type; // INFO, WARNING, SUCCESS, DANGER
    private boolean isRead;
    private String actionUrl;
    private Timestamp createdAt;

    public Notification() {
        this.type = "INFO";
        this.isRead = false;
    }

    public Notification(int userId, String title, String message, String type) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type != null ? type : "INFO";
        this.isRead = false;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
