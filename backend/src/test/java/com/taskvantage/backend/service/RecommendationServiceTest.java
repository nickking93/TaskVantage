package com.taskvantage.backend.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

public class RecommendationServiceTest {

    private static RecommendationService recommendationService;
    private static SentenceEmbeddingClient embeddingClientMock;

    @BeforeAll
    public static void setUp() {
        // Mock SentenceEmbeddingClient
        embeddingClientMock = Mockito.mock(SentenceEmbeddingClient.class);
        recommendationService = new RecommendationService(embeddingClientMock);
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
}