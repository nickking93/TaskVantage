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

@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private static final double MAX_DAY_BOOST = 2.0; // Increased for more impact
    private static final double BASE_DAY_BOOST = 1.0;
    private static final int FREQUENCY_THRESHOLD_HIGH = 5; // Tasks done 5+ times are considered highly frequent
    private static final int FREQUENCY_THRESHOLD_MEDIUM = 3; // Tasks done 3-4 times are considered moderately frequent

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
            return getDefaultRecommendations(limit);
        }

        try {
            DayOfWeek currentDayOfWeek = ZonedDateTime.now(ZoneOffset.UTC).getDayOfWeek();
            List<Task> candidateTasks = taskRepository.findPotentialTasksForUser(userId);

            // Group tasks by title and description to avoid duplicates
            Map<String, List<Task>> groupedTasks = candidateTasks.stream()
                    .collect(Collectors.groupingBy(task -> task.getTitle().toLowerCase() + "::" + task.getDescription().toLowerCase()));

            // Score and rank tasks
            List<Task> recommendations = groupedTasks.values().stream()
                    .map(group -> {
                        Task representativeTask = group.get(0);
                        double dayBoost = computeDayOfWeekBoost(representativeTask, currentDayOfWeek);
                        double recencyWeight = calculateRecencyWeight(representativeTask);
                        double frequencyWeight = calculateFrequencyWeight(representativeTask, userRecentTasks);

                        // Calculate base scores for each component
                        double dayComponent = (dayBoost > BASE_DAY_BOOST) ? 0.4 : 0.1;
                        double frequencyComponent = Math.min(0.3, (frequencyWeight - 1.0) * 0.1);
                        double recencyComponent = Math.min(0.15, recencyWeight * 0.5);

                        // Combine scores with priority tiers
                        double finalScore;
                        String recommendationReason;

                        if (dayBoost > BASE_DAY_BOOST && frequencyWeight > 2.0) {
                            // Highest tier: Same day AND frequently repeated (0.85-0.99)
                            finalScore = 0.85 + (frequencyComponent * 0.1) + (recencyComponent * 0.05);
                            recommendationReason = "High Priority: Regular task for " + currentDayOfWeek;
                        } else if (dayBoost > BASE_DAY_BOOST) {
                            // Second tier: Same day but not frequent (0.65-0.89)
                            finalScore = 0.65 + (frequencyComponent * 0.15) + (recencyComponent * 0.1);
                            recommendationReason = "Medium Priority: Scheduled for " + currentDayOfWeek;
                        } else if (frequencyWeight > 2.0) {
                            // Third tier: Frequent but different day (0.45-0.79)
                            finalScore = 0.45 + (frequencyComponent * 0.2) + (recencyComponent * 0.15);
                            recommendationReason = "Medium Priority: Frequently Repeated Task";
                        } else {
                            // Lowest tier: Neither same day nor frequent (0.25-0.74)
                            finalScore = 0.25 + (frequencyComponent * 0.3) + (recencyComponent * 0.2);
                            recommendationReason = "Regular Priority Task";
                        }

                        // Ensure score is between 0 and 0.99
                        finalScore = Math.min(0.99, Math.max(0.01, finalScore));

                        // Log detailed scoring breakdown
                        logger.debug(
                                "Score breakdown for task '{}': dayBoost={}, frequencyWeight={}, recencyWeight={}, " +
                                        "finalScore={}, tier={}",
                                representativeTask.getTitle(),
                                dayBoost,
                                frequencyWeight,
                                recencyWeight,
                                finalScore,
                                recommendationReason
                        );

                        // Update task metadata
                        representativeTask.setRecommendationScore((float) finalScore);
                        representativeTask.setRecommended(true);
                        representativeTask.setRecommendedBy(recommendationReason);
                        representativeTask.setLastRecommendedOn(ZonedDateTime.now(ZoneOffset.UTC));

                        return representativeTask;
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

    double computeDayOfWeekBoost(Task task, DayOfWeek targetDay) {
        // Check scheduled start day if available
        if (task.getScheduledStart() != null &&
                task.getScheduledStart().getDayOfWeek() == targetDay) {
            return MAX_DAY_BOOST; // Highest boost for explicitly scheduled tasks
        }

        // Check completion history
        if (task.getCompletionDateTime() != null &&
                task.getCompletionDateTime().getDayOfWeek() == targetDay) {
            return MAX_DAY_BOOST - 0.2; // Slightly lower boost for historical pattern
        }

        // Default case - no day-based boost
        return BASE_DAY_BOOST;
    }

    double calculateFrequencyWeight(Task task, List<Task> userHistory) {
        // Count exact matches (same title and description)
        long exactMatches = userHistory.stream()
                .filter(historyTask ->
                        historyTask.getTitle().equalsIgnoreCase(task.getTitle()) &&
                                historyTask.getDescription().equalsIgnoreCase(task.getDescription()))
                .count();

        // Apply tiered frequency weighting
        if (exactMatches >= FREQUENCY_THRESHOLD_HIGH) {
            return 3.0; // High frequency weight
        } else if (exactMatches >= FREQUENCY_THRESHOLD_MEDIUM) {
            return 2.0; // Medium frequency weight
        } else {
            return 1.0 + (exactMatches * 0.2); // Linear scaling for lower frequencies
        }
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

        // Exponential decay with 14-day half-life (more aggressive than before)
        double halfLife = 14.0;
        double lambda = Math.log(2) / halfLife;
        double weight = Math.exp(-lambda * ageInDays);

        logger.trace("Calculated recency weight {} for task {} (age: {} days)", weight, task.getId(), ageInDays);
        return weight;
    }
    public RecommendationResponse getRecommendedTasksByWeekday(Long userId, int limit) {
        logger.info("Getting weekday recommendations for user: {}, limit: {}", userId, limit);

        RecommendationResponse response = new RecommendationResponse();
        try {
            // Use getRecommendationsForUser with the user's ID and limit
            List<Task> recommendations = getRecommendationsForUser(userId, limit);

            // Prepare response
            response.setRecommendations(recommendations);
            response.setStatus("success");
            response.setMessage("Recommendations for the current weekday fetched successfully.");
            logger.info("Weekday recommendations generated successfully for user: {}", userId);
        } catch (Exception e) {
            logger.error("Error generating weekday recommendations: {}", e.getMessage(), e);
            response.setStatus("error");
            response.setMessage("Failed to generate weekday recommendations: " + e.getMessage());
            response.setRecommendations(Collections.emptyList());
        }

        return response;
    }

    protected Map<String, Double> buildUserTaskProfile(List<Task> userTasks) {
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

    protected double computeSimilarityWithProfile(double[] taskEmbedding, Map<String, Double> userProfile) {
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

    protected void updateProfile(Map<String, Double> profile, double[] embedding, double weight) {
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

    protected double cosineSimilarity(double[] vectorA, double[] vectorB) {
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
                // Task-based recommendations
                Task targetTask = taskRepository.findById(taskId).orElseThrow(() -> {
                    logger.error("Task not found with ID: {}", taskId);
                    return new IllegalArgumentException("Task not found for ID: " + taskId);
                });

                List<Task> relatedTasks = taskRepository.findRelatedTasks(
                        taskId, userId, targetTask.getTitle(), targetTask.getDescription());

                recommendations = relatedTasks.stream()
                        .map(task -> {
                            double similarityScore = computeContentSimilarity(targetTask, task);
                            task.setRecommendationScore((float) similarityScore);
                            task.setRecommended(true);
                            task.setRecommendedBy(similarityScore > 0.7 ? "HIGHLY_SIMILAR" : "RELATED_TASK");
                            task.setLastRecommendedOn(ZonedDateTime.now(ZoneOffset.UTC));
                            return task;
                        })
                        .sorted((t1, t2) -> Float.compare(t2.getRecommendationScore(), t1.getRecommendationScore()))
                        .limit(limit)
                        .collect(Collectors.toList());
            } else {
                recommendations = getRecommendationsForUser(userId, limit);
            }

            response.setRecommendations(recommendations);
            response.setStatus("success");
            response.setMessage("Recommendations fetched successfully.");
            logger.info("Successfully generated recommendations response with {} items", recommendations.size());

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
}