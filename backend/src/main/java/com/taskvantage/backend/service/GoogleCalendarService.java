package com.taskvantage.backend.service;

import com.google.api.client.http.GenericUrl;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.taskvantage.backend.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class GoogleCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);

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
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime end = start.plusHours(1);

        createCalendarEvent(testUser, "Test Task from Backend", start, end);
    }

    // Helper method to convert ZonedDateTime to Date for Google Calendar
    private Date convertToDate(ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }

    public void revokeAccess(User user) throws GeneralSecurityException, IOException {
        if (user.getGoogleAccessToken() != null) {
            Calendar calendarService = getCalendarService(user);
            try {
                // Revoke access using Google's revoke endpoint
                calendarService.getRequestFactory()
                        .buildGetRequest(new GenericUrl("https://oauth2.googleapis.com/revoke?token=" + user.getGoogleAccessToken()))
                        .execute();
            } catch (IOException e) {
                // Log but don't throw - we still want to clear the tokens even if revoke fails
                logger.warn("Failed to revoke Google Calendar access token", e);
            }
        }
    }
}