package com.spendly.servlet;

import com.google.gson.JsonObject;
import com.spendly.dao.TransactionDAO;
import com.spendly.model.Transaction;
import com.spendly.model.User;
import com.spendly.service.AuthService;
import com.spendly.service.ExportService;
import com.spendly.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transaction Controller Servlet.
 * Path mapping: /api/transactions/*
 */
public class TransactionServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(TransactionServlet.class.getName());

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AuthService authService = new AuthService();
    private final ExportService exportService = new ExportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Filtered list
                String search = req.getParameter("search");
                String type = req.getParameter("type");
                String catParam = req.getParameter("categoryId");
                Integer categoryId = (catParam != null && !catParam.isEmpty()) ? Integer.parseInt(catParam) : null;
                String paymentMethod = req.getParameter("paymentMethod");

                String startParam = req.getParameter("startDate");
                Date startDate = (startParam != null && !startParam.isEmpty()) ? Date.valueOf(startParam) : null;

                String endParam = req.getParameter("endDate");
                Date endDate = (endParam != null && !endParam.isEmpty()) ? Date.valueOf(endParam) : null;

                String minParam = req.getParameter("minAmount");
                BigDecimal minAmount = (minParam != null && !minParam.isEmpty()) ? new BigDecimal(minParam) : null;

                String maxParam = req.getParameter("maxAmount");
                BigDecimal maxAmount = (maxParam != null && !maxParam.isEmpty()) ? new BigDecimal(maxParam) : null;

                String sortBy = req.getParameter("sortBy");

                List<Transaction> list = transactionDAO.getFiltered(
                        user.getId(), search, type, categoryId, paymentMethod, startDate, endDate, minAmount, maxAmount, sortBy
                );
                JsonUtil.sendSuccess(resp, list);
            } else if (pathInfo.equals("/recent")) {
                String limitParam = req.getParameter("limit");
                int limit = (limitParam != null && !limitParam.isEmpty()) ? Integer.parseInt(limitParam) : 5;
                List<Transaction> list = transactionDAO.getRecent(user.getId(), limit);
                JsonUtil.sendSuccess(resp, list);
            } else if (pathInfo.equals("/export/csv")) {
                resp.setContentType("text/csv;charset=UTF-8");
                resp.setHeader("Content-Disposition", "attachment; filename=\"spendly_transactions.csv\"");
                List<Transaction> list = transactionDAO.getFiltered(user.getId(), null, null, null, null, null, null, null, null, "newest");
                exportService.exportTransactionsToCsv(list, resp.getWriter());
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Transaction GET error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error fetching transactions: " + e.getMessage());
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
            if (pathInfo == null || pathInfo.equals("/")) {
                handleCreate(req, resp, user);
            } else if (pathInfo.equals("/update")) {
                handleUpdate(req, resp, user);
            } else if (pathInfo.equals("/delete")) {
                handleDelete(req, resp, user);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found: " + pathInfo);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Transaction POST error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing transaction: " + e.getMessage());
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
            LOGGER.log(Level.SEVERE, "Transaction PUT error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error updating transaction: " + e.getMessage());
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
            LOGGER.log(Level.SEVERE, "Transaction DELETE error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error deleting transaction: " + e.getMessage());
        }
    }

    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        Transaction tx = parseTransaction(body, user.getId());

        if (tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Amount must be greater than zero.");
            return;
        }
        if (tx.getMerchant() == null || tx.getMerchant().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Merchant / Payee name is required.");
            return;
        }

        Transaction created = transactionDAO.create(tx);
        if (created != null) {
            JsonUtil.sendSuccess(resp, "Transaction added successfully!", created);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create transaction.");
        }
    }

    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        if (!body.has("id")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Transaction ID is required for update.");
            return;
        }

        Transaction tx = parseTransaction(body, user.getId());
        tx.setId(body.get("id").getAsInt());

        boolean ok = transactionDAO.update(tx);
        if (ok) {
            Transaction updated = transactionDAO.getById(tx.getId(), user.getId());
            JsonUtil.sendSuccess(resp, "Transaction updated successfully!", updated);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Transaction not found or unauthorized.");
        }
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        int txId;
        if (body.has("id")) {
            txId = body.get("id").getAsInt();
        } else if (req.getParameter("id") != null) {
            txId = Integer.parseInt(req.getParameter("id"));
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Transaction ID is required.");
            return;
        }

        boolean ok = transactionDAO.delete(txId, user.getId());
        if (ok) {
            JsonUtil.sendSuccess(resp, "Transaction deleted successfully!", null);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Transaction not found or unauthorized.");
        }
    }

    private Transaction parseTransaction(JsonObject body, int userId) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setType(body.has("type") ? body.get("type").getAsString().toUpperCase() : "EXPENSE");
        tx.setAmount(body.has("amount") ? body.get("amount").getAsBigDecimal() : BigDecimal.ZERO);
        tx.setCategoryId(body.has("categoryId") ? body.get("categoryId").getAsInt() : 1);
        tx.setMerchant(body.has("merchant") ? body.get("merchant").getAsString() : "General Expense");
        tx.setPaymentMethod(body.has("paymentMethod") ? body.get("paymentMethod").getAsString() : "UPI");
        tx.setUpiId(body.has("upiId") && !body.get("upiId").isJsonNull() ? body.get("upiId").getAsString() : null);

        if (body.has("accountId") && !body.get("accountId").isJsonNull()) {
            tx.setAccountId(body.get("accountId").getAsInt());
        }

        if (body.has("transactionDate") && !body.get("transactionDate").getAsString().isEmpty()) {
            tx.setTransactionDate(Date.valueOf(body.get("transactionDate").getAsString()));
        } else {
            tx.setTransactionDate(new Date(System.currentTimeMillis()));
        }

        if (body.has("transactionTime") && !body.get("transactionTime").getAsString().isEmpty()) {
            String timeStr = body.get("transactionTime").getAsString();
            if (timeStr.length() == 5) timeStr += ":00";
            tx.setTransactionTime(Time.valueOf(timeStr));
        } else {
            tx.setTransactionTime(new Time(System.currentTimeMillis()));
        }

        tx.setNotes(body.has("notes") && !body.get("notes").isJsonNull() ? body.get("notes").getAsString() : null);
        tx.setRecurring(body.has("isRecurring") && body.get("isRecurring").getAsBoolean());
        return tx;
    }
}
