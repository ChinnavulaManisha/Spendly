package com.spendly.servlet;

import com.google.gson.JsonObject;
import com.spendly.dao.GoalDAO;
import com.spendly.dao.NotificationDAO;
import com.spendly.model.Goal;
import com.spendly.model.Notification;
import com.spendly.model.User;
import com.spendly.service.AuthService;
import com.spendly.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Savings Goals Controller Servlet.
 * Path mapping: /api/goals/*
 */
public class GoalServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GoalServlet.class.getName());

    private final GoalDAO goalDAO = new GoalDAO();
    private final AuthService authService = new AuthService();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        try {
            List<Goal> goals = goalDAO.getAllForUser(user.getId());
            JsonUtil.sendSuccess(resp, goals);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Goal GET error", e);
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
            if (pathInfo != null && pathInfo.equals("/add-funds")) {
                handleAddFunds(req, resp, user);
            } else if (pathInfo != null && pathInfo.equals("/update")) {
                handleUpdate(req, resp, user);
            } else if (pathInfo != null && pathInfo.equals("/delete")) {
                handleDelete(req, resp, user);
            } else {
                handleCreate(req, resp, user);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Goal POST error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        try {
            handleUpdate(req, resp, user);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Goal PUT error", e);
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
            LOGGER.log(Level.SEVERE, "Goal DELETE error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        Goal g = parseGoal(body, user.getId());

        if (g.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Target amount must be greater than zero.");
            return;
        }
        if (g.getTitle() == null || g.getTitle().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Goal title is required.");
            return;
        }

        Goal created = goalDAO.create(g);
        if (created != null) {
            JsonUtil.sendSuccess(resp, "Savings goal created successfully!", created);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create goal.");
        }
    }

    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        if (!body.has("id")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Goal ID is required.");
            return;
        }

        Goal g = parseGoal(body, user.getId());
        g.setId(body.get("id").getAsInt());

        boolean ok = goalDAO.update(g);
        if (ok) {
            Goal updated = goalDAO.getById(g.getId(), user.getId());
            JsonUtil.sendSuccess(resp, "Goal updated successfully!", updated);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Goal not found or unauthorized.");
        }
    }

    private void handleAddFunds(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        int goalId = body.has("id") ? body.get("id").getAsInt() : 0;
        BigDecimal depositAmount = body.has("amount") ? body.get("amount").getAsBigDecimal() : BigDecimal.ZERO;

        if (goalId <= 0 || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid Goal ID and deposit amount are required.");
            return;
        }

        boolean ok = goalDAO.addFunds(goalId, user.getId(), depositAmount);
        if (ok) {
            Goal updated = goalDAO.getById(goalId, user.getId());
            if ("COMPLETED".equalsIgnoreCase(updated.getStatus())) {
                notificationDAO.create(new Notification(
                        user.getId(),
                        "🎉 Goal Reached!",
                        "Congratulations! You reached your savings goal for '" + updated.getTitle() + "'!",
                        "SUCCESS"
                ));
            }
            JsonUtil.sendSuccess(resp, "Added $" + depositAmount + " to '" + updated.getTitle() + "'!", updated);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Goal not found or unauthorized.");
        }
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        int goalId;
        if (body.has("id")) {
            goalId = body.get("id").getAsInt();
        } else if (req.getParameter("id") != null) {
            goalId = Integer.parseInt(req.getParameter("id"));
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Goal ID is required.");
            return;
        }

        boolean ok = goalDAO.delete(goalId, user.getId());
        if (ok) {
            JsonUtil.sendSuccess(resp, "Goal deleted successfully!", null);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Goal not found or unauthorized.");
        }
    }

    private Goal parseGoal(JsonObject body, int userId) {
        Goal g = new Goal();
        g.setUserId(userId);
        g.setTitle(body.has("title") ? body.get("title").getAsString() : "New Savings Goal");
        g.setTargetAmount(body.has("targetAmount") ? body.get("targetAmount").getAsBigDecimal() : BigDecimal.ZERO);
        g.setSavedAmount(body.has("savedAmount") ? body.get("savedAmount").getAsBigDecimal() : BigDecimal.ZERO);

        if (body.has("targetDate") && !body.get("targetDate").getAsString().isEmpty()) {
            g.setTargetDate(Date.valueOf(body.get("targetDate").getAsString()));
        } else {
            g.setTargetDate(Date.valueOf(java.time.LocalDate.now().plusMonths(6)));
        }

        g.setCategoryName(body.has("categoryName") ? body.get("categoryName").getAsString() : "General");
        g.setIcon(body.has("icon") ? body.get("icon").getAsString() : "piggy-bank");
        g.setColor(body.has("color") ? body.get("color").getAsString() : "#10b981");
        g.setStatus(body.has("status") ? body.get("status").getAsString() : "ACTIVE");
        g.setNotes(body.has("notes") && !body.get("notes").isJsonNull() ? body.get("notes").getAsString() : null);
        return g;
    }
}
