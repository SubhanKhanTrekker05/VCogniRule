package com.ooad.mhs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Protects the admin pages: runs BEFORE every /admin/** request (except login and logout)
 * and sends anyone who is not logged in back to the login page.
 *
 * This replaces the same "if not logged in, redirect" line that was repeated in 13
 * AdminController methods (DRY). It implements Spring's HandlerInterceptor interface;
 * WebConfig decides which URLs it covers.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    /** Session attribute that holds the logged-in admin's username. */
    public static final String SESSION_ATTRIBUTE = "admin";

    /** Login page that unauthenticated requests are sent to. */
    public static final String LOGIN_PATH = "/admin";

    /** True when the session belongs to a logged-in admin. */
    public static boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute(SESSION_ATTRIBUTE) != null;
    }

    /**
     * @return true to let the request continue to the controller; false after redirecting to login
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (isLoggedIn(request.getSession(false))) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + LOGIN_PATH);
        return false;
    }
}
