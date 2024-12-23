package com.taskvantage.backend.service;

import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.AbstractMap;
import java.util.Optional;

@Service
public class RecommendationService {

    @Autowired
    private TaskRepository taskRepository;

    private final SentenceEmbeddingClient embeddingClient;

    // Constructor for production
    public RecommendationService(SentenceEmbeddingClient embeddingClient, TaskRepository taskRepository) {
        this.embeddingClient = embeddingClient;
        this.taskRepository = taskRepository;
    }

    public double computeTextualSimilarity(String text1, String text2) {
        double[] embedding1 = embeddingClient.getSentenceEmbedding(text1);
        double[] embedding2 = embeddingClient.getSentenceEmbedding(text2);

        return cosineSimilarity(embedding1, embedding2);
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

    public List<Task> getRecommendedTasks(Long taskId, int limit) {
        Task targetTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        List<Task> allTasks = taskRepository.findAll();

        // Debug print
        System.out.println("Target task description: " + targetTask.getDescription());

        return allTasks.stream()
                .filter(task -> !task.getId().equals(taskId))
                .map(task -> {
                    double similarity = computeTextualSimilarity(targetTask.getDescription(), task.getDescription());
                    System.out.println("Task " + task.getId() + " similarity: " + similarity); // Debug print
                    return new AbstractMap.SimpleEntry<>(task, similarity);
                })
                .sorted((entry1, entry2) -> Double.compare(entry2.getValue(), entry1.getValue()))
                .limit(limit)
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList());
    }
}