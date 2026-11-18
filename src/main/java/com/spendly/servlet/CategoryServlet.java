package com.spendly.servlet;

import com.google.gson.JsonObject;
import com.spendly.dao.CategoryDAO;
import com.spendly.model.Category;
import com.spendly.model.User;
import com.spendly.service.AuthService;
import com.spendly.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Category Controller Servlet.
 * Path mapping: /api/categories/*
 */
public class CategoryServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CategoryServlet.class.getName());

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        try {
            List<Category> list = categoryDAO.getAllForUser(user.getId());
            JsonUtil.sendSuccess(resp, list);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching categories", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo != null && pathInfo.equals("/delete")) {
                handleDelete(req, resp, user);
                return;
            }

            JsonObject body = JsonUtil.readJsonObject(req);
            String name = body.has("name") ? body.get("name").getAsString() : null;
            String type = body.has("type") ? body.get("type").getAsString() : "EXPENSE";
            String icon = body.has("icon") ? body.get("icon").getAsString() : "tag";
            String color = body.has("color") ? body.get("color").getAsString() : "#6366f1";

            if (name == null || name.trim().isEmpty()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Category name is required.");
                return;
            }

            Category cat = categoryDAO.createCustomCategory(user.getId(), name, type, icon, color);
            if (cat != null) {
                JsonUtil.sendSuccess(resp, "Custom category created!", cat);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create category.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating category", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        try {
            handleDelete(req, resp, user);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting category", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        int catId;
        if (body.has("id")) {
            catId = body.get("id").getAsInt();
        } else if (req.getParameter("id") != null) {
            catId = Integer.parseInt(req.getParameter("id"));
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Category ID is required.");
            return;
        }

        boolean ok = categoryDAO.deleteCustomCategory(catId, user.getId());
        if (ok) {
            JsonUtil.sendSuccess(resp, "Category deleted successfully!", null);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cannot delete standard system categories or not found.");
        }
    }
}
