package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.RecommendationResponse;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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

    private double calculateRecencyWeight(Task task) {
        // Implement recency weighting logic
        // More recent tasks should have higher weights
        return 1.0; // Placeholder - implement actual weighting logic
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

    private double computeSimilarityWithProfile(double[] taskEmbedding, Map<String, Double> userProfile) {
        // Implement similarity computation between task and user profile
        // This could use various similarity metrics depending on your needs
        return 0.0; // Placeholder - implement actual similarity computation
    }

    private void updateProfile(Map<String, Double> profile, double[] embedding, double weight) {
        // Update the user profile with new task embedding
        // This could involve various aggregation strategies
        // Placeholder implementation
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