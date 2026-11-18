package com.spendly.servlet;

import com.google.gson.JsonObject;
import com.spendly.model.User;
import com.spendly.service.AuthService;
import com.spendly.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentication and User Session Controller Servlet.
 * Path mapping: /api/auth/*
 */
public class AuthServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AuthServlet.class.getName());
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/session-check")) {
            handleSessionCheck(req, resp);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        try {
            switch (pathInfo) {
                case "/login":
                    handleLogin(req, resp);
                    break;
                case "/register":
                    handleRegister(req, resp);
                    break;
                case "/logout":
                    handleLogout(req, resp);
                    break;
                case "/forgot-password":
                    handleForgotPassword(req, resp);
                    break;
                case "/theme":
                    handleThemeUpdate(req, resp);
                    break;
                case "/profile":
                    handleProfileUpdate(req, resp);
                    break;
                default:
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found: " + pathInfo);
                    break;
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Auth error on " + pathInfo, e);
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        String email = body.has("email") ? body.get("email").getAsString() : null;
        String password = body.has("password") ? body.get("password").getAsString() : null;

        if (email == null || password == null || email.trim().isEmpty() || password.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and password are required.");
            return;
        }

        User user = authService.login(email, password);
        if (user != null) {
            authService.establishSession(req, user);
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("fullName", user.getFullName());
            data.put("email", user.getEmail());
            data.put("currency", user.getCurrency());
            data.put("themePreference", user.getThemePreference());
            JsonUtil.sendSuccess(resp, "Login successful! Welcome back, " + user.getFullName(), data);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid email or password.");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        String fullName = body.has("fullName") ? body.get("fullName").getAsString() : null;
        String email = body.has("email") ? body.get("email").getAsString() : null;
        String password = body.has("password") ? body.get("password").getAsString() : null;
        String currency = body.has("currency") ? body.get("currency").getAsString() : "USD";

        User user = authService.register(fullName, email, password, currency);
        if (user != null) {
            authService.establishSession(req, user);
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("fullName", user.getFullName());
            data.put("email", user.getEmail());
            data.put("currency", user.getCurrency());
            data.put("themePreference", user.getThemePreference());
            JsonUtil.sendSuccess(resp, "Account created successfully! Welcome to Spendly.", data);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create account.");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        authService.invalidateSession(req);
        JsonUtil.sendSuccess(resp, "Logged out successfully.", null);
    }

    private void handleSessionCheck(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = authService.getSessionUser(req);
        if (user != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("authenticated", true);
            data.put("id", user.getId());
            data.put("fullName", user.getFullName());
            data.put("email", user.getEmail());
            data.put("currency", user.getCurrency());
            data.put("themePreference", user.getThemePreference());
            JsonUtil.sendSuccess(resp, "Session active", data);
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("authenticated", false);
            JsonUtil.sendSuccess(resp, "No active session", data);
        }
    }

    private void handleForgotPassword(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = JsonUtil.readJsonObject(req);
        String email = body.has("email") ? body.get("email").getAsString() : null;
        String newPassword = body.has("newPassword") ? body.get("newPassword").getAsString() : null;

        if (email == null || newPassword == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and new password are required.");
            return;
        }

        boolean ok = authService.resetPassword(email, newPassword);
        if (ok) {
            JsonUtil.sendSuccess(resp, "Password reset successfully! You can now log in with your new password.", null);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to reset password.");
        }
    }

    private void handleThemeUpdate(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        JsonObject body = JsonUtil.readJsonObject(req);
        String theme = body.has("theme") ? body.get("theme").getAsString() : "dark";
        user.setThemePreference(theme);
        authService.updateTheme(user.getId(), theme);
        JsonUtil.sendSuccess(resp, "Theme updated", theme);
    }

    private void handleProfileUpdate(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = authService.getSessionUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        JsonObject body = JsonUtil.readJsonObject(req);
        String fullName = body.has("fullName") ? body.get("fullName").getAsString() : user.getFullName();
        String currency = body.has("currency") ? body.get("currency").getAsString() : user.getCurrency();

        authService.updateProfile(user.getId(), fullName, currency);
        user.setFullName(fullName);
        user.setCurrency(currency);
        JsonUtil.sendSuccess(resp, "Profile updated successfully.", user);
    }
}
