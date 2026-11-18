package com.spendly.filter;

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
 * Filter ensuring UTF-8 request/response character encoding and standard security headers.
 */
public class EncodingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpServletResponse httpRes = (HttpServletResponse) response;
        httpRes.setHeader("X-Content-Type-Options", "nosniff");
        httpRes.setHeader("X-Frame-Options", "SAMEORIGIN");
        httpRes.setHeader("X-XSS-Protection", "1; mode=block");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
