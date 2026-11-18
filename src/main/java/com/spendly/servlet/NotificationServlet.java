package com.spendly.servlet;

import com.google.gson.JsonObject;
import com.spendly.dao.NotificationDAO;
import com.spendly.model.Notification;
import com.spendly.model.User;
import com.spendly.service.AuthService;
import com.spendly.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-App Notification Controller Servlet.
 * Path mapping: /api/notifications/*
 */
public class NotificationServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(NotificationServlet.class.getName());

    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        try {
            List<Notification> list = notificationDAO.getAllForUser(user.getId());
            int unread = notificationDAO.getUnreadCount(user.getId());

            Map<String, Object> data = new HashMap<>();
            data.put("notifications", list);
            data.put("unreadCount", unread);

            JsonUtil.sendSuccess(resp, data);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Notification GET error", e);
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
            if (pathInfo != null && pathInfo.equals("/mark-all-read")) {
                notificationDAO.markAllAsRead(user.getId());
                JsonUtil.sendSuccess(resp, "All notifications marked as read.", null);
            } else {
                JsonObject body = JsonUtil.readJsonObject(req);
                int notifId = body.has("id") ? body.get("id").getAsInt() : 0;
                if (notifId > 0) {
                    notificationDAO.markAsRead(notifId, user.getId());
                    JsonUtil.sendSuccess(resp, "Notification marked as read.", null);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Notification ID is required.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Notification POST error", e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }
}
