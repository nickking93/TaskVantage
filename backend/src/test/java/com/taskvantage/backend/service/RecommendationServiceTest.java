package com.taskvantage.backend.service;

import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

public class RecommendationServiceTest {

    private static RecommendationService recommendationService;
    private static SentenceEmbeddingClient embeddingClientMock;
    private static TaskRepository taskRepositoryMock;

    @BeforeAll
    public static void setUp() {
        // Mock SentenceEmbeddingClient and TaskRepository
        embeddingClientMock = Mockito.mock(SentenceEmbeddingClient.class);
        taskRepositoryMock = Mockito.mock(TaskRepository.class);

        // Inject mocks into RecommendationService
        recommendationService = new RecommendationService(embeddingClientMock, taskRepositoryMock);
    }

    @Test
    public void testComputeTextualSimilarity_SimilarTexts() {
        String text1 = "Complete the project documentation";
        String text2 = "Finalize the documentation for the project";

        // Similar embeddings
        when(embeddingClientMock.getSentenceEmbedding(text1))
                .thenReturn(new double[]{0.1, 0.2, 0.3, 0.4});
        when(embeddingClientMock.getSentenceEmbedding(text2))
                .thenReturn(new double[]{0.1, 0.2, 0.29, 0.41});

        double similarity = recommendationService.computeTextualSimilarity(text1, text2);

        System.out.println("Computed similarity for similar texts: " + similarity);

        assertTrue(similarity > 0.8 && similarity <= 1.0, "Expected similarity above 0.8 for similar texts");
    }

    @Test
    public void testComputeTextualSimilarity_NonSimilarTexts() {
        String text1 = "The weather is sunny.";
        String text2 = "Quantum computing is fascinating.";

        // Modified embeddings to produce a small non-zero similarity
        when(embeddingClientMock.getSentenceEmbedding(text1))
                .thenReturn(new double[]{0.8, 0.1, 0.1, 0.1});
        when(embeddingClientMock.getSentenceEmbedding(text2))
                .thenReturn(new double[]{0.1, 0.8, 0.1, 0.1});

        double similarity = recommendationService.computeTextualSimilarity(text1, text2);

        System.out.println("Computed similarity for non-similar texts: " + similarity);

        assertTrue(similarity > 0.0 && similarity < 0.3,
                "Expected similarity between 0.0 and 0.3 for dissimilar texts");
    }

    @Test
    public void testGetRecommendedTasks() {
        Task targetTask = new Task(1L, "Complete the project", "Finish writing all the documentation");
        Task task1 = new Task(2L, "Write documentation", "Start the documentation draft");
        Task task2 = new Task(3L, "Plan the project", "Create a timeline for the project");
        Task task3 = new Task(4L, "Unrelated task", "Buy groceries");

        List<Task> mockTasks = List.of(targetTask, task1, task2, task3);

        when(taskRepositoryMock.findById(1L)).thenReturn(Optional.of(targetTask));
        when(taskRepositoryMock.findAll()).thenReturn(mockTasks);

        // Target vector pointing mostly in x and y directions (documentation-related)
        when(embeddingClientMock.getSentenceEmbedding(targetTask.getDescription()))
                .thenReturn(new double[]{0.7, 0.7, 0.1, 0.1});

        // Very similar vector (also documentation-related)
        when(embeddingClientMock.getSentenceEmbedding(task1.getDescription()))
                .thenReturn(new double[]{0.65, 0.75, 0.1, 0.1});  // Should give ~0.99 similarity

        // Somewhat different vector (project-related but not documentation)
        when(embeddingClientMock.getSentenceEmbedding(task2.getDescription()))
                .thenReturn(new double[]{0.4, 0.4, 0.6, 0.4});    // Should give ~0.75 similarity

        // Very different vector (groceries - completely different topic)
        when(embeddingClientMock.getSentenceEmbedding(task3.getDescription()))
                .thenReturn(new double[]{0.1, 0.1, 0.7, 0.7});    // Should give ~0.3 similarity

        List<Task> recommendedTasks = recommendationService.getRecommendedTasks(1L, 2);

        // Debug print all similarities
        System.out.println("Similarities:");
        double sim1 = recommendationService.computeTextualSimilarity(targetTask.getDescription(), task1.getDescription());
        double sim2 = recommendationService.computeTextualSimilarity(targetTask.getDescription(), task2.getDescription());
        double sim3 = recommendationService.computeTextualSimilarity(targetTask.getDescription(), task3.getDescription());
        System.out.printf("Task1 (documentation): %.3f%n", sim1);
        System.out.printf("Task2 (project): %.3f%n", sim2);
        System.out.printf("Task3 (groceries): %.3f%n", sim3);

        // Basic size assertion
        assertEquals(2, recommendedTasks.size());

        // Verify order
        assertEquals(task1, recommendedTasks.get(0));
        assertEquals(task2, recommendedTasks.get(1));

        // Verify similarity ranges
        assertTrue(sim1 > 0.95, "Documentation tasks should be very similar");
        assertTrue(sim2 > 0.6 && sim2 < 0.9, "Project planning should be moderately similar");
        assertTrue(sim3 < 0.5, "Groceries should be very different");
    }
}