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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

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
    public void checkAndSendNotifications() {
        // Current time in UTC
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusMinutes(15); // Time window of 15 minutes

        logger.info("Checking for tasks with scheduled_start between {} and {}", now, end);

        // Retrieve all users from the repository
        List<User> users = userRepository.findAll();

        for (User user : users) {
            logger.info("Processing user: {}", user.getUsername());

            // Get all tasks scheduled to start in the next 15 minutes
            List<Task> tasksToNotify = taskRepository.findTasksScheduledBetween(user.getId(), now, end);

            if (tasksToNotify.isEmpty()) {
                logger.info("No tasks found for user {} in the scheduled time range.", user.getUsername());
            }

            tasksToNotify.forEach(task -> {
                // Log the scheduled_start time for each task
                logger.info("Checking task '{}', scheduled_start: {}", task.getTitle(), task.getScheduledStart());

                // Check if the task's notification has not been sent
                if (task.getNotificationSent() == null || !task.getNotificationSent()) {
                    if (user.getToken() != null) {
                        String message = "Your task '" + task.getTitle() + "' is starting in less than 15 minutes.";
                        firebaseNotificationService.sendNotification(user.getToken(), task.getTitle(), message);

                        // Mark the notification as sent
                        task.setNotificationSent(true);
                        taskRepository.save(task); // Update the task in the database

                        logger.info("Notification sent for task '{}' to user '{}'", task.getTitle(), user.getUsername());
                    } else {
                        logger.warn("User with ID {} does not have an FCM token.", user.getId());
                    }
                } else {
                    logger.info("Notification already sent for task '{}'", task.getTitle());
                }
            });
        }

        logger.info("Notification check completed.");
    }
}