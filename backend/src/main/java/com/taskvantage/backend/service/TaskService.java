package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskService {

    Task addTask(Task task);

    Optional<Task> getTaskById(Long id);

    List<Task> getAllTasks();

    List<TaskSummary> getTasksByUserId(Long userId);  // Updated return type

    Task updateTask(Task task);

    void deleteTask(Long id);

    // Method to retrieve task summary information
    TaskSummary getTaskSummary(Long userId);
}
