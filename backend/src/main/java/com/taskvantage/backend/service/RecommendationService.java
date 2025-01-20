package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.RecommendationResponse;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

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
            // Build user's task profile
            Map<String, Double> userTaskProfile = buildUserTaskProfile(userRecentTasks);
            logger.debug("Built user profile with {} dimensions", userTaskProfile.size());

            // Get potential tasks
            List<Task> candidateTasks = taskRepository.findPotentialTasksForUser(userId);
            logger.debug("Found {} candidate tasks for recommendations", candidateTasks.size());

            // Score and rank tasks
            List<Task> recommendations = candidateTasks.stream()
                    .map(task -> {
                        double score = computeTaskScore(task, userTaskProfile);
                        logger.trace("Task {}: score = {}", task.getId(), score);
                        return new AbstractMap.SimpleEntry<>(task, score);
                    })
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(limit)
                    .map(AbstractMap.SimpleEntry::getKey)
                    .collect(Collectors.toList());

            logger.info("Successfully generated {} recommendations for user {}", recommendations.size(), userId);
            return recommendations;

        } catch (Exception e) {
            logger.error("Error generating recommendations for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    private Map<String, Double> buildUserTaskProfile(List<Task> userTasks) {
        logger.debug("Building user profile from {} tasks", userTasks.size());
        Map<String, Double> profile = new HashMap<>();

        for (Task task : userTasks) {
            try {
                String taskContent = task.getTitle() + " " + task.getDescription();
                double[] embedding = embeddingClient.getSentenceEmbedding(taskContent);
                double weight = calculateRecencyWeight(task);

                logger.trace("Task {}: recency weight = {}", task.getId(), weight);
                updateProfile(profile, embedding, weight);

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

        double halfLife = 30.0;
        double lambda = Math.log(2) / halfLife;
        double weight = Math.exp(-lambda * ageInDays);

        logger.trace("Calculated recency weight {} for task {} (age: {} days)", weight, task.getId(), ageInDays);

        return weight;
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
                recommendations = taskRepository.findRelatedTasks(
                        taskId, targetTask.getTitle(), targetTask.getDescription());
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
}