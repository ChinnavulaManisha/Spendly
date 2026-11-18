package com.spendly.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Category Entity for grouping expenses and incomes.
 */
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private Integer userId; // null for default/system categories
    private String name;
    private String type; // EXPENSE or INCOME
    private String icon;
    private String color;
    private boolean isCustom;
    private Timestamp createdAt;

    public Category() {
        this.type = "EXPENSE";
        this.icon = "tag";
        this.color = "#6366f1";
        this.isCustom = false;
    }

    public Category(int id, Integer userId, String name, String type, String icon, String color, boolean isCustom) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type != null ? type : "EXPENSE";
        this.icon = icon != null ? icon : "tag";
        this.color = color != null ? color : "#6366f1";
        this.isCustom = isCustom;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
