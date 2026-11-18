package com.spendly.servlet;

import com.google.gson.JsonObject;
import com.spendly.dao.RecurringPaymentDAO;
import com.spendly.model.RecurringPayment;
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
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Recurring Payments & Subscriptions Controller Servlet.
 * Path mapping: /api/recurring/*
 */
public class RecurringServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RecurringServlet.class.getName());

    private final RecurringPaymentDAO recurringDAO = new RecurringPaymentDAO();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo != null && pathInfo.equals("/upcoming")) {
                String daysParam = req.getParameter("days");
                int days = (daysParam != null && !daysParam.isEmpty()) ? Integer.parseInt(daysParam) : 15;
                List<RecurringPayment> list = recurringDAO.getUpcomingPayments(user.getId(), days);
                JsonUtil.sendSuccess(resp, list);
            } else {
                List<RecurringPayment> list = recurringDAO.getAllForUser(user.getId());
                JsonUtil.sendSuccess(resp, list);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Recurring GET error", e);
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
            if (pathInfo != null && pathInfo.equals("/update")) {
                handleUpdate(req, resp, user);
            } else if (pathInfo != null && pathInfo.equals("/delete")) {
                handleDelete(req, resp, user);
            } else {
                handleCreate(req, resp, user);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Recurring POST error", e);
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
            LOGGER.log(Level.SEVERE, "Recurring PUT error", e);
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
            LOGGER.log(Level.SEVERE, "Recurring DELETE error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        RecurringPayment rp = parseRecurring(body, user.getId());

        if (rp.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Subscription amount must be greater than zero.");
            return;
        }

        RecurringPayment created = recurringDAO.create(rp);
        if (created != null) {
            JsonUtil.sendSuccess(resp, "Recurring subscription added!", created);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create subscription.");
        }
    }

    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        if (!body.has("id")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Subscription ID is required.");
            return;
        }

        RecurringPayment rp = parseRecurring(body, user.getId());
        rp.setId(body.get("id").getAsInt());

        boolean ok = recurringDAO.update(rp);
        if (ok) {
            RecurringPayment updated = recurringDAO.getById(rp.getId(), user.getId());
            JsonUtil.sendSuccess(resp, "Subscription updated!", updated);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Subscription not found or unauthorized.");
        }
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        int recurringId;
        if (body.has("id")) {
            recurringId = body.get("id").getAsInt();
        } else if (req.getParameter("id") != null) {
            recurringId = Integer.parseInt(req.getParameter("id"));
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Subscription ID is required.");
            return;
        }

        boolean ok = recurringDAO.delete(recurringId, user.getId());
        if (ok) {
            JsonUtil.sendSuccess(resp, "Subscription removed!", null);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Subscription not found or unauthorized.");
        }
    }

    private RecurringPayment parseRecurring(JsonObject body, int userId) {
        RecurringPayment rp = new RecurringPayment();
        rp.setUserId(userId);
        rp.setTitle(body.has("title") ? body.get("title").getAsString() : "Subscription");
        rp.setMerchant(body.has("merchant") ? body.get("merchant").getAsString() : "Merchant");
        rp.setAmount(body.has("amount") ? body.get("amount").getAsBigDecimal() : BigDecimal.ZERO);
        rp.setCategoryId(body.has("categoryId") ? body.get("categoryId").getAsInt() : 11);
        rp.setBillingCycle(body.has("billingCycle") ? body.get("billingCycle").getAsString() : "MONTHLY");

        if (body.has("nextDueDate") && !body.get("nextDueDate").getAsString().isEmpty()) {
            rp.setNextDueDate(Date.valueOf(body.get("nextDueDate").getAsString()));
        } else {
            rp.setNextDueDate(Date.valueOf(LocalDate.now().plusDays(30)));
        }

        rp.setPaymentMethod(body.has("paymentMethod") ? body.get("paymentMethod").getAsString() : "UPI AutoPay");
        rp.setUpiId(body.has("upiId") && !body.get("upiId").isJsonNull() ? body.get("upiId").getAsString() : null);
        rp.setAutoPay(!body.has("autoPay") || body.get("autoPay").getAsBoolean());
        rp.setStatus(body.has("status") ? body.get("status").getAsString() : "ACTIVE");
        return rp;
    }
}
