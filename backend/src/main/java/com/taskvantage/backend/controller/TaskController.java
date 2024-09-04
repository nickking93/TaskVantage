package com.taskvantage.backend.controller;

import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.model.User;
import com.taskvantage.backend.service.TaskService;
import com.taskvantage.backend.service.CustomUserDetailsService;
import com.taskvantage.backend.Security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public TaskController(TaskService taskService, JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService) {
        this.taskService = taskService;
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
    }

    // Create a new task
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        Task createdTask = taskService.addTask(task);
        return ResponseEntity.ok(createdTask);
    }

    // Get a task by its ID
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        Optional<Task> taskOptional = taskService.getTaskById(id);
        return taskOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get all tasks for a specific user by userId
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskSummary>> getTasksByUserId(@PathVariable Long userId) {
        List<TaskSummary> tasks = taskService.getTasksByUserId(userId);
        return ResponseEntity.ok(tasks);
    }

    // Get task summary for a specific user
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

    // Start a task by setting its start date and changing its status to 'In Progress'
    @PatchMapping("/{id}/start")
    public ResponseEntity<Void> startTask(@PathVariable Long id) {
        taskService.startTask(id, LocalDateTime.now());
        return ResponseEntity.noContent().build();
    }

    // Mark a task as completed
    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> markTaskAsCompleted(@PathVariable Long id) {
        taskService.markTaskAsCompleted(id);
        return ResponseEntity.noContent().build();
    }

    // Update an existing task
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id);
        Task updatedTask = taskService.updateTask(task);
        return updatedTask != null ? ResponseEntity.ok(updatedTask) : ResponseEntity.notFound().build();
    }

    // Delete a task by its ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // Update FCM token
    @PostMapping("/{userId}/update-token")
    public ResponseEntity<Map<String, Object>> updateFcmToken(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long userId,
            @RequestBody Map<String, String> tokenRequest) {

        // Add a log to see if this method is being called
        System.out.println("Received request to update FCM token for user: " + userId);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized: Invalid or missing Authorization header"));
        }

        String jwtToken = authorizationHeader.substring(7);
        String username;
        Long tokenUserId;
        try {
            // Extract username and userId from JWT
            username = jwtUtil.getUsernameFromToken(jwtToken);
            tokenUserId = jwtUtil.getUserIdFromToken(jwtToken);
            System.out.println("Extracted username: " + username + ", token userId: " + tokenUserId);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Invalid JWT token"));
        }

        // Ensure that the userId from the JWT matches the userId from the path
        if (!userId.equals(tokenUserId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: User ID mismatch"));
        }

        String fcmToken = tokenRequest.get("fcmToken");
        if (fcmToken == null || fcmToken.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("message", "Invalid FCM token"));
        }

        // Update FCM token
        customUserDetailsService.updateUserToken(username, fcmToken);
        return ResponseEntity.ok(Map.of("message", "FCM Token updated successfully"));
    }
}