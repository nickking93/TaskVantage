package com.taskvantage.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseNotificationService.class);
    private static final ConcurrentHashMap<String, Long> sentMessages = new ConcurrentHashMap<>();
    private static final long MESSAGE_EXPIRY_MINUTES = 30;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    public void sendNotification(String token, String title, String body) {
        if (token == null || token.trim().isEmpty()) {
            logger.error("FCM token is null or empty, cannot send notification.");
            return;
        }

        // Create a unique message identifier
        String messageKey = createMessageKey(token, title, body);

        // Check for duplicate messages
        if (isDuplicateMessage(messageKey)) {
            logger.info("Duplicate message detected, skipping notification send.");
            return;
        }

        Message message = createMessage(token, title, body);
        sendMessageWithRetry(message, messageKey);
    }

    private Message createMessage(String token, String title, String body) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                // Add additional data to help with deduplication on client side
                .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                .putData("messageId", String.valueOf(System.nanoTime()))
                .build();
    }

    private void sendMessageWithRetry(Message message, String messageKey) {
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                logger.info("Successfully sent message: {} for key: {}", response, messageKey);

                // Record successful send
                sentMessages.put(messageKey, System.currentTimeMillis());
                return;

            } catch (Exception e) {
                lastException = e;
                logger.warn("Attempt {} failed to send FCM notification: {}", attempt + 1, e.getMessage());

                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (attempt + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Retry delay interrupted", ie);
                        break;
                    }
                }
            }
        }

        // If we get here, all retries failed
        if (lastException != null) {
            logger.error("Final attempt to send FCM notification failed after {} retries", MAX_RETRIES, lastException);
        }
    }

    private String createMessageKey(String token, String title, String body) {
        return String.format("%s:%s:%s:%d",
                token.substring(Math.max(0, token.length() - 10)), // Last 10 chars of token
                title,
                body,
                System.currentTimeMillis() / (1000 * 60 * 15) // 15-minute bucket
        );
    }

    private boolean isDuplicateMessage(String messageKey) {
        Long lastSentTime = sentMessages.get(messageKey);

        if (lastSentTime == null) {
            return false;
        }

        long timeSinceLastMessage = System.currentTimeMillis() - lastSentTime;

        // Clean up old messages
        cleanupOldMessages();

        return TimeUnit.MILLISECONDS.toMinutes(timeSinceLastMessage) < MESSAGE_EXPIRY_MINUTES;
    }

    private void cleanupOldMessages() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(MESSAGE_EXPIRY_MINUTES);

        sentMessages.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }
}