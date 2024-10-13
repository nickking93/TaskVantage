package com.taskvantage.backend.controller;
import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.service.TaskService;
import com.taskvantage.backend.service.CustomUserDetailsService;
import com.taskvantage.backend.Security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.MalformedJwtException;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public TaskController(TaskService taskService, JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService) {
        this.taskService = taskService;
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody Task task) {
        Map<String, Object> response = new HashMap<>();

        if (task.getTitle() == null || task.getTitle().isEmpty()) {
            response.put("message", "Title is required");
            return ResponseEntity.badRequest().body(response);
        }

        // Convert dueDate to UTC if it's provided
        if (task.getDueDate() != null) {
            ZonedDateTime localDueDate = task.getDueDate();
            ZonedDateTime utcDueDate = localDueDate.withZoneSameInstant(ZoneOffset.UTC);  // Convert to UTC
            task.setDueDate(utcDueDate);
            logger.info("Converted Due Date to UTC: {}", utcDueDate);
        }

        // Convert scheduledStart to UTC if it's provided
        if (task.getScheduledStart() != null) {
            ZonedDateTime localScheduledStart = task.getScheduledStart();
            ZonedDateTime utcScheduledStart = localScheduledStart.withZoneSameInstant(ZoneOffset.UTC);  // Convert to UTC
            task.setScheduledStart(utcScheduledStart);
            logger.info("Converted Scheduled Start to UTC: {}", utcScheduledStart);
        }

        // Create and save the task
        Task createdTask = taskService.addTask(task);

        logger.info("Task created successfully with due date: {} and scheduled start: {}",
                createdTask.getDueDate(), createdTask.getScheduledStart());

        response.put("task", createdTask);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        // Logging the received ID for debugging
        System.out.println("Fetching task with ID: " + id);

        // Fetch the task by ID
        Optional<Task> taskOptional = taskService.getTaskById(id);

        // Check if the task exists
        if (taskOptional.isEmpty()) {
            response.put("message", "Task not found with ID: " + id);
            System.out.println("Task not found with ID: " + id);  // Log if task is not found
            return ResponseEntity.status(404).body(response);  // Return 404 if task is not found
        }

        // Log the task details for debugging
        Task task = taskOptional.get();
        System.out.println("Fetched Task: " + task);

        // Add task to the response
        response.put("task", task);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskSummary>> getTasksByUserId(@PathVariable Long userId) {
        List<TaskSummary> tasks = taskService.getTasksByUserId(userId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/summary/{userId}")
    public ResponseEntity<Map<String, Object>> getTaskSummary(@PathVariable Long userId) {
        TaskSummary taskSummary = taskService.getTaskSummary(userId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTasks", taskSummary.getTotalTasks());
        summary.put("totalSubtasks", taskSummary.getTotalSubtasks());
        summary.put("pastDeadlineTasks", taskSummary.getPastDeadlineTasks());
        summary.put("completedTasksThisMonth", taskSummary.getCompletedTasksThisMonth());
        summary.put("totalTasksThisMonth", taskSummary.getTotalTasksThisMonth());

        return ResponseEntity.ok(summary);
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startTask(@PathVariable Long id, @RequestBody Task updatedTask) {
        Map<String, Object> response = new HashMap<>();

        // Retrieve the existing task
        Optional<Task> taskOptional = taskService.getTaskById(id);
        if (taskOptional.isEmpty()) {
            response.put("message", "Task not found");
            return ResponseEntity.notFound().build();
        }

        Task task = taskOptional.get();

        // Update the task with the new start date and status
        task.setStartDate(updatedTask.getStartDate() != null ? updatedTask.getStartDate().withZoneSameInstant(ZoneOffset.UTC) : ZonedDateTime.now(ZoneOffset.UTC));
        task.setStatus("In Progress");
        task.setLastModifiedDate(ZonedDateTime.now(ZoneOffset.UTC));

        // Save the updated task
        taskService.updateTask(task);
        response.put("message", "Task started successfully");
        response.put("task", task);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> markTaskAsCompleted(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        Optional<Task> taskOptional = taskService.getTaskById(id);
        if (taskOptional.isEmpty()) {
            response.put("message", "Task not found");
            return ResponseEntity.notFound().build();
        }

        taskService.markTaskAsCompleted(id);
        response.put("message", "Task marked as completed");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTask(@PathVariable Long id, @RequestBody Task task) {
        Map<String, Object> response = new HashMap<>();

        if (task.getId() == null || !task.getId().equals(id)) {
            response.put("message", "Task ID mismatch");
            return ResponseEntity.badRequest().body(response);
        }

        Task updatedTask = taskService.updateTask(task);
        if (updatedTask == null) {
            response.put("message", "Task not found");
            return ResponseEntity.notFound().build();
        }

        response.put("task", updatedTask);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        Optional<Task> taskOptional = taskService.getTaskById(id);
        if (taskOptional.isEmpty()) {
            response.put("message", "Task not found");
            return ResponseEntity.notFound().build();
        }

        taskService.deleteTask(id);
        response.put("message", "Task deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/update-token")
    public ResponseEntity<Map<String, Object>> updateFcmToken(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long userId,
            @RequestBody Map<String, String> tokenRequest) {

        if (!isValidAuthorizationHeader(authorizationHeader)) {
            return createErrorResponse("Unauthorized: Invalid or missing Authorization header", 401);
        }

        String jwtToken = authorizationHeader.substring(7);
        String username;
        Long tokenUserId;
        try {
            Map<String, Object> tokenData = parseJwtToken(jwtToken);
            username = (String) tokenData.get("username");
            tokenUserId = (Long) tokenData.get("userId");
        } catch (ExpiredJwtException e) {
            return createErrorResponse("Forbidden: JWT token expired", 403);
        } catch (SignatureException e) {
            return createErrorResponse("Forbidden: JWT signature does not match", 403);
        } catch (MalformedJwtException e) {
            return createErrorResponse("Forbidden: JWT token is malformed", 403);
        } catch (Exception e) {
            return createErrorResponse("Forbidden: Invalid JWT token", 403);
        }

        if (!userId.equals(tokenUserId)) {
            return createErrorResponse("Forbidden: User ID mismatch", 403);
        }

        String fcmToken = tokenRequest.get("fcmToken");
        if (fcmToken == null || fcmToken.isEmpty()) {
            return createErrorResponse("Invalid FCM token", 400);
        }

        // Update FCM token
        customUserDetailsService.updateUserToken(username, fcmToken);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "FCM Token updated successfully");
        return ResponseEntity.ok(response);
    }

    private boolean isValidAuthorizationHeader(String authorizationHeader) {
        return authorizationHeader != null && authorizationHeader.startsWith("Bearer ");
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, int statusCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.status(statusCode).body(response);
    }

    private Map<String, Object> parseJwtToken(String jwtToken) throws ExpiredJwtException, SignatureException, MalformedJwtException {
        Map<String, Object> tokenData = new HashMap<>();
        String username = jwtUtil.getUsernameFromToken(jwtToken);  // Assuming `jwtUtil` has a method for this
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);        // Assuming `jwtUtil` has a method for this

        tokenData.put("username", username);
        tokenData.put("userId", userId);
        return tokenData;
    }
}