package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.RecommendationResponse;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private static final double MAX_DAY_BOOST = 1.5; // Maximum possible day boost
    private static final double MAX_TIME_BOOST = 1.0; // Maximum possible time boost

    private final TaskRepository taskRepository;
    private final SentenceEmbeddingClient embeddingClient;

    @Autowired
    public RecommendationService(SentenceEmbeddingClient embeddingClient, TaskRepository taskRepository) {
        this.embeddingClient = embeddingClient;
        this.taskRepository = taskRepository;
        logger.info("RecommendationService initialized with embedding client and task repository");
    }

    public List<Task> getRecommendationsForUser(Long userId, int limit) {
        logger.info("Generating recommendations for user: {}, limit: {}", userId, limit);

        // Get user's recent tasks
        List<Task> userRecentTasks = taskRepository.findRecentTasksByUserId(userId);
        logger.debug("Found {} recent tasks for user {}", userRecentTasks.size(), userId);

        if (userRecentTasks.isEmpty()) {
            logger.info("No recent tasks found for user {}. Returning default recommendations.", userId);
            List<Task> defaultRecs = getDefaultRecommendations(limit);
            defaultRecs.forEach(task -> {
                task.setRecommendationScore(1.0f);
                task.setRecommendedBy("POPULAR");
                task.setRecommended(true);
            });
            return defaultRecs;
        }

        try {
            // Get current day of week
            DayOfWeek currentDayOfWeek = ZonedDateTime.now(ZoneOffset.UTC).getDayOfWeek();

            // Find tasks completed on the same day of week
            List<Task> sameDayTasks = userRecentTasks.stream()
                    .filter(task -> task.getCompletionDateTime() != null)
                    .filter(task -> task.getCompletionDateTime().getDayOfWeek() == currentDayOfWeek)
                    .collect(Collectors.toList());

            logger.debug("Found {} tasks completed on {}", sameDayTasks.size(), currentDayOfWeek);

            // Build user's task profile
            Map<String, Double> userTaskProfile = buildUserTaskProfile(sameDayTasks);
            List<Task> candidateTasks = taskRepository.findPotentialTasksForUser(userId);

            // Score and rank tasks with day-of-week boost
            List<Task> recommendations = candidateTasks.stream()
                    .map(task -> {
                        double baseScore = computeTaskScore(task, userTaskProfile);
                        double dayBoost = computeDayOfWeekBoost(task, currentDayOfWeek);
                        double timeBoost = computeTimeOfDayBoost(task);
                        double rawScore = baseScore * dayBoost * timeBoost;
                        double finalScore = normalizeScore(rawScore);

                        task.setRecommendationScore((float)finalScore);
                        task.setRecommended(true);
                        task.setRecommendedBy(dayBoost > 1.0 ? "SAME_DAY" : "SIMILAR_CONTENT");
                        task.setLastRecommendedOn(ZonedDateTime.now(ZoneOffset.UTC));

                        return task;
                    })
                    .sorted((t1, t2) -> Float.compare(t2.getRecommendationScore(), t1.getRecommendationScore()))
                    .limit(limit)
                    .collect(Collectors.toList());

            logger.info("Successfully generated {} recommendations for user {}", recommendations.size(), userId);
            return recommendations;

        } catch (Exception e) {
            logger.error("Error generating recommendations for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    public RecommendationResponse getRecommendedTasksByWeekday(Long userId, int limit) {
        logger.info("Getting weekday recommendations for user: {}, limit: {}", userId, limit);

        RecommendationResponse response = new RecommendationResponse();
        try {
            // Get current day of week
            DayOfWeek currentDayOfWeek = ZonedDateTime.now(ZoneOffset.UTC).getDayOfWeek();

            // Get user's recent tasks completed on this day of week
            List<Task> userRecentTasks = taskRepository.findRecentTasksByUserId(userId);
            List<Task> sameDayTasks = userRecentTasks.stream()
                    .filter(task -> task.getCompletionDateTime() != null)
                    .filter(task -> task.getCompletionDateTime().getDayOfWeek() == currentDayOfWeek)
                    .collect(Collectors.toList());

            if (sameDayTasks.isEmpty()) {
                logger.info("No tasks found for day: {}. Returning default recommendations.", currentDayOfWeek);
                List<Task> defaultRecs = getDefaultRecommendations(limit);
                defaultRecs.forEach(task -> {
                    task.setRecommendationScore(1.0f);
                    task.setRecommendedBy("POPULAR");
                    task.setRecommended(true);
                    task.setLastRecommendedOn(ZonedDateTime.now(ZoneOffset.UTC));
                });
                response.setRecommendations(defaultRecs);
                response.setStatus("success");
                response.setMessage("No historical tasks found for " + currentDayOfWeek);
                return response;
            }

            // Build profile from same-day tasks
            Map<String, Double> userTaskProfile = buildUserTaskProfile(sameDayTasks);
            List<Task> candidateTasks = taskRepository.findPotentialTasksForUser(userId);

            // Score and rank tasks with emphasis on day matching
            List<Task> recommendations = candidateTasks.stream()
                    .map(task -> {
                        double baseScore = computeTaskScore(task, userTaskProfile);
                        double dayBoost = computeDayOfWeekBoost(task, currentDayOfWeek);
                        double timeBoost = computeTimeOfDayBoost(task);
                        double rawScore = baseScore * dayBoost * timeBoost;
                        double finalScore = normalizeScore(rawScore);

                        task.setRecommendationScore((float)finalScore);
                        task.setRecommended(true);
                        task.setRecommendedBy("WEEKDAY_MATCH");
                        task.setLastRecommendedOn(ZonedDateTime.now(ZoneOffset.UTC));

                        return task;
                    })
                    .sorted((t1, t2) -> Float.compare(t2.getRecommendationScore(), t1.getRecommendationScore()))
                    .limit(limit)
                    .collect(Collectors.toList());

            response.setRecommendations(recommendations);
            response.setStatus("success");
            response.setMessage("Recommendations for " + currentDayOfWeek + " generated successfully");

        } catch (Exception e) {
            logger.error("Error generating weekday recommendations: {}", e.getMessage(), e);
            response.setStatus("error");
            response.setMessage("Failed to generate weekday recommendations: " + e.getMessage());
            response.setRecommendations(Collections.emptyList());
        }

        return response;
    }

    private Map<String, Double> buildUserTaskProfile(List<Task> userTasks) {
        logger.debug("Building user profile from {} tasks", userTasks.size());
        Map<String, Double> profile = new HashMap<>();

        for (Task task : userTasks) {
            try {
                String taskContent = task.getTitle() + " " + task.getDescription();
                double[] embedding = embeddingClient.getSentenceEmbedding(taskContent);

                // Calculate combined weight based on recency and completion time
                double recencyWeight = calculateRecencyWeight(task);
                double completionWeight = calculateCompletionTimeWeight(task);
                double finalWeight = recencyWeight * completionWeight;

                logger.trace("Task {}: recencyWeight = {}, completionWeight = {}, finalWeight = {}",
                        task.getId(), recencyWeight, completionWeight, finalWeight);

                updateProfile(profile, embedding, finalWeight);

            } catch (Exception e) {
                logger.warn("Failed to process task {} for profile: {}", task.getId(), e.getMessage());
                // Continue processing other tasks
            }
        }

        return profile;
    }

    double calculateRecencyWeight(Task task) {
        ZonedDateTime mostRecentTimestamp = Stream.of(
                        task.getLastModifiedDate(),
                        task.getCompletionDateTime(),
                        task.getCreationDate()
                )
                .filter(Objects::nonNull)
                .max(ZonedDateTime::compareTo)
                .orElse(task.getCreationDate());

        double ageInDays = Duration.between(mostRecentTimestamp, ZonedDateTime.now(ZoneOffset.UTC)).toDays();

        double halfLife = 30.0; // 30-day half-life for base recency
        double lambda = Math.log(2) / halfLife;
        double weight = Math.exp(-lambda * ageInDays);

        logger.trace("Calculated recency weight {} for task {} (age: {} days)", weight, task.getId(), ageInDays);
        return weight;
    }

    double calculateCompletionTimeWeight(Task task) {
        if (task.getCompletionDateTime() == null) {
            return 1.0;
        }

        // Get completion time of day in minutes
        int completionMinute = task.getCompletionDateTime().getHour() * 60
                + task.getCompletionDateTime().getMinute();

        // Current time of day in minutes
        int currentMinute = ZonedDateTime.now(ZoneOffset.UTC).getHour() * 60
                + ZonedDateTime.now(ZoneOffset.UTC).getMinute();

        // Calculate time difference in minutes
        int timeDiff = Math.abs(completionMinute - currentMinute);

        // Weight drops off based on time difference, with 4-hour half-life
        double halfLife = 240.0; // 4 hours in minutes
        double lambda = Math.log(2) / halfLife;
        return Math.exp(-lambda * timeDiff);
    }

    double computeDayOfWeekBoost(Task task, DayOfWeek targetDay) {
        // Check scheduled start day if available
        if (task.getScheduledStart() != null) {
            return task.getScheduledStart().getDayOfWeek() == targetDay ? 1.5 : 1.0;
        }

        // Check completion day if available
        if (task.getCompletionDateTime() != null) {
            return task.getCompletionDateTime().getDayOfWeek() == targetDay ? 1.3 : 1.0;
        }

        return 1.0; // No boost if no day information available
    }

    double computeTimeOfDayBoost(Task task) {
        if (task.getScheduledStart() != null) {
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            int taskHour = task.getScheduledStart().getHour();
            int currentHour = now.getHour();

            // Higher boost for tasks scheduled around the same time of day
            int hourDiff = Math.abs(taskHour - currentHour);
            return Math.exp(-hourDiff / 4.0); // Exponential decay with 4-hour characteristic time
        }
        return 1.0;
    }

    private double computeTaskScore(Task candidateTask, Map<String, Double> userProfile) {
        logger.debug("Computing score for task {} against user profile", candidateTask.getId());
        try {
            String taskContent = candidateTask.getTitle() + " " + candidateTask.getDescription();
            double[] taskEmbedding = embeddingClient.getSentenceEmbedding(taskContent);
            double similarity = computeSimilarityWithProfile(taskEmbedding, userProfile);

            logger.trace("Task {} similarity score: {}", candidateTask.getId(), similarity);
            return similarity;
        } catch (Exception e) {
            logger.warn("Failed to compute score for task {}: {}", candidateTask.getId(), e.getMessage());
            return 0.0;
        }
    }

    private List<Task> getDefaultRecommendations(int limit) {
        logger.debug("Fetching default recommendations with limit {}", limit);
        try {
            List<Task> popularTasks = taskRepository.findPopularTasks(limit);
            logger.info("Retrieved {} default recommendations", popularTasks.size());
            return popularTasks;
        } catch (Exception e) {
            logger.error("Error fetching default recommendations: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    double computeSimilarityWithProfile(double[] taskEmbedding, Map<String, Double> userProfile) {
        logger.trace("Computing similarity with profile of size {}", userProfile.size());

        // Convert user profile map back to array format
        double[] profileVector = new double[taskEmbedding.length];
        for (int i = 0; i < taskEmbedding.length; i++) {
            profileVector[i] = userProfile.getOrDefault("dim_" + i, 0.0);
        }

        // Calculate cosine similarity
        double similarity = cosineSimilarity(taskEmbedding, profileVector);
        // Normalize to [0,1] range
        double normalizedSimilarity = (similarity + 1.0) / 2.0;

        logger.trace("Computed similarity: {} (normalized: {})", similarity, normalizedSimilarity);
        return normalizedSimilarity;
    }

    void updateProfile(Map<String, Double> profile, double[] embedding, double weight) {
        logger.trace("Updating profile with embedding of length {} and weight {}", embedding.length, weight);

        // Initialize empty profile
        if (profile.isEmpty()) {
            for (int i = 0; i < embedding.length; i++) {
                profile.put("dim_" + i, embedding[i] * weight);
            }
            logger.trace("Initialized new profile with {} dimensions", embedding.length);
            return;
        }

        // Update existing profile
        double alpha = 0.7; // Weight for new value vs historical values
        for (int i = 0; i < embedding.length; i++) {
            String dimKey = "dim_" + i;
            double currentValue = profile.getOrDefault(dimKey, 0.0);
            double newValue = (alpha * embedding[i] * weight) + ((1 - alpha) * currentValue);
            profile.put(dimKey, newValue);
        }
        logger.trace("Updated existing profile dimensions");
    }

    private double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            magnitudeA += vectorA[i] * vectorA[i];
            magnitudeB += vectorB[i] * vectorB[i];
        }

        if (magnitudeA == 0.0 || magnitudeB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB));
    }

    public RecommendationResponse getRecommendedTasks(Long userId, Long taskId, int limit) {
        logger.info("Getting recommended tasks for user: {}, taskId: {}, limit: {}", userId, taskId, limit);

        RecommendationResponse response = new RecommendationResponse();

        try {
            List<Task> recommendations;
            if (taskId != null) {
                Task targetTask = taskRepository.findById(taskId).orElseThrow(() -> {
                    logger.error("Task not found with ID: {}", taskId);
                    return new IllegalArgumentException("Task not found for ID: " + taskId);
                });

                logger.debug("Finding tasks related to task: {}", taskId);
                List<Task> relatedTasks = taskRepository.findRelatedTasks(
                        taskId, targetTask.getTitle(), targetTask.getDescription());

                recommendations = relatedTasks.stream()
                        .map(task -> {
                            double similarity = computeContentSimilarity(targetTask, task);
                            task.setRecommendationScore((float)similarity);
                            task.setRecommended(true);
                            task.setRecommendedBy("TASK_SIMILARITY");
                            task.setLastRecommendedOn(ZonedDateTime.now(ZoneOffset.UTC));
                            return task;
                        })
                        .sorted((t1, t2) -> Float.compare(t2.getRecommendationScore(), t1.getRecommendationScore()))
                        .limit(limit)
                        .collect(Collectors.toList());
            } else {
                logger.debug("Getting user-based recommendations for user: {}", userId);
                recommendations = getRecommendationsForUser(userId, limit);
            }

            response.setRecommendations(recommendations);
            response.setStatus("success");
            response.setMessage("Recommendations fetched successfully.");

            logger.info("Successfully generated recommendations response with {} items", recommendations.size());

        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for recommendations: {}", e.getMessage());
            response.setStatus("error");
            response.setMessage("Invalid request: " + e.getMessage());
            response.setRecommendations(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error generating recommendations: {}", e.getMessage(), e);
            response.setStatus("error");
            response.setMessage("Failed to fetch recommendations: " + e.getMessage());
            response.setRecommendations(Collections.emptyList());
        }

        return response;
    }

    private double computeContentSimilarity(Task task1, Task task2) {
        try {
            String content1 = task1.getTitle() + " " + task1.getDescription();
            String content2 = task2.getTitle() + " " + task2.getDescription();
            double[] embedding1 = embeddingClient.getSentenceEmbedding(content1);
            double[] embedding2 = embeddingClient.getSentenceEmbedding(content2);
            return (cosineSimilarity(embedding1, embedding2) + 1.0) / 2.0; // Normalize to [0,1]
        } catch (Exception e) {
            logger.warn("Failed to compute content similarity: {}", e.getMessage());
            return 0.0;
        }
    }

    private double normalizeScore(double rawScore) {
        double maxPossibleScore = 1.0 * MAX_DAY_BOOST * MAX_TIME_BOOST;
        return Math.min(1.0, Math.max(0.0, rawScore / maxPossibleScore));
    }
}