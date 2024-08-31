package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.exception.TaskNotFoundException;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Task addTask(Task task) {
        task.setCreationDate(LocalDateTime.now());
        task.setLastModifiedDate(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Override
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public List<TaskSummary> getTasksByUserId(Long userId) {
        return taskRepository.findTaskSummariesByUserId(userId);
    }

    @Override
    public Task updateTask(Task updatedTask) {
        Optional<Task> existingTaskOptional = taskRepository.findById(updatedTask.getId());

        if (existingTaskOptional.isPresent()) {
            Task existingTask = existingTaskOptional.get();

            // Update the fields with the new values from updatedTask
            existingTask.setTitle(updatedTask.getTitle());
            existingTask.setDescription(updatedTask.getDescription());
            existingTask.setPriority(updatedTask.getPriority());
            existingTask.setStatus(updatedTask.getStatus());
            existingTask.setDueDate(updatedTask.getDueDate());
            existingTask.setLastModifiedDate(LocalDateTime.now());
            existingTask.setScheduledStart(updatedTask.getScheduledStart()); // Update scheduledStart
            existingTask.setStartDate(updatedTask.getStartDate()); // Update startDate
            existingTask.setCompletionDateTime(updatedTask.getCompletionDateTime()); // Update completionDateTime
            existingTask.setTags(updatedTask.getTags());
            existingTask.setSubtasks(updatedTask.getSubtasks());
            existingTask.setAttachments(updatedTask.getAttachments());
            existingTask.setComments(updatedTask.getComments());
            existingTask.setReminders(updatedTask.getReminders());
            existingTask.setRecurring(updatedTask.isRecurring());
            // Calculate the duration if completionDateTime and startDate are present
            if (updatedTask.getCompletionDateTime() != null && updatedTask.getStartDate() != null) {
                Duration duration = Duration.between(updatedTask.getStartDate(), updatedTask.getCompletionDateTime());
                existingTask.setDuration(duration);
            }

            return taskRepository.save(existingTask);
        } else {
            // Consider throwing an exception if the task is not found
            throw new TaskNotFoundException("Task with id " + updatedTask.getId() + " not found.");
        }
    }


    @Override
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Override
    public TaskSummary getTaskSummary(Long userId) {
        List<TaskSummary> tasks = taskRepository.findTaskSummariesByUserId(userId);

        long totalTasks = tasks.size();
        long totalSubtasks = tasks.stream().mapToLong(TaskSummary::getTotalSubtasks).sum();
        long pastDeadlineTasks = tasks.stream()
                .filter(task -> task.getDueDate() != null &&
                        task.getDueDate().isBefore(LocalDateTime.now()) &&
                        !"Completed".equalsIgnoreCase(task.getStatus())) // Exclude completed tasks
                .count();

        YearMonth currentMonth = YearMonth.now();
        long completedTasksThisMonth = tasks.stream()
                .filter(task -> "Completed".equalsIgnoreCase(task.getStatus()) &&
                        task.getDueDate() != null &&
                        YearMonth.from(task.getDueDate()).equals(currentMonth))
                .count();

        long totalTasksThisMonth = tasks.stream()
                .filter(task -> task.getDueDate() != null && YearMonth.from(task.getDueDate()).equals(currentMonth))
                .count();

        TaskSummary summary = new TaskSummary();  // Using the no-argument constructor
        summary.setTotalTasks(totalTasks);
        summary.setTotalSubtasks(totalSubtasks);
        summary.setPastDeadlineTasks(pastDeadlineTasks);
        summary.setCompletedTasksThisMonth(completedTasksThisMonth);
        summary.setTotalTasksThisMonth(totalTasksThisMonth);

        return summary;
    }

    // New method to start a task
    @Override
    public void startTask(Long taskId, LocalDateTime startDate) {
        taskRepository.startTask(taskId, startDate);
    }

    @Override
    public void markTaskAsCompleted(Long taskId) {
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            task.setStatus("Complete");
            task.setLastModifiedDate(LocalDateTime.now(ZoneOffset.UTC)); // Set in UTC
            taskRepository.save(task);
        } else {
            throw new TaskNotFoundException("Task not found with id " + taskId);
        }
    }

}
