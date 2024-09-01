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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
        LocalDateTime nowUTC = LocalDateTime.now(ZoneOffset.UTC).withNano(0); // Truncate milliseconds
        LocalDateTime endUTC = nowUTC.plusMinutes(15).withNano(0);

        String nowUTCString = nowUTC.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String endUTCString = endUTC.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        logger.info("Querying for tasks with scheduled_start between {} and {}", nowUTCString, endUTCString);

        List<Task> tasks = taskRepository.findTasksToNotify(nowUTCString, endUTCString);

        logger.info("Found {} tasks within the time range", tasks.size());

        for (Task task : tasks) {
            // Check if the task's notification has not been sent
            if (task.getNotificationSent() == null || !task.getNotificationSent()) {
                User user = userRepository.findById(task.getUserId()).orElse(null);
                if (user != null && user.getToken() != null) {
                    String message = "Your task '" + task.getTitle() + "' is starting in less than 15 minutes.";
                    firebaseNotificationService.sendNotification(user.getToken(), task.getTitle(), message);

                    // Update the task to mark notification as sent
                    task.setNotificationSent(true);
                    taskRepository.save(task);
                } else {
                    logger.warn("User with ID {} does not have an FCM token.", task.getUserId());
                }
            }
        }

        logger.info("Notification check completed.");
    }
}