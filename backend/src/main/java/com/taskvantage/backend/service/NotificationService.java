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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final ConcurrentHashMap<String, Long> lastNotificationTimes = new ConcurrentHashMap<>();
    private static final long NOTIFICATION_COOLDOWN_MINUTES = 15;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final FirebaseNotificationService firebaseNotificationService;

    @Autowired
    public NotificationService(TaskRepository taskRepository, UserRepository userRepository, FirebaseNotificationService firebaseNotificationService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.firebaseNotificationService = firebaseNotificationService;
    }

    @Scheduled(fixedRate = 60000) // Runs every minute
    @Transactional
    public void checkAndSendNotifications() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        // Narrow the window to exactly 15 minutes from now
        ZonedDateTime exactWindow = now.plusMinutes(15);

        logger.debug("Starting notification check at {} for tasks at {}", now, exactWindow);

        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                processUserTasks(user, now, exactWindow);
            } catch (Exception e) {
                logger.error("Error processing notifications for user {}: {}", user.getUsername(), e.getMessage(), e);
            }
        }

        logger.debug("Notification check completed at {}", ZonedDateTime.now(ZoneOffset.UTC));
    }

    private void processUserTasks(User user, ZonedDateTime now, ZonedDateTime targetTime) {
        logger.debug("Processing tasks for user: {}", user.getUsername());

        // Get tasks that start exactly at the target time (15 minutes from now)
        List<Task> tasksToNotify = taskRepository.findTasksScheduledBetween(
                user.getId(),
                targetTime.minusMinutes(1), // 1 minute buffer before
                targetTime.plusMinutes(1)   // 1 minute buffer after
        );

        for (Task task : tasksToNotify) {
            String taskKey = String.format("%d-%d", user.getId(), task.getId());

            if (canSendNotification(taskKey) && !isNotificationSentRecently(task)) {
                sendTaskNotification(user, task);
            } else {
                logger.debug("Skipping notification for task '{}' due to rate limiting or previous send",
                        task.getTitle());
            }
        }
    }

    private boolean canSendNotification(String taskKey) {
        Long lastNotificationTime = lastNotificationTimes.get(taskKey);
        long currentTime = System.currentTimeMillis();

        if (lastNotificationTime == null ||
                TimeUnit.MILLISECONDS.toMinutes(currentTime - lastNotificationTime) >= NOTIFICATION_COOLDOWN_MINUTES) {
            lastNotificationTimes.put(taskKey, currentTime);
            return true;
        }
        return false;
    }

    private boolean isNotificationSentRecently(Task task) {
        return task.getNotificationSent() != null && task.getNotificationSent();
    }

    private void sendTaskNotification(User user, Task task) {
        if (user.getToken() == null) {
            logger.warn("User {} has no FCM token registered", user.getUsername());
            return;
        }

        try {
            String message = String.format("Your task '%s' is starting in %d minutes",
                    task.getTitle(), 15);

            firebaseNotificationService.sendNotification(user.getToken(), task.getTitle(), message);

            task.setNotificationSent(true);
            taskRepository.save(task);

            logger.info("Successfully sent notification for task '{}' to user '{}'",
                    task.getTitle(), user.getUsername());

        } catch (Exception e) {
            logger.error("Failed to send notification for task '{}' to user '{}': {}",
                    task.getTitle(), user.getUsername(), e.getMessage(), e);
        }
    }
}