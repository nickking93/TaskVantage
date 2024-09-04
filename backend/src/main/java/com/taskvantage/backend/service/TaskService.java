package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.model.Task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskService {

    Task addTask(Task task);

    Optional<Task> getTaskById(Long id);

    List<Task> getAllTasks();

    List<TaskSummary> getTasksByUserId(Long userId);

    // New method to get non-completed tasks by user ID
    List<TaskSummary> getNonCompletedTasksByUserId(Long userId);

    Task updateTask(Task task);

    void deleteTask(Long id);

    // Existing method to get task summary
    TaskSummary getTaskSummary(Long userId);

    void startTask(Long taskId, LocalDateTime startDate);

    void markTaskAsCompleted(Long taskId);
}