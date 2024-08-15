package com.taskvantage.backend.controller;

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
    public ResponseEntity<List<Task>> getTasksByUserId(@PathVariable Long userId) {
        List<Task> tasks = taskService.getTasksByUserId(userId);
        return ResponseEntity.ok(tasks);
    }

    // Get task summary for a specific user
    @GetMapping("/summary/{userId}")
    public ResponseEntity<Map<String, Object>> getTaskSummary(@PathVariable Long userId) {
        List<Task> tasks = taskService.getTasksByUserId(userId);

        long totalTasks = tasks.size();
        long totalSubtasks = tasks.stream().mapToLong(task -> task.getSubtasks().size()).sum();
        long pastDeadlineTasks = tasks.stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDateTime.now()))
                .count();
        long completedTasksThisMonth = tasks.stream()
                .filter(task -> "Completed".equalsIgnoreCase(task.getStatus()) &&
                        task.getDueDate() != null &&
                        task.getDueDate().getMonth() == LocalDateTime.now().getMonth() &&
                        task.getDueDate().getYear() == LocalDateTime.now().getYear())
                .count();
        long totalTasksThisMonth = tasks.stream()
                .filter(task -> task.getDueDate() != null &&
                        task.getDueDate().getMonth() == LocalDateTime.now().getMonth() &&
                        task.getDueDate().getYear() == LocalDateTime.now().getYear())
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTasks", totalTasks);
        summary.put("totalSubtasks", totalSubtasks);
        summary.put("pastDeadlineTasks", pastDeadlineTasks);
        summary.put("completedTasksThisMonth", completedTasksThisMonth);
        summary.put("totalTasksThisMonth", totalTasksThisMonth);

        return ResponseEntity.ok(summary);
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
