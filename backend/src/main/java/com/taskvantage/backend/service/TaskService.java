package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.model.Task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskService {

    // Adds a new task
    Task addTask(Task task);

    // Retrieves a task by its ID
    Optional<Task> getTaskById(Long id);

    // Retrieves all tasks
    List<Task> getAllTasks();

    // Retrieves tasks by a specific user ID
    List<TaskSummary> getTasksByUserId(Long userId);

    // Retrieves non-completed tasks for a specific user ID
    List<TaskSummary> getNonCompletedTasksByUserId(Long userId);

    // Updates an existing task
    Task updateTask(Task task);

    // Deletes a task by its ID
    void deleteTask(Long id);

    // Retrieves a summary of tasks for a specific user ID
    TaskSummary getTaskSummary(Long userId);

    // Starts a task with startDate as LocalDateTime
    void startTask(Long taskId, LocalDateTime startDate);

    // Marks a task as completed
    void markTaskAsCompleted(Long taskId);
}