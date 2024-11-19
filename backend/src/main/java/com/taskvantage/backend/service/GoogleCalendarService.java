package com.taskvantage.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.taskvantage.backend.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    // Build the Tasks service using the stored access token
    private Tasks getTasksService(User user) throws GeneralSecurityException, IOException {
        GoogleCredential credential = new GoogleCredential().setAccessToken(user.getGoogleAccessToken());

        return new Tasks.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
        ).setApplicationName("TaskVantage").build();
    }

    // Create a task in Google Tasks
    public void createGoogleTask(User user, String taskTitle, ZonedDateTime start, ZonedDateTime end, String description)
            throws GeneralSecurityException, IOException {
        if (user.getGoogleAccessToken() == null) {
            logger.error("No Google access token found for user {}", user.getId());
            return;
        }

        Tasks tasksService = getTasksService(user);

        try {
            // Get the default task list
            TaskLists taskLists = tasksService.tasklists().list().execute();
            if (taskLists.getItems() != null && !taskLists.getItems().isEmpty()) {
                String taskListId = taskLists.getItems().get(0).getId();

                com.google.api.services.tasks.model.Task googleTask = new com.google.api.services.tasks.model.Task()
                        .setTitle(taskTitle)
                        .setDue(formatDateTime(end))
                        .setNotes(description);

                tasksService.tasks().insert(taskListId, googleTask).execute();
                logger.info("Successfully created Google Task '{}' for user {}", taskTitle, user.getId());
            } else {
                // Create a new task list if none exists
                TaskList newTaskList = new TaskList().setTitle("TaskVantage Tasks");
                TaskList createdList = tasksService.tasklists().insert(newTaskList).execute();

                com.google.api.services.tasks.model.Task googleTask = new com.google.api.services.tasks.model.Task()
                        .setTitle(taskTitle)
                        .setDue(formatDateTime(end))
                        .setNotes(description);

                tasksService.tasks().insert(createdList.getId(), googleTask).execute();
                logger.info("Created new task list and task '{}' for user {}", taskTitle, user.getId());
            }
        } catch (GoogleJsonResponseException e) {
            logger.error("Google Tasks API error: {}, Error code: {}", e.getDetails().getMessage(), e.getStatusCode());
            throw e;
        }
    }

    // Update a task in Google Tasks
    public void updateGoogleTask(User user, String taskTitle, ZonedDateTime start, ZonedDateTime end, String taskId, String description)
            throws GeneralSecurityException, IOException {
        Tasks tasksService = getTasksService(user);

        // Update the task
        com.google.api.services.tasks.model.Task googleTask = new com.google.api.services.tasks.model.Task()
                .setTitle(taskTitle)
                .setDue(formatDateTime(end))  // Google Tasks only supports due date, not start date
                .setNotes(description);       // Add description as notes

        // Get the default task list
        TaskLists taskLists = tasksService.tasklists().list().execute();
        if (taskLists.getItems() != null && !taskLists.getItems().isEmpty()) {
            String taskListId = taskLists.getItems().get(0).getId();
            tasksService.tasks().update(taskListId, taskId, googleTask).execute();
            logger.info("Updated Google Task '{}' for user {}", taskTitle, user.getId());
        } else {
            logger.warn("No task lists found for user {}", user.getId());
        }
    }

    // Helper method to format DateTime for Google Tasks
    private String formatDateTime(ZonedDateTime dateTime) {
        // Google Tasks API expects RFC 3339 timestamp
        return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    // Test Google Tasks integration
    public void testGoogleCalendarIntegration() throws GeneralSecurityException, IOException {
        String accessToken = System.getenv("TEST_GOOGLE_ACCESS_TOKEN");

        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalStateException("Environment variable TEST_GOOGLE_ACCESS_TOKEN is not set or is empty.");
        }

        User testUser = new User();
        testUser.setGoogleAccessToken(accessToken);

        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusHours(1);

        createGoogleTask(testUser, "Test Task from Backend", start, end, "Test task description");
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