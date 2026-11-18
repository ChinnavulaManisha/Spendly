package com.spendly.filter;

import com.spendly.model.User;
import com.spendly.service.AuthService;
import com.spendly.util.JsonUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter ensuring session authentication for protected API endpoints and dashboard views.
 */
public class AuthFilter implements Filter {

    private final AuthService authService = new AuthService();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Whitelisted public endpoints
        if (path.startsWith("/api/auth/login") ||
            path.startsWith("/api/auth/register") ||
            path.startsWith("/api/auth/forgot-password") ||
            path.startsWith("/api/auth/session-check") ||
            path.equals("/") ||
            path.equals("/index.html") ||
            path.startsWith("/css/") ||
            path.startsWith("/js/") ||
            path.startsWith("/images/") ||
            path.startsWith("/favicon.ico")) {
            chain.doFilter(request, response);
            return;
        }

        User user = authService.getSessionUser(req);
        if (user == null) {
            if (path.startsWith("/api/")) {
                JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Session expired or unauthorized. Please log in.");
            } else {
                res.sendRedirect(req.getContextPath() + "/index.html");
            }
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
