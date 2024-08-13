package com.taskvantage.backend.config;

import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.model.User;
import com.taskvantage.backend.repository.TaskRepository;
import com.taskvantage.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(UserRepository userRepository, TaskRepository taskRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if the test user already exists
            User user = userRepository.findByUsername("pat@example.com");
            if (user == null) {
                user = new User();
                user.setUsername("pat@example.com");
                user.setPassword(passwordEncoder.encode("password"));
                userRepository.save(user);
                System.out.println("Test user created: username=pat@example.com, password=password");
            } else {
                System.out.println("Test user already exists.");
            }

            // Check if the test task already exists for the user
            if (taskRepository.findAll().isEmpty()) {
                Task task = new Task();
                task.setTitle("Test Task");
                task.setDescription("This is a test task description.");
                task.setPriority("High");
                task.setStatus("Pending");
                task.setDueDate(LocalDateTime.now().plusDays(7));
                task.setCreationDate(LocalDateTime.now());
                task.setLastModifiedDate(LocalDateTime.now());
                task.setTags(Arrays.asList("test", "sample"));
                task.setSubtasks(List.of());  // Add any subtasks if needed
                task.setAttachments(List.of());  // Add any attachments if needed
                task.setComments(List.of());  // Add any comments if needed
                task.setReminders(List.of(LocalDateTime.now().plusDays(1)));  // Add any reminders if needed
                task.setRecurring(false);  // Set to true if the task is recurring

                taskRepository.save(task);
                System.out.println("Test task created: title=Test Task, status=Pending");
            } else {
                System.out.println("Test task already exists.");
            }
        };
    }
}
