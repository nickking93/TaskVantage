package com.taskvantage.backend.service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.taskvantage.backend.model.User;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class GoogleCalendarService {

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
}