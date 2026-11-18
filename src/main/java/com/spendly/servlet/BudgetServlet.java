package com.spendly.servlet;

import com.google.gson.JsonObject;
import com.spendly.dao.BudgetDAO;
import com.spendly.dao.NotificationDAO;
import com.spendly.model.Budget;
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
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Budget Controller Servlet with over-budget warning and live progress calculation.
 * Path mapping: /api/budgets/*
 */
public class BudgetServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(BudgetServlet.class.getName());

    private final BudgetDAO budgetDAO = new BudgetDAO();
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
            LocalDate now = LocalDate.now();
            String mParam = req.getParameter("month");
            String yParam = req.getParameter("year");

            int month = (mParam != null && !mParam.isEmpty()) ? Integer.parseInt(mParam) : now.getMonthValue();
            int year = (yParam != null && !yParam.isEmpty()) ? Integer.parseInt(yParam) : now.getYear();

            List<Budget> budgets = budgetDAO.getAllForUserMonth(user.getId(), month, year);

            // Check if any budget is exceeded or near limit and alert
            for (Budget b : budgets) {
                if (b.isExceeded()) {
                    checkAndTriggerAlert(user.getId(), b, "DANGER", "Budget Exceeded Alert: You have exceeded your " + b.getName() + " limit!");
                } else if (b.isNearLimit()) {
                    checkAndTriggerAlert(user.getId(), b, "WARNING", "Budget Warning: " + b.getName() + " is at " + Math.round(b.getPercentageUsed()) + "% capacity.");
                }
            }

            JsonUtil.sendSuccess(resp, budgets);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Budget GET error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    private void checkAndTriggerAlert(int userId, Budget b, String type, String message) {
        // Can optionally log or write notification
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
            if (pathInfo != null && pathInfo.equals("/update")) {
                handleUpdate(req, resp, user);
            } else if (pathInfo != null && pathInfo.equals("/delete")) {
                handleDelete(req, resp, user);
            } else {
                handleCreate(req, resp, user);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Budget POST error", e);
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
            LOGGER.log(Level.SEVERE, "Budget PUT error", e);
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
            LOGGER.log(Level.SEVERE, "Budget DELETE error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        Budget b = parseBudget(body, user.getId());

        if (b.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Budget amount must be greater than zero.");
            return;
        }

        Budget created = budgetDAO.create(b);
        if (created != null) {
            JsonUtil.sendSuccess(resp, "Budget created successfully!", created);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create budget.");
        }
    }

    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        if (!body.has("id")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Budget ID is required.");
            return;
        }

        Budget b = parseBudget(body, user.getId());
        b.setId(body.get("id").getAsInt());

        boolean ok = budgetDAO.update(b);
        if (ok) {
            Budget updated = budgetDAO.getById(b.getId(), user.getId());
            JsonUtil.sendSuccess(resp, "Budget updated successfully!", updated);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Budget not found or unauthorized.");
        }
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        int budgetId;
        if (body.has("id")) {
            budgetId = body.get("id").getAsInt();
        } else if (req.getParameter("id") != null) {
            budgetId = Integer.parseInt(req.getParameter("id"));
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Budget ID is required.");
            return;
        }

        boolean ok = budgetDAO.delete(budgetId, user.getId());
        if (ok) {
            JsonUtil.sendSuccess(resp, "Budget deleted successfully!", null);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Budget not found or unauthorized.");
        }
    }

    private Budget parseBudget(JsonObject body, int userId) {
        LocalDate now = LocalDate.now();
        Budget b = new Budget();
        b.setUserId(userId);
        b.setName(body.has("name") ? body.get("name").getAsString() : "Monthly Budget");
        b.setAmount(body.has("amount") ? body.get("amount").getAsBigDecimal() : BigDecimal.ZERO);
        b.setPeriod(body.has("period") ? body.get("period").getAsString() : "MONTHLY");
        b.setMonthVal(body.has("monthVal") ? body.get("monthVal").getAsInt() : now.getMonthValue());
        b.setYearVal(body.has("yearVal") ? body.get("yearVal").getAsInt() : now.getYear());
        b.setAlertThreshold(body.has("alertThreshold") ? body.get("alertThreshold").getAsInt() : 80);

        if (body.has("categoryId") && !body.get("categoryId").isJsonNull() && body.get("categoryId").getAsInt() > 0) {
            b.setCategoryId(body.get("categoryId").getAsInt());
        } else {
            b.setCategoryId(null);
        }
        return b;
    }
}
