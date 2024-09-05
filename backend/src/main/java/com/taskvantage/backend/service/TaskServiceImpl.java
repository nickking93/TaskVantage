package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.exception.TaskNotFoundException;
import com.taskvantage.backend.model.Subtask;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.model.TaskPriority;
import com.taskvantage.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
        // Set the creation and last modified dates to the current time in UTC
        task.setCreationDate(LocalDateTime.now());
        task.setLastModifiedDate(LocalDateTime.now());

        // Save the task in the repository
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
    public List<TaskSummary> getNonCompletedTasksByUserId(Long userId) {
        return taskRepository.findTaskSummariesByUserId(userId).stream()
                .filter(task -> !"Completed".equalsIgnoreCase(task.getStatus()))
                .toList();
    }

    @Override
    public TaskSummary getTaskSummary(Long userId) {
        List<TaskSummary> tasks = taskRepository.findTaskSummariesByUserId(userId);

        long totalTasks = tasks.size();
        long totalSubtasks = tasks.stream().mapToLong(TaskSummary::getTotalSubtasks).sum();
        long pastDeadlineTasks = tasks.stream()
                .filter(task -> task.getDueDate() != null &&
                        task.getDueDate().isBefore(LocalDateTime.now()) && // Compare directly with LocalDateTime
                        !"Completed".equalsIgnoreCase(task.getStatus()))
                .count();

        YearMonth currentMonth = YearMonth.now(); // Use current time in UTC
        long completedTasksThisMonth = tasks.stream()
                .filter(task -> "Completed".equalsIgnoreCase(task.getStatus()) &&
                        task.getDueDate() != null &&
                        YearMonth.from(task.getDueDate()).equals(currentMonth))
                .count();

        long totalTasksThisMonth = tasks.stream()
                .filter(task -> task.getDueDate() != null &&
                        YearMonth.from(task.getDueDate()).equals(currentMonth))
                .count();

        TaskSummary summary = new TaskSummary();
        summary.setTotalTasks(totalTasks);
        summary.setTotalSubtasks((int) totalSubtasks);
        summary.setPastDeadlineTasks(pastDeadlineTasks);
        summary.setCompletedTasksThisMonth(completedTasksThisMonth);
        summary.setTotalTasksThisMonth(totalTasksThisMonth);

        return summary;
    }

    @Override
    public Task updateTask(Task updatedTask) {
        Optional<Task> existingTaskOptional = taskRepository.findById(updatedTask.getId());

        if (existingTaskOptional.isPresent()) {
            Task existingTask = existingTaskOptional.get();

            // Update fields with new values from updatedTask
            existingTask.setTitle(updatedTask.getTitle());
            existingTask.setDescription(updatedTask.getDescription());

            // Ensure priority is set (default if null)
            if (updatedTask.getPriority() != null) {
                existingTask.setPriority(updatedTask.getPriority());
            }

            // Only update the status if provided in the updated task
            if (updatedTask.getStatus() != null) {
                existingTask.setStatus(updatedTask.getStatus());
            }

            // Update optional fields
            if (updatedTask.getDueDate() != null) {
                existingTask.setDueDate(updatedTask.getDueDate());
            }

            // Update startDate and other time-based fields
            if (updatedTask.getStartDate() != null) {
                existingTask.setStartDate(updatedTask.getStartDate());
            }

            if (updatedTask.getScheduledStart() != null) {
                existingTask.setScheduledStart(updatedTask.getScheduledStart());
            }

            if (updatedTask.getCompletionDateTime() != null) {
                existingTask.setCompletionDateTime(updatedTask.getCompletionDateTime());
            }

            // Update last modified date to current time
            existingTask.setLastModifiedDate(LocalDateTime.now());

            // Handle subtasks
            if (updatedTask.getSubtasks() != null) {
                List<Subtask> existingSubtasks = existingTask.getSubtasks();

                // Remove subtasks that are no longer present in the updatedTask
                existingSubtasks.removeIf(subtask -> !updatedTask.getSubtasks().contains(subtask));

                // Add new subtasks that aren't already in the existing collection
                for (Subtask newSubtask : updatedTask.getSubtasks()) {
                    if (!existingSubtasks.contains(newSubtask)) {
                        existingSubtasks.add(newSubtask);
                        newSubtask.setTask(existingTask);  // Set reference to the parent task
                    }
                }
            }

            // Handle comments as before
            if (updatedTask.getComments() != null) {
                existingTask.getComments().clear();  // Clear existing comments
                existingTask.getComments().addAll(updatedTask.getComments());  // Add new/updated comments
            }

            // Handle other fields
            existingTask.setTags(updatedTask.getTags());
            existingTask.setAttachments(updatedTask.getAttachments());
            existingTask.setReminders(updatedTask.getReminders());
            existingTask.setRecurring(updatedTask.isRecurring());
            existingTask.setNotificationSent(updatedTask.getNotificationSent());

            // Calculate the duration if both startDate and completionDateTime are present
            if (updatedTask.getCompletionDateTime() != null && updatedTask.getStartDate() != null) {
                Duration duration = Duration.between(updatedTask.getStartDate(), updatedTask.getCompletionDateTime());
                existingTask.setDuration(duration);
            }

            return taskRepository.save(existingTask);
        } else {
            throw new TaskNotFoundException("Task with id " + updatedTask.getId() + " not found.");
        }
    }

    @Override
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Override
    public void startTask(Long taskId, LocalDateTime startDate) {
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();

            // Set default priority if not set
            if (task.getPriority() == null) {
                task.setPriority(TaskPriority.MEDIUM); // Default priority
            }

            task.setStartDate(startDate);
            task.setStatus("In Progress"); // Set task status to "In Progress"
            task.setLastModifiedDate(LocalDateTime.now());

            taskRepository.save(task);
        } else {
            throw new TaskNotFoundException("Task not found with id " + taskId);
        }
    }

    @Override
    public void markTaskAsCompleted(Long taskId) {
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            task.setStatus("Complete");
            task.setLastModifiedDate(LocalDateTime.now());
            taskRepository.save(task);
        } else {
            throw new TaskNotFoundException("Task not found with id " + taskId);
        }
    }
}