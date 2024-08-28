package com.taskvantage.backend.controller;

import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.service.TaskService;
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

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
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
        List<TaskSummary> allTasks = taskService.getTasksByUserId(userId);

        // Filter out completed tasks
        List<TaskSummary> nonCompletedTasks = allTasks.stream()
                .filter(task -> !"Completed".equalsIgnoreCase(task.getStatus()))
                .toList();

        // Calculations excluding completed tasks
        long totalTasks = nonCompletedTasks.size();
        long totalSubtasks = nonCompletedTasks.stream().mapToLong(TaskSummary::getTotalSubtasks).sum();
        long pastDeadlineTasks = nonCompletedTasks.stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDateTime.now()))
                .count();

        // Monthly tasks calculations including completed tasks
        long completedTasksThisMonth = allTasks.stream()
                .filter(task -> "Completed".equalsIgnoreCase(task.getStatus()) &&
                        task.getDueDate() != null &&
                        task.getDueDate().getMonth() == LocalDateTime.now().getMonth() &&
                        task.getDueDate().getYear() == LocalDateTime.now().getYear())
                .count();
        long totalTasksThisMonth = allTasks.stream()
                .filter(task -> task.getDueDate() != null &&
                        task.getDueDate().getMonth() == LocalDateTime.now().getMonth() &&
                        task.getDueDate().getYear() == LocalDateTime.now().getYear())
                .count();

        // Prepare the summary response
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTasks", totalTasks);
        summary.put("totalSubtasks", totalSubtasks);
        summary.put("pastDeadlineTasks", pastDeadlineTasks);
        summary.put("completedTasksThisMonth", completedTasksThisMonth);
        summary.put("totalTasksThisMonth", totalTasksThisMonth);

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
        if (updatedTask != null) {
            return ResponseEntity.ok(updatedTask);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete a task by its ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
