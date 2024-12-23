package com.taskvantage.backend.service;

import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private TaskRepository taskRepository;

    private final SentenceEmbeddingClient embeddingClient;

    // Constructor for production
    public RecommendationService(SentenceEmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
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
}