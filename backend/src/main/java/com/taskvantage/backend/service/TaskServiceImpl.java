package com.taskvantage.backend.service;
import com.taskvantage.backend.model.User;
import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.exception.TaskNotFoundException;
import com.taskvantage.backend.model.Comment;
import com.taskvantage.backend.model.Subtask;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.model.TaskPriority;
import com.taskvantage.backend.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);
    private final TaskRepository taskRepository;
    private final GoogleCalendarService googleCalendarService;  // Inject the GoogleCalendarService
    private final CustomUserDetailsService userDetailsService;  // To fetch user details
    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository, GoogleCalendarService googleCalendarService, CustomUserDetailsService userDetailsService, CustomUserDetailsService customUserDetailsService) {
        this.taskRepository = taskRepository;
        this.googleCalendarService = googleCalendarService;
        this.userDetailsService = userDetailsService;
        this.customUserDetailsService = customUserDetailsService;
    }

    private void syncWithGoogleCalendar(Task task, User user) {
        // Only sync if user has Google Calendar connected AND sync is enabled
        if (user != null &&
                user.getGoogleAccessToken() != null &&
                user.isTaskSyncEnabled()) {
            try {
                if (task.getScheduledStart() != null && task.getDueDate() != null) {
                    // Pass `task.isAllDay()` (or appropriate boolean value) to match method signature
                    googleCalendarService.createCalendarEvent(
                            user,
                            task.getTitle(),
                            task.getScheduledStart(),
                            task.getDueDate(),
                            task.isAllDay()
                    );
                }
            } catch (GeneralSecurityException | IOException e) {
                // Log the error but don't fail the task operation
                logger.error("Failed to sync task with Google Calendar", e);
            }
        }
    }


    @Override
    public Task addTask(Task task) {
        // Set creation and last modified dates to current time in UTC
        task.setCreationDate(ZonedDateTime.now(ZoneOffset.UTC));
        task.setLastModifiedDate(ZonedDateTime.now(ZoneOffset.UTC));

        // Ensure that dueDate and scheduledStart remain in UTC
        if (task.getDueDate() != null) {
            task.setDueDate(task.getDueDate().withZoneSameInstant(ZoneOffset.UTC));
        }
        if (task.getScheduledStart() != null) {
            task.setScheduledStart(task.getScheduledStart().withZoneSameInstant(ZoneOffset.UTC));
        }

        // Save task to the database
        Task savedTask = taskRepository.save(task);

        // Sync with Google Calendar if enabled
        User user = customUserDetailsService.findUserById(task.getUserId());
        syncWithGoogleCalendar(savedTask, user);

        return savedTask;
    }

    @Override
    public Task updateTask(Task updatedTask) {
        Optional<Task> existingTaskOptional = taskRepository.findById(updatedTask.getId());

        if (existingTaskOptional.isPresent()) {
            Task existingTask = existingTaskOptional.get();

            // Break down the update logic into helper methods
            updateBasicFields(existingTask, updatedTask);
            updateDates(existingTask, updatedTask);
            updateSubtasks(existingTask, updatedTask);
            updateComments(existingTask, updatedTask);
            updateOtherFields(existingTask, updatedTask);

            // Save the updated task
            Task savedTask = taskRepository.save(existingTask);

            // Sync updates with Google Calendar if enabled
            User user = customUserDetailsService.findUserById(savedTask.getUserId());
            syncWithGoogleCalendar(savedTask, user);

            return savedTask;
        } else {
            throw new TaskNotFoundException(String.format("Task with id %d not found. Unable to update task.", updatedTask.getId()));
        }
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
                        task.getDueDate().isBefore(ZonedDateTime.now(ZoneOffset.UTC)) && // Compare with ZonedDateTime in UTC
                        !"Completed".equalsIgnoreCase(task.getStatus()))
                .count();

        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC); // Use current time in UTC
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

    private void updateBasicFields(Task existingTask, Task updatedTask) {
        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());

        // Use Optional to avoid explicit null checks
        Optional.ofNullable(updatedTask.getPriority()).ifPresent(existingTask::setPriority);
        Optional.ofNullable(updatedTask.getStatus()).ifPresent(existingTask::setStatus);
    }

    private void updateDates(Task existingTask, Task updatedTask) {
        // Use Optional for handling nullable dates
        Optional.ofNullable(updatedTask.getDueDate()).ifPresent(existingTask::setDueDate);
        Optional.ofNullable(updatedTask.getScheduledStart()).ifPresent(existingTask::setScheduledStart);
        Optional.ofNullable(updatedTask.getStartDate()).ifPresent(existingTask::setStartDate);

        // Convert completionDateTime to UTC if provided
        Optional.ofNullable(updatedTask.getCompletionDateTime())
                .map(dateTime -> dateTime.withZoneSameInstant(ZoneOffset.UTC))
                .ifPresent(existingTask::setCompletionDateTime);

        // Always update lastModifiedDate to the current time in UTC
        existingTask.setLastModifiedDate(ZonedDateTime.now(ZoneOffset.UTC));

        // Set duration if both startDate and completionDateTime are provided
        if (updatedTask.getStartDate() != null && updatedTask.getCompletionDateTime() != null) {
            ZonedDateTime completionDateTimeUtc = updatedTask.getCompletionDateTime().withZoneSameInstant(ZoneOffset.UTC);
            Duration duration = Duration.between(updatedTask.getStartDate(), completionDateTimeUtc);
            existingTask.setDuration(duration);
        }
    }

    private void updateSubtasks(Task existingTask, Task updatedTask) {
        if (updatedTask.getSubtasks() != null) {
            List<Subtask> existingSubtasks = existingTask.getSubtasks();

            // Remove subtasks not in the updated list
            existingSubtasks.removeIf(subtask -> !updatedTask.getSubtasks().contains(subtask));

            // Add new subtasks that aren't already in the list
            for (Subtask newSubtask : updatedTask.getSubtasks()) {
                if (!existingSubtasks.contains(newSubtask)) {
                    existingSubtasks.add(newSubtask);
                    newSubtask.setTask(existingTask);  // Set reference to parent task
                }
            }
        }
    }

    private void updateComments(Task existingTask, Task updatedTask) {
        if (updatedTask.getComments() != null) {
            List<Comment> existingComments = existingTask.getComments();
            List<Comment> updatedComments = updatedTask.getComments();

            // Remove comments not in the updated comments
            existingComments.removeIf(comment -> !updatedComments.contains(comment));

            // Add new comments that are not already present
            for (Comment newComment : updatedComments) {
                if (!existingComments.contains(newComment)) {
                    existingComments.add(newComment);
                }
            }
        }
    }

    private void updateOtherFields(Task existingTask, Task updatedTask) {
        // Direct field updates using Optional where needed
        existingTask.setTags(updatedTask.getTags());
        existingTask.setAttachments(updatedTask.getAttachments());
        existingTask.setReminders(updatedTask.getReminders());
        existingTask.setRecurring(updatedTask.isRecurring());
        existingTask.setNotificationSent(updatedTask.getNotificationSent());
    }

    @Override
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Override
    public void startTask(Long taskId, ZonedDateTime startDate) {
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();

            // Set default priority if not set
            if (task.getPriority() == null) {
                task.setPriority(TaskPriority.MEDIUM); // Default priority
            }

            task.setStartDate(startDate);
            task.setStatus("In Progress"); // Set task status to "In Progress"
            task.setLastModifiedDate(ZonedDateTime.now(ZoneOffset.UTC));

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
            task.setCompletionDateTime(ZonedDateTime.now(ZoneOffset.UTC)); // Set completionDateTime to now
            task.setLastModifiedDate(ZonedDateTime.now(ZoneOffset.UTC)); // Update lastModifiedDate

            taskRepository.save(task);
        } else {
            throw new TaskNotFoundException("Task not found with id " + taskId);
        }
    }
}
