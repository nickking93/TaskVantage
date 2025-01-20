package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.RecommendationResponse;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
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

    @Autowired
    private TaskRepository taskRepository;
    private final SentenceEmbeddingClient embeddingClient;

    public RecommendationService(SentenceEmbeddingClient embeddingClient, TaskRepository taskRepository) {
        this.embeddingClient = embeddingClient;
        this.taskRepository = taskRepository;
    }

    public List<Task> getRecommendationsForUser(Long userId, int limit) {
        // 1. Get user's recent tasks
        List<Task> userRecentTasks = taskRepository.findRecentTasksByUserId(userId);
        if (userRecentTasks.isEmpty()) {
            // If user has no history, return popular or general tasks
            return getDefaultRecommendations(limit);
        }

        // 2. Build user's task profile from their recent tasks
        Map<String, Double> userTaskProfile = buildUserTaskProfile(userRecentTasks);

        // 3. Get potential tasks to recommend
        List<Task> candidateTasks = taskRepository.findPotentialTasksForUser(userId);

        // 4. Score and rank candidate tasks
        return candidateTasks.stream()
                .map(task -> new AbstractMap.SimpleEntry<>(task, computeTaskScore(task, userTaskProfile)))
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(limit)
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList());
    }

    private Map<String, Double> buildUserTaskProfile(List<Task> userTasks) {
        Map<String, Double> profile = new HashMap<>();

        for (Task task : userTasks) {
            // Combine title and description for better context
            String taskContent = task.getTitle() + " " + task.getDescription();
            double[] embedding = embeddingClient.getSentenceEmbedding(taskContent);

            // Aggregate embeddings with weights based on recency
            // More recent tasks have higher weight
            updateProfile(profile, embedding, calculateRecencyWeight(task));
        }

        return profile;
    }

    double calculateRecencyWeight(Task task) {
        // Get the most recent timestamp between lastModifiedDate and completionDateTime
        // If neither exists, fall back to creationDate
        ZonedDateTime mostRecentTimestamp = Stream.of(
                        task.getLastModifiedDate(),
                        task.getCompletionDateTime(),
                        task.getCreationDate()  // This is never null as it's set on creation
                )
                .filter(Objects::nonNull)
                .max(ZonedDateTime::compareTo)
                .orElse(task.getCreationDate());

        // Calculate the age of the task in days
        double ageInDays = Duration.between(mostRecentTimestamp, ZonedDateTime.now(ZoneOffset.UTC))
                .toDays();

        // Use exponential decay formula: weight = e^(-lambda * t)
        // lambda = ln(2)/halfLife: we'll set halfLife to 30 days
        double halfLife = 30.0; // tasks older than 30 days will have less than 0.5 weight
        double lambda = Math.log(2) / halfLife;

        return Math.exp(-lambda * ageInDays);
    }

    private double computeTaskScore(Task candidateTask, Map<String, Double> userProfile) {
        String taskContent = candidateTask.getTitle() + " " + candidateTask.getDescription();
        double[] taskEmbedding = embeddingClient.getSentenceEmbedding(taskContent);

        // Compare task embedding with user profile
        return computeSimilarityWithProfile(taskEmbedding, userProfile);
    }

    private List<Task> getDefaultRecommendations(int limit) {
        // Return popular or starter tasks when user has no history
        return taskRepository.findPopularTasks(limit);
    }

    double computeSimilarityWithProfile(double[] taskEmbedding, Map<String, Double> userProfile) {
        // Convert user profile map back to array format to match task embedding
        double[] profileVector = new double[taskEmbedding.length];
        for (int i = 0; i < taskEmbedding.length; i++) {
            profileVector[i] = userProfile.getOrDefault("dim_" + i, 0.0);
        }

        // Calculate cosine similarity between task embedding and profile
        double similarity = cosineSimilarity(taskEmbedding, profileVector);

        // Normalize similarity to range [0,1] in case of negative values
        // Cosine similarity range is [-1,1], we shift to [0,1]
        return (similarity + 1.0) / 2.0;
    }

    void updateProfile(Map<String, Double> profile, double[] embedding, double weight) {
        // If profile is empty, initialize it with the first embedding
        if (profile.isEmpty()) {
            for (int i = 0; i < embedding.length; i++) {
                profile.put("dim_" + i, embedding[i] * weight);
            }
            return;
        }

        // Update each dimension of the profile with weighted average
        for (int i = 0; i < embedding.length; i++) {
            String dimKey = "dim_" + i;
            // Get current value or 0.0 if dimension doesn't exist
            double currentValue = profile.getOrDefault(dimKey, 0.0);
            // Update with weighted contribution of new embedding
            // Using exponential moving average formula
            double alpha = 0.7; // Weight for new value vs historical values
            double newValue = (alpha * embedding[i] * weight) + ((1 - alpha) * currentValue);
            profile.put(dimKey, newValue);
        }
    }

    // Keep the original cosineSimilarity method as it's still useful
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
        RecommendationResponse response = new RecommendationResponse();
        List<Task> recommendations;

        try {
            if (taskId != null) {
                // If taskId is provided, find recommendations related to the specific task
                Task targetTask = taskRepository.findById(taskId).orElseThrow(() ->
                        new IllegalArgumentException("Task not found for ID: " + taskId));
                // Use the findRelatedTasks with the correct signature
                recommendations = taskRepository.findRelatedTasks(taskId, targetTask.getTitle(), targetTask.getDescription());
            } else {
                // Otherwise, get user-based recommendations
                recommendations = getRecommendationsForUser(userId, limit);
            }

            // Set all fields of the response
            response.setRecommendations(recommendations);
            response.setStatus("success");
            response.setMessage("Recommendations fetched successfully.");
        } catch (IllegalArgumentException e) {
            response.setStatus("error");
            response.setMessage("Invalid request: " + e.getMessage());
            response.setRecommendations(Collections.emptyList());
        } catch (Exception e) {
            response.setStatus("error");
            response.setMessage("Failed to fetch recommendations: " + e.getMessage());
            response.setRecommendations(Collections.emptyList());
        }

        return response;
    }
}