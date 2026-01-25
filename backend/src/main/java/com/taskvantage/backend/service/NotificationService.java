package com.taskvantage.backend.service;

import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.model.User;
import com.taskvantage.backend.repository.TaskRepository;
import com.taskvantage.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final Map<String, NotificationAttempt> notificationAttempts = new ConcurrentHashMap<>();
    private static final long NOTIFICATION_COOLDOWN_MINUTES = 15;
    private static final long NOTIFICATION_WINDOW_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 3;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final FirebaseNotificationService firebaseNotificationService;

    private static class NotificationAttempt {
        final long timestamp;
        int attempts;
        boolean success;

        NotificationAttempt() {
            this.timestamp = System.currentTimeMillis();
            this.attempts = 1;
            this.success = false;
        }

        boolean canRetry() {
            return attempts < MAX_ATTEMPTS && !success;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMillis(NOTIFICATION_COOLDOWN_MINUTES);
        }
    }

    @Autowired
    public NotificationService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            FirebaseNotificationService firebaseNotificationService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.firebaseNotificationService = firebaseNotificationService;
    }

    @Scheduled(fixedRate = 60000) // Runs every minute
    @Transactional
    public void checkAndSendNotifications() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime windowEnd = now.plusMinutes(NOTIFICATION_WINDOW_MINUTES);

        logger.debug("Starting notification check at {} for tasks starting before {}", now, windowEnd);

        List<User> users = userRepository.findUsersWithValidTokens();
        logger.debug("Processing notifications for {} users", users.size());

        cleanupOldAttempts();

        for (User user : users) {
            try {
                processUserTasks(user, now, windowEnd);
            } catch (Exception e) {
                logger.error("Error processing notifications for user ID {}: {}", user.getId(), e.getMessage(), e);
            }
        }

        logger.debug("Notification check completed at {}", ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    private void processUserTasks(User user, ZonedDateTime now, ZonedDateTime windowEnd) {
        if (user.getToken() == null) {
            logger.debug("Skipping notification check for user ID {} - no FCM token", user.getId());
            return;
        }

        List<Task> tasksToNotify = taskRepository.findTasksScheduledBetween(
                user.getId(),
                now,
                windowEnd
        );

        for (Task task : tasksToNotify) {
            if (shouldSendNotification(task, now)) {
                String notificationKey = createNotificationKey(user.getId(), task.getId());
                sendNotification(user, task, now, notificationKey);
            }
        }
    }

    private boolean shouldSendNotification(Task task, ZonedDateTime now) {
        // Skip if notification was already sent or task is completed
        if (Boolean.TRUE.equals(task.getNotificationSent()) || Task.isCompletedStatus(task.getStatus())) {
            logger.debug("Skipping notification for task '{}' - already sent or completed", task.getTitle());
            return false;
        }

        // Calculate minutes until task starts
        long minutesUntilStart = ChronoUnit.MINUTES.between(now, task.getScheduledStart());

        // Only send notification if we're within the notification window
        return minutesUntilStart <= NOTIFICATION_WINDOW_MINUTES && minutesUntilStart > 0;
    }

    private String createNotificationKey(Long userId, Long taskId) {
        return String.format("%d-%d", userId, taskId);
    }

    private void sendNotification(User user, Task task, ZonedDateTime now, String notificationKey) {
        NotificationAttempt attempt = notificationAttempts.get(notificationKey);

        if (attempt != null && !attempt.isExpired()) {
            if (!attempt.canRetry()) {
                logger.debug("Skipping notification for task '{}' - max attempts reached or already successful",
                        task.getTitle());
                return;
            }
            attempt.attempts++;
        } else {
            attempt = new NotificationAttempt();
            notificationAttempts.put(notificationKey, attempt);
        }

        try {
            // Set notification as sent before actually sending to prevent race conditions
            task.setNotificationSent(true);
            task = taskRepository.save(task);

            // Calculate minutes until task starts
            long minutesUntilStart = ChronoUnit.MINUTES.between(now, task.getScheduledStart());
            String timeMessage = minutesUntilStart > 1
                    ? String.format("starts in %d minutes", minutesUntilStart)
                    : "starts in less than a minute";

            String message = String.format("Your task '%s' %s", task.getTitle(), timeMessage);

            boolean success = firebaseNotificationService.sendNotification(
                    user.getToken(),
                    task.getTitle(),
                    message,
                    user.getUsername()
            );

            if (success) {
                logger.debug("Successfully sent notification for task ID {} to user ID {}",
                        task.getId(), user.getId());
                attempt.success = true;
            } else {
                // If notification fails, revert the notification sent flag
                task.setNotificationSent(false);
                taskRepository.save(task);
                logger.error("Failed to send notification for task '{}'", task.getTitle());
            }

        } catch (Exception e) {
            // If notification fails, revert the notification sent flag
            task.setNotificationSent(false);
            taskRepository.save(task);

            logger.error("Failed to send notification for task ID {} to user ID {}: {}",
                    task.getId(), user.getId(), e.getMessage(), e);
        }
    }

    private void cleanupOldAttempts() {
        notificationAttempts.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
