package com.taskvantage.backend.config;

import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.model.User;
import com.taskvantage.backend.repository.TaskRepository;
import com.taskvantage.backend.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

            // Load tasks from CSV file
            if (taskRepository.findAll().isEmpty()) {
                String csvFilePath = "./src/main/java/com/taskvantage/backend/data/task_test_data.csv";
                try (
                        Reader reader = new FileReader(Paths.get(csvFilePath).toFile());
                        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())
                ) {
                    List<Task> tasks = new ArrayList<>();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

                    for (CSVRecord record : csvParser) {
                        Task task = new Task();
                        task.setTitle(record.get("title"));
                        task.setDescription(record.get("description"));
                        task.setPriority(record.get("priority"));
                        task.setStatus(record.get("status"));
                        task.setDueDate(LocalDateTime.parse(record.get("due_date"), formatter));
                        task.setCreationDate(LocalDateTime.parse(record.get("creation_date"), formatter));
                        task.setLastModifiedDate(LocalDateTime.parse(record.get("last_modified_date"), formatter)); // Updated
                        task.setStartDate(LocalDateTime.parse(record.get("actual_start"), formatter)); // Updated
                        task.setScheduledStart(LocalDateTime.parse(record.get("scheduledStart"), formatter)); // Updated

                        // Handle optional completionDateTime
                        String completionDateTime = record.get("completionDateTime");
                        if (completionDateTime != null && !completionDateTime.isEmpty()) {
                            task.setCompletionDateTime(LocalDateTime.parse(completionDateTime, formatter));
                        }

                        // Set the userId for the task
                        task.setUserId(user.getId());

                        tasks.add(task);
                    }

                    taskRepository.saveAll(tasks);
                    System.out.println("Tasks imported from CSV and saved to database.");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error loading tasks from CSV file.");
                }
            } else {
                System.out.println("Tasks already exist.");
            }
        };
    }
}
