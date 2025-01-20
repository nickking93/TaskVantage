package com.taskvantage.backend.service;

import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class RecommendationServiceTest {

    private RecommendationService recommendationService;
    private SentenceEmbeddingClient embeddingClientMock;
    private TaskRepository taskRepositoryMock;

    @BeforeEach
    public void setUp() {
        embeddingClientMock = Mockito.mock(SentenceEmbeddingClient.class);
        taskRepositoryMock = Mockito.mock(TaskRepository.class);
        recommendationService = new RecommendationService(embeddingClientMock, taskRepositoryMock);
    }

    @Nested
    class RecencyWeightTests {
        @Test
        void testCalculateRecencyWeight_RecentTask() {
            Task task = new Task();
            task.setCreationDate(ZonedDateTime.now().minusDays(1));
            task.setLastModifiedDate(ZonedDateTime.now().minusHours(2));

            double weight = recommendationService.calculateRecencyWeight(task);

            assertTrue(weight > 0.95, "Recent task should have weight close to 1.0");
        }

        @Test
        void testCalculateRecencyWeight_OldTask() {
            Task task = new Task();
            task.setCreationDate(ZonedDateTime.now().minusDays(60));
            task.setLastModifiedDate(ZonedDateTime.now().minusDays(60));

            double weight = recommendationService.calculateRecencyWeight(task);

            assertEquals(0.25, weight, 0.01, "Old task should have weight around 0.25");
            assertTrue(weight > 0, "Weight should be positive");
        }

        @Test
        void testCalculateRecencyWeight_UsesMostRecentTimestamp() {
            Task task = new Task();
            task.setCreationDate(ZonedDateTime.now().minusDays(30));
            task.setLastModifiedDate(ZonedDateTime.now().minusDays(15));
            task.setCompletionDateTime(ZonedDateTime.now().minusDays(5));

            double weight = recommendationService.calculateRecencyWeight(task);

            // Weight should be based on completion time (5 days ago)
            assertTrue(weight > 0.8, "Should use most recent timestamp (completion date)");
        }
    }

    @Nested
    class ProfileUpdateTests {
        @Test
        void testUpdateProfile_InitialCreation() {
            Map<String, Double> profile = new HashMap<>();
            double[] embedding = {0.5, 0.3, 0.8};
            double weight = 1.0;

            recommendationService.updateProfile(profile, embedding, weight);

            assertEquals(3, profile.size(), "Profile should have same dimensions as embedding");
            assertEquals(0.5, profile.get("dim_0"), 0.001, "First dimension should match embedding");
            assertEquals(0.3, profile.get("dim_1"), 0.001, "Second dimension should match embedding");
            assertEquals(0.8, profile.get("dim_2"), 0.001, "Third dimension should match embedding");
        }

        @Test
        void testUpdateProfile_WeightedUpdate() {
            Map<String, Double> profile = new HashMap<>();
            double[] firstEmbedding = {1.0, 1.0, 1.0};
            double[] secondEmbedding = {0.0, 0.0, 0.0};

            // First update
            recommendationService.updateProfile(profile, firstEmbedding, 1.0);
            // Second update with 0.5 weight
            recommendationService.updateProfile(profile, secondEmbedding, 0.5);

            // With alpha = 0.7 and weight = 0.5, new value should be:
            // (0.7 * 0.0 * 0.5) + (0.3 * 1.0) = 0.3
            assertEquals(0.3, profile.get("dim_0"), 0.001, "Should apply correct weighted average");
        }
    }

    @Nested
    class SimilarityTests {
        @Test
        void testComputeSimilarityWithProfile_IdenticalVectors() {
            double[] taskEmbedding = {0.5, 0.5, 0.5};
            Map<String, Double> userProfile = new HashMap<>();
            for (int i = 0; i < taskEmbedding.length; i++) {
                userProfile.put("dim_" + i, taskEmbedding[i]);
            }

            double similarity = recommendationService.computeSimilarityWithProfile(taskEmbedding, userProfile);

            assertEquals(1.0, similarity, 0.001, "Identical vectors should have maximum similarity");
        }

        @Test
        void testComputeSimilarityWithProfile_OppositeVectors() {
            double[] taskEmbedding = {0.5, 0.5, 0.5};
            Map<String, Double> userProfile = new HashMap<>();
            for (int i = 0; i < taskEmbedding.length; i++) {
                userProfile.put("dim_" + i, -taskEmbedding[i]);
            }

            double similarity = recommendationService.computeSimilarityWithProfile(taskEmbedding, userProfile);

            assertEquals(0.0, similarity, 0.001, "Opposite vectors should have minimum similarity");
        }

        @Test
        void testComputeSimilarityWithProfile_OrthogonalVectors() {
            double[] taskEmbedding = {1.0, 0.0, 0.0};
            Map<String, Double> userProfile = new HashMap<>();
            userProfile.put("dim_0", 0.0);
            userProfile.put("dim_1", 1.0);
            userProfile.put("dim_2", 0.0);

            double similarity = recommendationService.computeSimilarityWithProfile(taskEmbedding, userProfile);

            assertEquals(0.5, similarity, 0.001, "Orthogonal vectors should have middle similarity");
        }
    }

    @Test
    public void testGetRecommendationsForUser_WithHistory() {
        Long userId = 1L;

        // Create user's historical tasks
        Task task1 = new Task(1L, "API Documentation", "Write API documentation");
        task1.setUserId(userId);
        task1.setStatus("Completed");
        task1.setLastModifiedDate(ZonedDateTime.now().minusDays(1));

        Task task2 = new Task(2L, "Database Schema", "Create database documentation");
        task2.setUserId(userId);
        task2.setStatus("Completed");
        task2.setLastModifiedDate(ZonedDateTime.now().minusDays(2));

        List<Task> userHistory = List.of(task1, task2);

        // Create candidate tasks for recommendations
        Task task3 = new Task(3L, "Update API Docs", "Review and update API documentation");
        Task task4 = new Task(4L, "Code Review", "Review pull requests");
        Task task5 = new Task(5L, "Buy Groceries", "Get weekly groceries");

        List<Task> candidateTasks = List.of(task3, task4, task5);

        // Mock repository responses
        when(taskRepositoryMock.findRecentTasksByUserId(userId)).thenReturn(userHistory);
        when(taskRepositoryMock.findPotentialTasksForUser(userId)).thenReturn(candidateTasks);

        // Mock embeddings for historical tasks
        when(embeddingClientMock.getSentenceEmbedding("API Documentation Write API documentation"))
                .thenReturn(new double[]{0.7, 0.7, 0.1, 0.1});
        when(embeddingClientMock.getSentenceEmbedding("Database Schema Create database documentation"))
                .thenReturn(new double[]{0.6, 0.8, 0.1, 0.1});

        // Mock embeddings for candidate tasks
        when(embeddingClientMock.getSentenceEmbedding("Update API Docs Review and update API documentation"))
                .thenReturn(new double[]{0.75, 0.65, 0.1, 0.1});
        when(embeddingClientMock.getSentenceEmbedding("Code Review Review pull requests"))
                .thenReturn(new double[]{0.4, 0.4, 0.6, 0.4});
        when(embeddingClientMock.getSentenceEmbedding("Buy Groceries Get weekly groceries"))
                .thenReturn(new double[]{0.1, 0.1, 0.7, 0.7});

        // Get recommendations
        List<Task> recommendations = recommendationService.getRecommendationsForUser(userId, 2);

        // Verify results
        assertEquals(2, recommendations.size(), "Should return requested number of recommendations");
        assertEquals("Update API Docs", recommendations.get(0).getTitle(),
                "First recommendation should be most similar to user's documentation history");
        assertEquals("Code Review", recommendations.get(1).getTitle(),
                "Second recommendation should be somewhat related to development work");
    }

    @Test
    public void testGetRecommendationsForUser_NoHistory() {
        Long userId = 1L;

        // Mock empty user history
        when(taskRepositoryMock.findRecentTasksByUserId(userId)).thenReturn(List.of());

        // Mock popular tasks
        Task task1 = new Task(1L, "Get Started", "Complete onboarding tasks");
        Task task2 = new Task(2L, "First Project", "Begin your first project");

        List<Task> popularTasks = List.of(task1, task2);
        when(taskRepositoryMock.findPopularTasks(anyInt())).thenReturn(popularTasks);

        List<Task> recommendations = recommendationService.getRecommendationsForUser(userId, 2);

        assertEquals(2, recommendations.size(), "Should return default recommendations");
        assertEquals("Get Started", recommendations.get(0).getTitle(),
                "Should return onboarding tasks for new users");
    }
}