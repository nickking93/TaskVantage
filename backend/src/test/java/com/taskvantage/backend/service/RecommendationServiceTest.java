package com.taskvantage.backend.service;

import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.ZonedDateTime;
import java.util.List;
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