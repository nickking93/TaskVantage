package com.taskvantage.backend.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class OAuth2CookieFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2CookieFilter.class);
    private static final String USER_ID_PARAM = "userId";
    private static final String USER_ID_COOKIE_NAME = "google_auth_user_id";
    private static final String OAUTH2_AUTH_REQUEST_URI = "/oauth2/authorization/google";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            if (isOAuth2AuthRequest(request)) {
                logger.debug("Processing OAuth2 authentication request");

                // Try to get userId from request parameter
                String userId = request.getParameter(USER_ID_PARAM);

                if (StringUtils.hasText(userId)) {
                    logger.debug("Found userId in request parameters: {}", userId);

                    // Create a cookie that will persist through the OAuth2 redirect
                    Cookie userIdCookie = new Cookie(USER_ID_COOKIE_NAME, userId);
                    userIdCookie.setPath("/");
                    userIdCookie.setHttpOnly(true);
                    userIdCookie.setMaxAge(3600); // 1 hour expiry

                    // Set SameSite attribute for better security
                    userIdCookie.setAttribute("SameSite", "Lax");

                    response.addCookie(userIdCookie);
                    logger.debug("Added userId cookie to response");
                } else {
                    logger.debug("No userId found in request parameters");
                    // Check if we already have the cookie
                    Optional<Cookie> existingCookie = getExistingUserIdCookie(request);
                    if (existingCookie.isEmpty()) {
                        logger.warn("No userId found in either parameters or cookies");
                    }
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Error in OAuth2CookieFilter", e);
            throw e;
        }
    }

    private boolean isOAuth2AuthRequest(HttpServletRequest request) {
        return request.getRequestURI().contains(OAUTH2_AUTH_REQUEST_URI);
    }

    private Optional<Cookie> getExistingUserIdCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> USER_ID_COOKIE_NAME.equals(cookie.getName()))
                    .findFirst();
        }
        return Optional.empty();
    }
}