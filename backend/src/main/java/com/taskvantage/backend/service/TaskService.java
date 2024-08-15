package com.taskvantage.backend.service;

import com.taskvantage.backend.model.Task;
import java.util.List;
import java.util.Optional;

public interface TaskService {

    Task addTask(Task task);

    Optional<Task> getTaskById(Long id);

    List<Task> getAllTasks();

    List<Task> getTasksByUserId(Long userId); // New method to get tasks by userId

    Task updateTask(Task task);

    void deleteTask(Long id);

    // Additional methods for business logic can be added here
}
