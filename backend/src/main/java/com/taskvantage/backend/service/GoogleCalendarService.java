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

    private Calendar getCalendarService(User user) throws GeneralSecurityException, IOException {
        GoogleCredential credential = new GoogleCredential().setAccessToken(user.getGoogleAccessToken());

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
        ).setApplicationName("TaskVantage").build();
    }

    public String createCalendarEvent(User user, String taskTitle, ZonedDateTime start, ZonedDateTime end, boolean isAllDay)
            throws GeneralSecurityException, IOException {
        Calendar calendarService = getCalendarService(user);

        Event event = new Event()
                .setSummary(taskTitle);

        if (isAllDay) {
            event.setStart(new EventDateTime().setDate(new com.google.api.client.util.DateTime(true, convertToDate(start).getTime(), 0)))
                    .setEnd(new EventDateTime().setDate(new com.google.api.client.util.DateTime(true, convertToDate(end).getTime(), 0)));
        } else {
            event.setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(convertToDate(start))))
                    .setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(convertToDate(end))));
        }

        Event createdEvent = calendarService.events().insert("primary", event).execute();
        logger.info("Created Google Calendar event with ID: {}", createdEvent.getId());
        return createdEvent.getId();
    }

    public void updateCalendarEvent(User user, String eventId, String taskTitle, ZonedDateTime start,
                                    ZonedDateTime end, boolean isAllDay) throws GeneralSecurityException, IOException {
        Calendar calendarService = getCalendarService(user);

        try {
            // First, get the existing event
            Event event = calendarService.events().get("primary", eventId).execute();

            // Update the event details
            event.setSummary(taskTitle);

            if (isAllDay) {
                event.setStart(new EventDateTime()
                                .setDate(new com.google.api.client.util.DateTime(true, convertToDate(start).getTime(), 0)))
                        .setEnd(new EventDateTime()
                                .setDate(new com.google.api.client.util.DateTime(true, convertToDate(end).getTime(), 0)));
            } else {
                event.setStart(new EventDateTime()
                                .setDateTime(new com.google.api.client.util.DateTime(convertToDate(start))))
                        .setEnd(new EventDateTime()
                                .setDateTime(new com.google.api.client.util.DateTime(convertToDate(end))));
            }

            calendarService.events().update("primary", eventId, event).execute();
            logger.info("Updated Google Calendar event: {}", eventId);
        } catch (IOException e) {
            logger.error("Failed to update Google Calendar event: {}", eventId, e);
            throw e;
        }
    }

    public void deleteCalendarEvent(User user, String eventId) throws GeneralSecurityException, IOException {
        Calendar calendarService = getCalendarService(user);

        try {
            calendarService.events().delete("primary", eventId).execute();
            logger.info("Deleted Google Calendar event: {}", eventId);
        } catch (IOException e) {
            logger.error("Failed to delete Google Calendar event: {}", eventId, e);
            throw e;
        }
    }

    public void testGoogleCalendarIntegration() throws GeneralSecurityException, IOException {
        String accessToken = System.getenv("TEST_GOOGLE_ACCESS_TOKEN");

        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalStateException("Environment variable TEST_GOOGLE_ACCESS_TOKEN is not set or is empty.");
        }

        User testUser = new User();
        testUser.setGoogleAccessToken(accessToken);

        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusHours(1);

        // Test regular event
        String eventId = createCalendarEvent(testUser, "Test Task from Backend", start, end, false);

        // Test update
        updateCalendarEvent(testUser, eventId, "Updated Test Task", start, end, false);

        // Test delete
        deleteCalendarEvent(testUser, eventId);

        // Test all-day event
        ZonedDateTime startOfDay = start.toLocalDate().atStartOfDay(start.getZone());
        ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        createCalendarEvent(testUser, "Test All-Day Task from Backend", startOfDay, endOfDay, true);
    }

    private Date convertToDate(ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }

    @Transactional
    public void revokeAccess(User user) {
        if (user == null) {
            logger.warn("Attempted to revoke access for null user");
            return;
        }

        boolean accessTokenRevoked = false;
        boolean refreshTokenRevoked = false;

        if (user.getGoogleAccessToken() != null) {
            accessTokenRevoked = revokeToken(user.getGoogleAccessToken());
        }

        if (user.getGoogleRefreshToken() != null) {
            refreshTokenRevoked = revokeToken(user.getGoogleRefreshToken());
        }

        clearUserGoogleData(user);

        logger.info("Google Calendar access revocation completed for user ID: {}. Access token revoked: {}, Refresh token revoked: {}",
                user.getId(), accessTokenRevoked, refreshTokenRevoked);
    }

    private boolean revokeToken(String token) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("token", token);
            HttpContent content = new UrlEncodedContent(params);

            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            HttpRequest request = requestFactory.buildPostRequest(
                    new GenericUrl(GOOGLE_OAUTH2_REVOKE_URL),
                    content
            );

            request.getHeaders().setContentType("application/x-www-form-urlencoded");

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

    private void clearUserGoogleData(User user) {
        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);
        user.setGoogleEmail(null);
        user.setTaskSyncEnabled(false);

        logger.debug("Cleared Google Calendar data for user ID: {}", user.getId());
    }
}