package com.spendly.service;

import com.spendly.dao.NotificationDAO;
import com.spendly.dao.UserDAO;
import com.spendly.model.Notification;
import com.spendly.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;

/**
 * Service for user authentication, session security, and account management.
 */
public class AuthService {

    public static final String SESSION_USER_KEY = "SPENDLY_LOGGED_IN_USER";

    private final UserDAO userDAO;
    private final NotificationDAO notificationDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.notificationDAO = new NotificationDAO();
    }

    public User register(String fullName, String email, String plainPassword, String currency) throws SQLException, IllegalArgumentException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Valid email address is required.");
        }
        if (plainPassword == null || plainPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (userDAO.emailExists(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = userDAO.register(fullName, email, plainPassword, currency);
        if (user != null) {
            Notification welcomeNotif = new Notification(
                    user.getId(),
                    "Welcome to Spendly!",
                    "Your account has been created successfully. Start tracking expenses, budgets, and savings goals!",
                    "SUCCESS"
            );
            notificationDAO.create(welcomeNotif);
        }
        return user;
    }

    public User login(String email, String plainPassword) throws SQLException {
        if (email == null || plainPassword == null) {
            return null;
        }
        return userDAO.authenticate(email, plainPassword);
    }

    public boolean resetPassword(String email, String newPassword) throws SQLException, IllegalArgumentException {
        if (email == null || !userDAO.emailExists(email)) {
            throw new IllegalArgumentException("No account found with this email address.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }
        return userDAO.resetPassword(email, newPassword);
    }

    public void establishSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_KEY, user);
        session.setMaxInactiveInterval(60 * 60 * 24); // 24 hours session
    }

    public User getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object obj = session.getAttribute(SESSION_USER_KEY);
            if (obj instanceof User) {
                return (User) obj;
            }
        }
        return null;
    }

    public void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_USER_KEY);
            session.invalidate();
        }
    }

    public boolean updateTheme(int userId, String theme) throws SQLException {
        return userDAO.updateThemePreference(userId, theme);
    }

    public boolean updateProfile(int userId, String fullName, String currency) throws SQLException {
        return userDAO.updateProfile(userId, fullName, currency);
    }
}
