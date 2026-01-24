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

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);
    private final TaskRepository taskRepository;
    private final GoogleCalendarService googleCalendarService;
    private final CustomUserDetailsService userDetailsService;
    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository, GoogleCalendarService googleCalendarService,
                           CustomUserDetailsService userDetailsService, CustomUserDetailsService customUserDetailsService) {
        this.taskRepository = taskRepository;
        this.googleCalendarService = googleCalendarService;
        this.userDetailsService = userDetailsService;
        this.customUserDetailsService = customUserDetailsService;
    }

    private void syncWithGoogleCalendar(Task task, User user, boolean isUpdate) {
        if (user != null && user.getGoogleAccessToken() != null && user.isTaskSyncEnabled()) {
            try {
                if (task.getScheduledStart() != null && task.getDueDate() != null) {
                    if (isUpdate && task.getGoogleCalendarEventId() != null) {
                        // Update existing calendar event
                        googleCalendarService.updateCalendarEvent(
                                user,
                                task.getGoogleCalendarEventId(),
                                task.getTitle(),
                                task.getScheduledStart(),
                                task.getDueDate(),
                                task.isAllDay()
                        );
                    } else {
                        // Create new calendar event
                        String eventId = googleCalendarService.createCalendarEvent(
                                user,
                                task.getTitle(),
                                task.getScheduledStart(),
                                task.getDueDate(),
                                task.isAllDay()
                        );
                        task.setGoogleCalendarEventId(eventId);
                    }
                }
            } catch (GeneralSecurityException | IOException e) {
                logger.error("Failed to sync task with Google Calendar", e);
            }
        }
    }

    private void deleteGoogleCalendarEvent(Task task, User user) {
        if (user != null &&
                user.getGoogleAccessToken() != null &&
                user.isTaskSyncEnabled() &&
                task.getGoogleCalendarEventId() != null) {
            try {
                googleCalendarService.deleteCalendarEvent(user, task.getGoogleCalendarEventId());
            } catch (GeneralSecurityException | IOException e) {
                logger.error("Failed to delete Google Calendar event", e);
            }
        }
    }

    @Override
    public Task addTask(Task task) {
        task.setCreationDate(ZonedDateTime.now(ZoneOffset.UTC));
        task.setLastModifiedDate(ZonedDateTime.now(ZoneOffset.UTC));

        if (task.getDueDate() != null) {
            task.setDueDate(task.getDueDate().withZoneSameInstant(ZoneOffset.UTC));
        }
        if (task.getScheduledStart() != null) {
            task.setScheduledStart(task.getScheduledStart().withZoneSameInstant(ZoneOffset.UTC));
        }

        Task savedTask = taskRepository.save(task);
        User user = customUserDetailsService.findUserById(task.getUserId());
        syncWithGoogleCalendar(savedTask, user, false);

        // Save again if Google Calendar ID was added
        if (savedTask.getGoogleCalendarEventId() != null) {
            savedTask = taskRepository.save(savedTask);
        }

        return savedTask;
    }

    @Override
    public Task updateTask(Task updatedTask) {
        Optional<Task> existingTaskOptional = taskRepository.findById(updatedTask.getId());

        if (existingTaskOptional.isPresent()) {
            Task existingTask = existingTaskOptional.get();
            String originalEventId = existingTask.getGoogleCalendarEventId();

            updateBasicFields(existingTask, updatedTask);
            updateDates(existingTask, updatedTask);
            updateSubtasks(existingTask, updatedTask);
            updateComments(existingTask, updatedTask);
            updateOtherFields(existingTask, updatedTask);

            // Preserve the Google Calendar Event ID
            existingTask.setGoogleCalendarEventId(originalEventId);

            // Save the task first to ensure all fields are updated
            Task savedTask = taskRepository.save(existingTask);

            // Sync with Google Calendar if needed
            User user = customUserDetailsService.findUserById(savedTask.getUserId());
            syncWithGoogleCalendar(savedTask, user, true);

            // Save again if needed (though the Google Calendar ID shouldn't have changed for updates)
            return taskRepository.save(savedTask);
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
                .filter(task -> !Task.isCompletedStatus(task.getStatus()))
                .toList();
    }

    @Override
    public TaskSummary getTaskSummary(Long userId) {
        List<TaskSummary> tasks = taskRepository.findTaskSummariesByUserId(userId);

        long totalTasks = tasks.size();
        long totalSubtasks = tasks.stream().mapToLong(TaskSummary::getTotalSubtasks).sum();
        long pastDeadlineTasks = tasks.stream()
                .filter(task -> task.getDueDate() != null &&
                        task.getDueDate().isBefore(ZonedDateTime.now(ZoneOffset.UTC)) &&
                        !Task.isCompletedStatus(task.getStatus()))
                .count();

        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        long completedTasksThisMonth = tasks.stream()
                .filter(task -> Task.isCompletedStatus(task.getStatus()) &&
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
    public void deleteTask(Long id) {
        Optional<Task> taskOptional = taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            User user = customUserDetailsService.findUserById(task.getUserId());

            // Delete from Google Calendar first
            deleteGoogleCalendarEvent(task, user);

            // Then delete from database
            taskRepository.deleteById(id);
        }
    }

    @Override
    public void startTask(Long taskId, ZonedDateTime startDate) {
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();

            // Set default priority if not set
            if (task.getPriority() == null) {
                task.setPriority(TaskPriority.MEDIUM);
            }

            task.setStartDate(startDate);
            task.setStatus("In Progress");
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
            task.setStatus(Task.STATUS_COMPLETED);
            task.setCompletionDateTime(ZonedDateTime.now(ZoneOffset.UTC));
            task.setLastModifiedDate(ZonedDateTime.now(ZoneOffset.UTC));

            taskRepository.save(task);
        } else {
            throw new TaskNotFoundException("Task not found with id " + taskId);
        }
    }

    private void updateBasicFields(Task existingTask, Task updatedTask) {
        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        Optional.ofNullable(updatedTask.getPriority()).ifPresent(existingTask::setPriority);
        Optional.ofNullable(updatedTask.getStatus()).ifPresent(existingTask::setStatus);
        existingTask.setIsAllDay(updatedTask.isAllDay());
    }

    private void updateDates(Task existingTask, Task updatedTask) {
        Optional.ofNullable(updatedTask.getDueDate())
                .map(date -> date.withZoneSameInstant(ZoneOffset.UTC))
                .ifPresent(existingTask::setDueDate);

        Optional.ofNullable(updatedTask.getScheduledStart())
                .map(date -> date.withZoneSameInstant(ZoneOffset.UTC))
                .ifPresent(existingTask::setScheduledStart);

        Optional.ofNullable(updatedTask.getStartDate())
                .map(date -> date.withZoneSameInstant(ZoneOffset.UTC))
                .ifPresent(existingTask::setStartDate);

        Optional.ofNullable(updatedTask.getCompletionDateTime())
                .map(dateTime -> dateTime.withZoneSameInstant(ZoneOffset.UTC))
                .ifPresent(existingTask::setCompletionDateTime);

        existingTask.setLastModifiedDate(ZonedDateTime.now(ZoneOffset.UTC));

        if (updatedTask.getStartDate() != null && updatedTask.getCompletionDateTime() != null) {
            Duration duration = Duration.between(
                    updatedTask.getStartDate(),
                    updatedTask.getCompletionDateTime().withZoneSameInstant(ZoneOffset.UTC)
            );
            existingTask.setDuration(duration);
        }
    }

    private void updateSubtasks(Task existingTask, Task updatedTask) {
        if (updatedTask.getSubtasks() != null) {
            List<Subtask> existingSubtasks = existingTask.getSubtasks();
            existingSubtasks.removeIf(subtask -> !updatedTask.getSubtasks().contains(subtask));
            for (Subtask newSubtask : updatedTask.getSubtasks()) {
                if (!existingSubtasks.contains(newSubtask)) {
                    existingSubtasks.add(newSubtask);
                    newSubtask.setTask(existingTask);
                }
            }
        }
    }

    private void updateComments(Task existingTask, Task updatedTask) {
        if (updatedTask.getComments() != null) {
            List<Comment> existingComments = existingTask.getComments();
            List<Comment> updatedComments = updatedTask.getComments();
            existingComments.removeIf(comment -> !updatedComments.contains(comment));
            for (Comment newComment : updatedComments) {
                if (!existingComments.contains(newComment)) {
                    existingComments.add(newComment);
                }
            }
        }
    }

    private void updateOtherFields(Task existingTask, Task updatedTask) {
        existingTask.setTags(updatedTask.getTags());
        existingTask.setAttachments(updatedTask.getAttachments());
        existingTask.setReminders(updatedTask.getReminders());
        existingTask.setRecurring(updatedTask.isRecurring());
        existingTask.setNotificationSent(updatedTask.getNotificationSent());
    }
}
