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

    Task updateTask(Task task);

    void deleteTask(Long id);

    TaskSummary getTaskSummary(Long userId);

    void startTask(Long taskId, LocalDateTime startDate);

    void markTaskAsCompleted(Long taskId);
}
