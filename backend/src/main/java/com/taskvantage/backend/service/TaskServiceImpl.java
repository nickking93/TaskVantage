package com.taskvantage.backend.service;
import com.taskvantage.backend.dto.SimilarTaskDTO;
import com.taskvantage.backend.model.User;
import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.exception.TaskNotFoundException;
import com.taskvantage.backend.model.Comment;
import com.taskvantage.backend.model.Subtask;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.model.TaskGroup;
import com.taskvantage.backend.model.TaskPriority;
import com.taskvantage.backend.repository.TaskGroupRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);
    private final TaskRepository taskRepository;
    private final TaskGroupRepository taskGroupRepository;
    private final GoogleCalendarService googleCalendarService;
    private final CustomUserDetailsService userDetailsService;
    private final CustomUserDetailsService customUserDetailsService;
    private final EmbeddingService embeddingService;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository, TaskGroupRepository taskGroupRepository,
                           GoogleCalendarService googleCalendarService,
                           CustomUserDetailsService userDetailsService, CustomUserDetailsService customUserDetailsService,
                           EmbeddingService embeddingService) {
        this.taskRepository = taskRepository;
        this.taskGroupRepository = taskGroupRepository;
        this.googleCalendarService = googleCalendarService;
        this.userDetailsService = userDetailsService;
        this.customUserDetailsService = customUserDetailsService;
        this.embeddingService = embeddingService;
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

        // Generate embedding for the task
        try {
            List<Double> embedding = embeddingService.generateEmbedding(task.getTitle(), task.getDescription());
            if (embedding != null && !embedding.isEmpty()) {
                String embeddingJson = embeddingService.embeddingToJson(embedding);
                task.setEmbedding(embeddingJson);
                logger.info("Generated embedding for new task: {}", task.getTitle());
            }
        } catch (Exception e) {
            logger.error("Failed to generate embedding for task, continuing without it: {}", e.getMessage());
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
        existingTask.setGroupId(updatedTask.getGroupId());
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

    @Override
    public Task updateTaskGroup(Long taskId, Long groupId) {
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        if (taskOptional.isEmpty()) {
            throw new TaskNotFoundException("Task not found with id " + taskId);
        }

        Task task = taskOptional.get();

        // Validate that the groupId belongs to the same user as the task
        if (groupId != null) {
            Optional<TaskGroup> groupOptional = taskGroupRepository.findById(groupId);
            if (groupOptional.isEmpty()) {
                throw new IllegalArgumentException("Group not found with id " + groupId);
            }
            TaskGroup group = groupOptional.get();
            if (!group.getUserId().equals(task.getUserId())) {
                throw new IllegalArgumentException("Group does not belong to the task owner");
            }
        }

        task.setGroupId(groupId);
        task.setLastModifiedDate(ZonedDateTime.now(ZoneOffset.UTC));
        return taskRepository.save(task);
    }

    @Override
    public List<SimilarTaskDTO> findSimilarTasks(Long taskId, Long userId, int limit) {
        logger.info("Finding similar tasks for taskId={}, userId={}, limit={}", taskId, userId, limit);

        // Get the target task
        Optional<Task> targetTaskOpt = taskRepository.findById(taskId);
        if (targetTaskOpt.isEmpty()) {
            logger.warn("Task not found with id {}", taskId);
            return new ArrayList<>();
        }

        Task targetTask = targetTaskOpt.get();
        if (targetTask.getEmbedding() == null || targetTask.getEmbedding().isEmpty()) {
            logger.warn("Target task {} has no embedding", taskId);
            return new ArrayList<>();
        }

        List<Double> targetEmbedding = embeddingService.jsonToEmbedding(targetTask.getEmbedding());
        if (targetEmbedding == null) {
            logger.error("Failed to parse embedding for task {}", taskId);
            return new ArrayList<>();
        }

        // Get all tasks for the user (excluding the target task)
        List<TaskSummary> userTaskSummaries = taskRepository.findTaskSummariesByUserId(userId);
        List<SimilarTaskDTO> similarTasks = new ArrayList<>();
        Set<Long> seenTaskIds = new HashSet<>(); // Track seen task IDs to ensure uniqueness

        for (TaskSummary summary : userTaskSummaries) {
            // Skip the target task itself
            if (summary.getId().equals(taskId)) {
                continue;
            }

            // Skip if we've already processed this task (ensures uniqueness)
            if (seenTaskIds.contains(summary.getId())) {
                continue;
            }

            // Fetch full task to get embedding
            Optional<Task> candidateTaskOpt = taskRepository.findById(summary.getId());
            if (candidateTaskOpt.isEmpty()) {
                continue;
            }

            Task candidateTask = candidateTaskOpt.get();
            if (candidateTask.getEmbedding() == null || candidateTask.getEmbedding().isEmpty()) {
                continue;
            }

            List<Double> candidateEmbedding = embeddingService.jsonToEmbedding(candidateTask.getEmbedding());
            if (candidateEmbedding == null) {
                continue;
            }

            // Calculate similarity
            double similarity = embeddingService.cosineSimilarity(targetEmbedding, candidateEmbedding);

            // Only include if similarity is in the range [0.7, 0.99)
            // This excludes both dissimilar tasks (<70%) and identical tasks (>=99%)
            if (similarity >= 0.7 && similarity < 0.99) {
                String reason = String.format("%.0f%% similar", similarity * 100);
                similarTasks.add(new SimilarTaskDTO(candidateTask, similarity, reason));
                seenTaskIds.add(summary.getId()); // Mark this task as seen
            }
        }

        // Sort by similarity (descending)
        List<SimilarTaskDTO> sortedTasks = similarTasks.stream()
                .sorted(Comparator.comparingDouble(SimilarTaskDTO::getSimilarityScore).reversed())
                .collect(Collectors.toList());

        // Apply diversity filter to ensure results are not too similar to each other
        List<SimilarTaskDTO> diverseTasks = applyDiversityFilter(sortedTasks, limit);

        logger.info("Found {} diverse similar tasks (out of {} candidates and requested limit of {})",
                diverseTasks.size(), sortedTasks.size(), limit);
        return diverseTasks;
    }

    /**
     * Filters similar tasks to ensure diversity - no two tasks in the result are too similar to each other.
     * Uses a greedy approach: keeps tasks that are sufficiently different from already selected tasks.
     */
    private List<SimilarTaskDTO> applyDiversityFilter(List<SimilarTaskDTO> candidates, int limit) {
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        List<SimilarTaskDTO> diverseResults = new ArrayList<>();

        // Always include the first task (most similar to target)
        diverseResults.add(candidates.get(0));

        // Diversity threshold: tasks should be less than 90% similar to each other
        final double DIVERSITY_THRESHOLD = 0.90;

        // Check remaining candidates
        for (int i = 1; i < candidates.size() && diverseResults.size() < limit; i++) {
            SimilarTaskDTO candidate = candidates.get(i);
            boolean isDiverse = true;

            // Check if candidate is sufficiently different from already selected tasks
            for (SimilarTaskDTO selected : diverseResults) {
                List<Double> candidateEmbedding = embeddingService.jsonToEmbedding(
                        candidate.getTask().getEmbedding());
                List<Double> selectedEmbedding = embeddingService.jsonToEmbedding(
                        selected.getTask().getEmbedding());

                if (candidateEmbedding != null && selectedEmbedding != null) {
                    double similarity = embeddingService.cosineSimilarity(
                            candidateEmbedding, selectedEmbedding);

                    // If too similar to an already selected task, skip it
                    if (similarity >= DIVERSITY_THRESHOLD) {
                        isDiverse = false;
                        logger.debug("Skipping task '{}' - too similar ({}%) to already selected task '{}'",
                                candidate.getTask().getTitle(), (int)(similarity * 100),
                                selected.getTask().getTitle());
                        break;
                    }
                }
            }

            if (isDiverse) {
                diverseResults.add(candidate);
            }
        }

        return diverseResults;
    }

    @Override
    public void generateEmbeddingForTask(Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            logger.warn("Task not found with id {}", taskId);
            return;
        }

        Task task = taskOpt.get();
        logger.info("Generating embedding for task: {}", task.getTitle());

        List<Double> embedding = embeddingService.generateEmbedding(task.getTitle(), task.getDescription());
        if (embedding != null && !embedding.isEmpty()) {
            String embeddingJson = embeddingService.embeddingToJson(embedding);
            task.setEmbedding(embeddingJson);
            taskRepository.save(task);
            logger.info("Successfully generated and saved embedding for task {}", taskId);
        } else {
            logger.error("Failed to generate embedding for task {}", taskId);
        }
    }

    @Override
    public int backfillEmbeddingsForUser(Long userId, boolean force) {
        logger.info("Backfilling embeddings for user {} (force={})", userId, force);

        List<TaskSummary> userTaskSummaries = taskRepository.findTaskSummariesByUserId(userId);
        int count = 0;

        for (TaskSummary summary : userTaskSummaries) {
            Optional<Task> taskOpt = taskRepository.findById(summary.getId());
            if (taskOpt.isEmpty()) {
                continue;
            }

            Task task = taskOpt.get();

            // Skip if already has embedding UNLESS force=true
            if (!force && task.getEmbedding() != null && !task.getEmbedding().isEmpty()) {
                continue;
            }

            List<Double> embedding = embeddingService.generateEmbedding(task.getTitle(), task.getDescription());
            if (embedding != null && !embedding.isEmpty()) {
                String embeddingJson = embeddingService.embeddingToJson(embedding);
                task.setEmbedding(embeddingJson);
                taskRepository.save(task);
                count++;
                logger.debug("Generated embedding for task {}", task.getId());
            }
        }

        logger.info("Backfilled {} embeddings for user {}", count, userId);
        return count;
    }

    @Override
    public int backfillAllEmbeddings(boolean force) {
        logger.info("Backfilling embeddings for all tasks (force={})", force);

        List<Task> allTasks = taskRepository.findAll();
        int count = 0;

        for (Task task : allTasks) {
            // Skip if already has embedding UNLESS force=true
            if (!force && task.getEmbedding() != null && !task.getEmbedding().isEmpty()) {
                continue;
            }

            List<Double> embedding = embeddingService.generateEmbedding(task.getTitle(), task.getDescription());
            if (embedding != null && !embedding.isEmpty()) {
                String embeddingJson = embeddingService.embeddingToJson(embedding);
                task.setEmbedding(embeddingJson);
                taskRepository.save(task);
                count++;

                if (count % 10 == 0) {
                    logger.info("Processed {} tasks so far...", count);
                }
            }
        }

        logger.info("Backfilled {} embeddings for all tasks", count);
        return count;
    }
}
