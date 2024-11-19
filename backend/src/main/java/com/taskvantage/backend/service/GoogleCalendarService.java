package com.taskvantage.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.taskvantage.backend.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class GoogleCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String GOOGLE_OAUTH2_REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private final HttpTransport httpTransport;

    public GoogleCalendarService() {
        this.httpTransport = new NetHttpTransport();
    }

    // Build the Calendar service using the stored access token
    private Calendar getCalendarService(User user) throws GeneralSecurityException, IOException {
        GoogleCredential credential = new GoogleCredential().setAccessToken(user.getGoogleAccessToken());

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
        ).setApplicationName("TaskVantage").build();
    }

    // Create an event in Google Calendar for a task
    public void createCalendarEvent(User user, String taskTitle, ZonedDateTime start, ZonedDateTime end) throws GeneralSecurityException, IOException {
        Calendar calendarService = getCalendarService(user);

        Event event = new Event()
                .setSummary(taskTitle)
                .setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(convertToDate(start))))
                .setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(convertToDate(end))));

        calendarService.events().insert("primary", event).execute();
    }

    public void updateCalendarEvent(User user, String taskTitle, ZonedDateTime start, ZonedDateTime end, String eventId) throws GeneralSecurityException, IOException {
        Calendar calendarService = getCalendarService(user);

        Event event = new Event()
                .setSummary(taskTitle)
                .setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(convertToDate(start))))
                .setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(convertToDate(end))));

        calendarService.events().update("primary", eventId, event).execute();
    }

    // Test Google Calendar integration with access token from environment variable
    public void testGoogleCalendarIntegration() throws GeneralSecurityException, IOException {
        // Retrieve access token from environment variable
        String accessToken = System.getenv("TEST_GOOGLE_ACCESS_TOKEN");

        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalStateException("Environment variable TEST_GOOGLE_ACCESS_TOKEN is not set or is empty.");
        }

        // Simulate a user with an access token from the environment
        User testUser = new User();
        testUser.setGoogleAccessToken(accessToken);

        // Simulate a task creation
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusHours(1);

        createCalendarEvent(testUser, "Test Task from Backend", start, end);
    }

    // Helper method to convert ZonedDateTime to Date for Google Calendar
    private Date convertToDate(ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * Comprehensively revokes Google Calendar access and cleans up user data
     */
    @Transactional
    public void revokeAccess(User user) {
        if (user == null) {
            logger.warn("Attempted to revoke access for null user");
            return;
        }

        boolean accessTokenRevoked = false;
        boolean refreshTokenRevoked = false;

        // 1. Revoke access token if present
        if (user.getGoogleAccessToken() != null) {
            accessTokenRevoked = revokeToken(user.getGoogleAccessToken());
        }

        // 2. Revoke refresh token if present
        if (user.getGoogleRefreshToken() != null) {
            refreshTokenRevoked = revokeToken(user.getGoogleRefreshToken());
        }

        // 3. Clear all Google-related data from user entity
        clearUserGoogleData(user);

        // Log the results
        logger.info("Google Calendar access revocation completed for user ID: {}. Access token revoked: {}, Refresh token revoked: {}",
                user.getId(), accessTokenRevoked, refreshTokenRevoked);
    }

    /**
     * Revokes a single token using Google's revocation endpoint
     */
    private boolean revokeToken(String token) {
        try {
            // Create the request body
            Map<String, String> params = new HashMap<>();
            params.put("token", token);
            HttpContent content = new UrlEncodedContent(params);

            // Build and execute the revocation request
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            HttpRequest request = requestFactory.buildPostRequest(
                    new GenericUrl(GOOGLE_OAUTH2_REVOKE_URL),
                    content
            );

            // Add required headers
            request.getHeaders().setContentType("application/x-www-form-urlencoded");

            // Execute the request
            HttpResponse response = request.execute();

            boolean success = response.getStatusCode() == 200;
            if (!success) {
                logger.error("Token revocation failed with status code: {}", response.getStatusCode());
            }

            response.disconnect();
            return success;

        } catch (IOException e) {
            logger.error("Error revoking token: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates if a token is still valid using Google's tokeninfo endpoint
     */
    public boolean isTokenValid(String token) {
        try {
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            GenericUrl url = new GenericUrl(GOOGLE_TOKEN_INFO_URL + "?access_token=" + token);

            HttpRequest request = requestFactory.buildGetRequest(url);
            HttpResponse response = request.execute();

            boolean isValid = response.getStatusCode() == 200;
            response.disconnect();
            return isValid;

        } catch (IOException e) {
            logger.debug("Token validation failed, assuming invalid: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Clears all Google-related data from the user entity
     */
    private void clearUserGoogleData(User user) {
        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);
        user.setGoogleEmail(null);
        user.setTaskSyncEnabled(false);

        logger.debug("Cleared Google Calendar data for user ID: {}", user.getId());
    }
}