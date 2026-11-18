package com.spendly.servlet;

import com.spendly.dao.TransactionDAO;
import com.spendly.model.DashboardSummary;
import com.spendly.model.SpendingInsight;
import com.spendly.model.User;
import com.spendly.service.AnalyticsService;
import com.spendly.service.AuthService;
import com.spendly.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Analytics and Chart.js Data Controller Servlet.
 * Path mapping: /api/analytics/*
 */
public class AnalyticsServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AnalyticsServlet.class.getName());

    private final AnalyticsService analyticsService = new AnalyticsService();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "/summary";

        try {
            LocalDate now = LocalDate.now();
            String mParam = req.getParameter("month");
            String yParam = req.getParameter("year");

            int month = (mParam != null && !mParam.isEmpty()) ? Integer.parseInt(mParam) : now.getMonthValue();
            int year = (yParam != null && !yParam.isEmpty()) ? Integer.parseInt(yParam) : now.getYear();

            switch (pathInfo) {
                case "/summary": {
                    DashboardSummary summary = analyticsService.getDashboardSummary(user.getId());
                    JsonUtil.sendSuccess(resp, summary);
                    break;
                }
                case "/charts/category": {
                    List<Map<String, Object>> catData = transactionDAO.getCategorySpendingBreakdown(user.getId(), month, year);
                    JsonUtil.sendSuccess(resp, catData);
                    break;
                }
                case "/charts/cashflow": {
                    List<Map<String, Object>> cashflow = transactionDAO.getMonthlyCashFlow(user.getId(), year);
                    JsonUtil.sendSuccess(resp, cashflow);
                    break;
                }
                case "/charts/trend": {
                    String daysParam = req.getParameter("days");
                    int days = (daysParam != null && !daysParam.isEmpty()) ? Integer.parseInt(daysParam) : 30;
                    List<Map<String, Object>> trend = transactionDAO.getSpendingTrend(user.getId(), days);
                    JsonUtil.sendSuccess(resp, trend);
                    break;
                }
                case "/insights": {
                    SpendingInsight insights = analyticsService.calculateSpendingInsights(user.getId(), month, year);
                    JsonUtil.sendSuccess(resp, insights);
                    break;
                }
                default:
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found: " + pathInfo);
                    break;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Analytics error on " + pathInfo, e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Analytics calculation error: " + e.getMessage());
        }
    }
}
