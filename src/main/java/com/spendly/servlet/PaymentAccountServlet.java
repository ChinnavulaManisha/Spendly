package com.spendly.servlet;

import com.google.gson.JsonObject;
import com.spendly.dao.PaymentAccountDAO;
import com.spendly.model.PaymentAccount;
import com.spendly.model.Transaction;
import com.spendly.model.User;
import com.spendly.service.AuthService;
import com.spendly.service.payment.DemoPaymentProvider;
import com.spendly.service.payment.PaymentProvider;
import com.spendly.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UPI & Payment Accounts Controller Servlet.
 * Path mapping: /api/accounts/*
 */
public class PaymentAccountServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(PaymentAccountServlet.class.getName());

    private final PaymentAccountDAO paymentAccountDAO = new PaymentAccountDAO();
    private final AuthService authService = new AuthService();
    private final PaymentProvider demoPaymentProvider = new DemoPaymentProvider();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        try {
            List<PaymentAccount> list = paymentAccountDAO.getAllForUser(user.getId());
            JsonUtil.sendSuccess(resp, list);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Account GET error", e);
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
            if (pathInfo != null && pathInfo.equals("/sync-demo")) {
                handleSyncDemo(req, resp, user);
            } else if (pathInfo != null && pathInfo.equals("/delete")) {
                handleDelete(req, resp, user);
            } else {
                handleCreate(req, resp, user);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Account POST error", e);
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
            LOGGER.log(Level.SEVERE, "Account DELETE error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        String accountType = body.has("accountType") ? body.get("accountType").getAsString() : "UPI";
        String providerName = body.has("providerName") ? body.get("providerName").getAsString() : "UPI Payment";
        String identifierMask = body.has("identifierMask") ? body.get("identifierMask").getAsString() : null;
        String accountHolder = body.has("accountHolderName") ? body.get("accountHolderName").getAsString() : user.getFullName();
        BigDecimal balance = body.has("balance") ? body.get("balance").getAsBigDecimal() : BigDecimal.ZERO;
        boolean isDefault = body.has("isDefault") && body.get("isDefault").getAsBoolean();

        if (identifierMask == null || identifierMask.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "UPI ID or Account identifier is required.");
            return;
        }

        PaymentAccount acc = new PaymentAccount();
        acc.setUserId(user.getId());
        acc.setAccountType(accountType);
        acc.setProviderName(providerName);
        acc.setIdentifierMask(identifierMask.trim());
        acc.setAccountHolderName(accountHolder);
        acc.setBalance(balance);
        acc.setDefault(isDefault);
        acc.setVerified(true);

        PaymentAccount created = paymentAccountDAO.create(acc);
        if (created != null) {
            JsonUtil.sendSuccess(resp, "Payment account / UPI ID connected successfully!", created);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to connect payment account.");
        }
    }

    private void handleSyncDemo(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        int accountId = body.has("accountId") ? body.get("accountId").getAsInt() : 0;
        String identifier = body.has("identifierMask") ? body.get("identifierMask").getAsString() : "alex@okhdfcbank";

        List<Transaction> syncedList = demoPaymentProvider.syncTransactions(user.getId(), accountId, identifier);

        Map<String, Object> data = new HashMap<>();
        data.put("syncedCount", syncedList.size());
        data.put("transactions", syncedList);
        data.put("provider", demoPaymentProvider.getProviderName());

        JsonUtil.sendSuccess(resp, "Successfully synchronized " + syncedList.size() + " statement transactions!", data);
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        int accountId;
        if (body.has("id")) {
            accountId = body.get("id").getAsInt();
        } else if (req.getParameter("id") != null) {
            accountId = Integer.parseInt(req.getParameter("id"));
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Account ID is required.");
            return;
        }

        boolean ok = paymentAccountDAO.delete(accountId, user.getId());
        if (ok) {
            JsonUtil.sendSuccess(resp, "Account removed successfully!", null);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Account not found or unauthorized.");
        }
    }
}
