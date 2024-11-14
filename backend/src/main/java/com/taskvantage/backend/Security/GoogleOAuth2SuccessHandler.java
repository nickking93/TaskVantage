package com.taskvantage.backend.Security;

import com.taskvantage.backend.model.User;
import com.taskvantage.backend.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.util.Optional;
import java.util.Arrays;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${frontend.url}")
    private String frontendUrl;

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuth2SuccessHandler.class);
    private static final String USER_ID_COOKIE_NAME = "google_auth_user_id";

    public GoogleOAuth2SuccessHandler(OAuth2AuthorizedClientService authorizedClientService,
                                      CustomUserDetailsService customUserDetailsService) {
        this.authorizedClientService = authorizedClientService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        logger.debug("Entering onAuthenticationSuccess handler");

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            logger.error("Authentication is not OAuth2AuthenticationToken");
            redirectToError(response, "invalid_auth_type");
            return;
        }

        try {
            String userId = extractUserIdFromCookie(request)
                    .orElseThrow(() -> new IllegalStateException("No userId found in cookies"));

            User user = customUserDetailsService.findUserById(Long.parseLong(userId));
            if (user == null) {
                logger.error("User not found with ID: {}", userId);
                redirectToError(response, "user_not_found");
                return;
            }

            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );

            if (client == null) {
                logger.error("OAuth2AuthorizedClient is null for user: {}", userId);
                redirectToError(response, "oauth2_client_not_found");
                return;
            }

            // Update user with Google information
            updateUserWithGoogleInfo(user, client, oauthToken);
            customUserDetailsService.saveUser(user);

            // Clear the userId cookie
            clearUserIdCookie(response);

            // Redirect to settings page
            String redirectUrl = String.format("%s/home/%s/settings?auth=success", frontendUrl, userId);
            logger.debug("Redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            logger.error("Error in OAuth2 success handler", e);
            redirectToError(response, "internal_error");
        }
    }

    private Optional<String> extractUserIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> USER_ID_COOKIE_NAME.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
        }
        return Optional.empty();
    }

    private void updateUserWithGoogleInfo(User user, OAuth2AuthorizedClient client,
                                          OAuth2AuthenticationToken oauthToken) {
        OAuth2AccessToken accessToken = client.getAccessToken();
        OAuth2RefreshToken refreshToken = client.getRefreshToken();

        String email = oauthToken.getPrincipal().getAttribute("email");
        user.setGoogleEmail(email);
        user.setGoogleAccessToken(accessToken.getTokenValue());

        if (refreshToken != null) {
            user.setGoogleRefreshToken(refreshToken.getTokenValue());
        }
    }

    private void clearUserIdCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(USER_ID_COOKIE_NAME, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void redirectToError(HttpServletResponse response, String error) throws IOException {
        response.sendRedirect(frontendUrl + "/settings?auth=error&message=" + error);
    }
}