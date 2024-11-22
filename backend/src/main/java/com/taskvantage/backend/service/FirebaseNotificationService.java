package com.taskvantage.backend.service;

import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseNotificationService.class);
    private static final ConcurrentHashMap<String, Long> sentMessages = new ConcurrentHashMap<>();
    private static final long MESSAGE_EXPIRY_MINUTES = 30;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    private final CustomUserDetailsService userService;

    @Autowired
    public FirebaseNotificationService(CustomUserDetailsService userService) {
        this.userService = userService;
    }

    public boolean sendNotification(String token, String title, String body, String username) {
        if (token == null || token.trim().isEmpty()) {
            logger.error("FCM token is null or empty, cannot send notification.");
            return false;
        }

        // Generate a unique messageId for this notification
        String messageId = generateMessageId();

        // Check for duplicate messages
        if (isDuplicateMessage(messageId)) {
            logger.info("Duplicate message detected, skipping notification send.");
            return true; // Consider this a "success" since we're intentionally skipping
        }

        // Update the token cache
        updateTokenCache(token, true);
        String oldFcmToken = userService.findUserByUsername(username).getToken();
        updateTokenCache(oldFcmToken, false);

        Message message = createMessage(token, title, body, messageId);
        return sendMessageWithRetry(message, messageId, token);
    }

    public void updateTokenCache(String token, boolean isActive) {
        if (isActive) {
            sentMessages.put(token, System.currentTimeMillis());
        } else {
            sentMessages.remove(token);
        }
    }

    private String generateMessageId() {
        return UUID.randomUUID().toString();
    }

    private Message createMessage(String token, String title, String body, String messageId) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("messageId", messageId)
                .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                .build();
    }

    private boolean sendMessageWithRetry(Message message, String messageId, String token) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                logger.info("Successfully sent message: {} for key: {}", response, messageId);
                sentMessages.put(messageId, System.currentTimeMillis());
                return true;
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT ||
                        e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    logger.warn("Invalid FCM token detected: {}", token);
                    handleInvalidToken(token);
                    return false;
                }
                logger.warn("Attempt {} failed to send FCM notification: {} (Error code: {})",
                        attempt + 1, e.getMessage(), e.getMessagingErrorCode());
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        logger.error("Failed to send FCM notification after {} attempts.", MAX_RETRIES);
        return false;
    }

    private void handleInvalidToken(String token) {
        userService.findUserByFCMToken(token).ifPresent(user -> {
            logger.info("Clearing invalid FCM token for user: {}", user.getUsername());
            userService.clearUserToken(user.getUsername());
        });
    }

    private boolean isDuplicateMessage(String messageId) {
        Long lastSentTime = sentMessages.get(messageId);
        if (lastSentTime != null) {
            long timeSinceLastMessage = System.currentTimeMillis() - lastSentTime;
            if (TimeUnit.MILLISECONDS.toMinutes(timeSinceLastMessage) < MESSAGE_EXPIRY_MINUTES) {
                return true;
            }
        }
        return false;
    }
}