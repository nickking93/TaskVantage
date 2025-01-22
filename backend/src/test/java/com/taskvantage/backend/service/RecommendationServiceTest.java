package com.taskvantage.backend.service;

import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;

import java.time.*;
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
    class DayOfWeekTests {
        @Test
        void testRecommendationsForSameWeekday() {
            Long userId = 1L;
            DayOfWeek today = ZonedDateTime.now(ZoneOffset.UTC).getDayOfWeek();

            // Create historical tasks completed on the same weekday
            Task historicalTask = new Task(1L, "Weekly Report", "Create weekly status report");
            historicalTask.setUserId(userId);
            historicalTask.setStatus("Completed");
            historicalTask.setCompletionDateTime(
                    ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1)
                            .with(today));  // Set to same day last week

            // Create candidate task scheduled for the same weekday
            Task candidateTask = new Task(2L, "Team Meeting", "Weekly team sync");
            candidateTask.setScheduledStart(
                    ZonedDateTime.now(ZoneOffset.UTC).plusDays(7)
                            .with(today));  // Set to same day next week

            when(taskRepositoryMock.findRecentTasksByUserId(userId))
                    .thenReturn(List.of(historicalTask));
            when(taskRepositoryMock.findPotentialTasksForUser(userId))
                    .thenReturn(List.of(candidateTask));

            // Mock embeddings
            when(embeddingClientMock.getSentenceEmbedding(contains("Weekly Report")))
                    .thenReturn(new double[]{0.5, 0.5, 0.5});
            when(embeddingClientMock.getSentenceEmbedding(contains("Team Meeting")))
                    .thenReturn(new double[]{0.5, 0.5, 0.5});

            List<Task> recommendations = recommendationService.getRecommendationsForUser(userId, 1);

            assertEquals(1, recommendations.size());
            assertEquals("Team Meeting", recommendations.get(0).getTitle(),
                    "Should recommend task scheduled for same weekday");
        }

        @Test
        void testDayBoostFactors() {
            Task task = new Task();
            DayOfWeek today = ZonedDateTime.now(ZoneOffset.UTC).getDayOfWeek();

            // Test scheduled start boost
            task.setScheduledStart(ZonedDateTime.now(ZoneOffset.UTC));
            assertEquals(2.0, recommendationService.computeDayOfWeekBoost(task, today),
                    "Should apply 2.0x boost for scheduled tasks on same day");

            // Test completion day boost
            task.setScheduledStart(null);
            task.setCompletionDateTime(ZonedDateTime.now(ZoneOffset.UTC));
            assertEquals(1.8, recommendationService.computeDayOfWeekBoost(task, today),
                    "Should apply 1.8x boost for completed tasks on same day");

            // Test different day
            task.setCompletionDateTime(ZonedDateTime.now(ZoneOffset.UTC).plusDays(1));
            assertEquals(1.0, recommendationService.computeDayOfWeekBoost(task, today),
                    "Should not boost tasks on different days");
        }
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

            assertTrue(weight < 0.1, "Old task should have low weight");
            assertTrue(weight > 0, "Weight should be positive");
        }

        @Test
        void testCalculateRecencyWeight_UsesMostRecentTimestamp() {
            Task task = new Task();
            task.setCreationDate(ZonedDateTime.now().minusDays(30));
            task.setLastModifiedDate(ZonedDateTime.now().minusDays(15));
            task.setCompletionDateTime(ZonedDateTime.now().minusDays(2)); // Changed from 5 to 2 days

            double weight = recommendationService.calculateRecencyWeight(task);

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
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        DayOfWeek today = now.getDayOfWeek();

        // Create user's historical tasks - both on the same weekday
        Task task1 = new Task(1L, "API Documentation", "Write API documentation");
        task1.setUserId(userId);
        task1.setStatus("Completed");
        task1.setCompletionDateTime(now.minusWeeks(1).with(today)); // Same weekday last week
        task1.setLastModifiedDate(task1.getCompletionDateTime());

        Task task2 = new Task(2L, "Database Schema", "Create database documentation");
        task2.setUserId(userId);
        task2.setStatus("Completed");
        task2.setCompletionDateTime(now.minusWeeks(2).with(today)); // Same weekday two weeks ago
        task2.setLastModifiedDate(task2.getCompletionDateTime());

        List<Task> userHistory = List.of(task1, task2);

        // Create candidate tasks for recommendations
        Task task3 = new Task(3L, "Update API Docs", "Review and update API documentation");
        task3.setScheduledStart(now); // Set to today, explicitly same day

        Task task4 = new Task(4L, "Code Review", "Review pull requests");
        task4.setScheduledStart(now.plusDays(1)); // Different day

        Task task5 = new Task(5L, "Buy Groceries", "Get weekly groceries");
        task5.setScheduledStart(now.plusDays(2)); // Different day

        List<Task> candidateTasks = List.of(task3, task4, task5);

        // Mock repository responses
        when(taskRepositoryMock.findRecentTasksByUserId(userId)).thenReturn(userHistory);
        when(taskRepositoryMock.findPotentialTasksForUser(userId)).thenReturn(candidateTasks);

        // Mock embeddings with more distinct values
        when(embeddingClientMock.getSentenceEmbedding(task1.getTitle() + " " + task1.getDescription()))
                .thenReturn(new double[]{0.9, 0.8, 0.1, 0.1}); // High similarity to task3
        when(embeddingClientMock.getSentenceEmbedding(task2.getTitle() + " " + task2.getDescription()))
                .thenReturn(new double[]{0.6, 0.8, 0.1, 0.1});
        when(embeddingClientMock.getSentenceEmbedding(task3.getTitle() + " " + task3.getDescription()))
                .thenReturn(new double[]{0.9, 0.8, 0.1, 0.1}); // High similarity to task1
        when(embeddingClientMock.getSentenceEmbedding(task4.getTitle() + " " + task4.getDescription()))
                .thenReturn(new double[]{0.4, 0.4, 0.6, 0.4});
        when(embeddingClientMock.getSentenceEmbedding(task5.getTitle() + " " + task5.getDescription()))
                .thenReturn(new double[]{0.1, 0.1, 0.7, 0.7});

        // Get recommendations
        List<Task> recommendations = recommendationService.getRecommendationsForUser(userId, 2);

        // Verify results
        assertEquals(2, recommendations.size(), "Should return requested number of recommendations");
        assertEquals("Update API Docs", recommendations.get(0).getTitle(),
                "First recommendation should be most similar and on same day of week");
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