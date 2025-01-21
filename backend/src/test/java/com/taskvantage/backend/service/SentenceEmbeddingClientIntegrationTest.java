package com.taskvantage.backend.service;

import com.taskvantage.backend.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SentenceEmbeddingClientIntegrationTest {

    // Move the static block here, before Spring context initialization
    static {
        if (System.getenv("JWT_SECRET") == null) {
            String encodedSecret = Base64.getEncoder().encodeToString("test-jwt-secret-key-for-testing-purposes-only".getBytes());
            System.setProperty("JWT_SECRET", encodedSecret);
        }
    }

    private static final Logger logger = Logger.getLogger(SentenceEmbeddingClientIntegrationTest.class.getName());

    @TestConfiguration
    static class TestConfig {
        @Bean
        public SentenceEmbeddingClient sentenceEmbeddingClient(
                @Value("${azure.cognitive.endpoint}") String endpoint,
                @Value("${azure.cognitive.apiKey}") String apiKey) {
            return new SentenceEmbeddingClient(endpoint, apiKey);
        }

        @Bean
        @Primary
        public PasswordEncoder passwordEncoder() {
            return NoOpPasswordEncoder.getInstance();
        }
    }

    @MockBean
    private AppConfig appConfig;

    @Autowired
    private SentenceEmbeddingClient embeddingClient;

    @Test
    void testGetEmbedding_ValidInput() {
        // Test with a simple sentence
        String testSentence = "Create a new project plan";
        double[] embedding = embeddingClient.getSentenceEmbedding(testSentence);

        assertNotNull(embedding, "Embedding should not be null");
        assertTrue(embedding.length > 0, "Embedding should have dimensions");

        // Azure's text embeddings typically have 1536 dimensions
        assertEquals(1536, embedding.length, "Azure's text embedding should have 1536 dimensions");
    }

    @Test
    void testGetEmbedding_SimilarSentences() {
        // Test that similar sentences have similar embeddings
        String sentence1 = "Schedule team meeting";
        String sentence2 = "Arrange team sync";

        double[] embedding1 = embeddingClient.getSentenceEmbedding(sentence1);
        double[] embedding2 = embeddingClient.getSentenceEmbedding(sentence2);

        // Log embeddings for debugging
        logger.info("Embedding for sentence1: " + Arrays.toString(embedding1));
        logger.info("Embedding for sentence2: " + Arrays.toString(embedding2));

        // Calculate cosine similarity
        double similarity = calculateCosineSimilarity(embedding1, embedding2);

        // Similar sentences should have high similarity (> 0.8)
        assertTrue(similarity > 0.8, "Similar sentences should have high embedding similarity");
    }

    @Test
    void testGetEmbedding_DifferentSentences() {
        // Test that different sentences have lower similarity than similar ones
        String similar1 = "Schedule team meeting";
        String similar2 = "Arrange team sync";
        String different1 = "Take my child to daycare";
        String different2 = "Write a book about snails";

        double[] similarEmbedding1 = embeddingClient.getSentenceEmbedding(similar1);
        double[] similarEmbedding2 = embeddingClient.getSentenceEmbedding(similar2);
        double[] differentEmbedding1 = embeddingClient.getSentenceEmbedding(different1);
        double[] differentEmbedding2 = embeddingClient.getSentenceEmbedding(different2);

        double similarPairSimilarity = calculateCosineSimilarity(similarEmbedding1, similarEmbedding2);
        double differentPairSimilarity = calculateCosineSimilarity(differentEmbedding1, differentEmbedding2);

        // Log similarities for debugging
        logger.info("Similar pair similarity: " + similarPairSimilarity);
        logger.info("Different pair similarity: " + differentPairSimilarity);

        // Solution 1: Adjusted threshold
        assertTrue(differentPairSimilarity < 0.8, "Different sentences should have lower embedding similarity");

        // Solution 4: Relative comparison
        assertTrue(differentPairSimilarity < similarPairSimilarity,
                "Different sentences should have lower similarity than similar sentences");
    }

    @Test
    void testGetEmbedding_EdgeCases() {
        // Test empty string
        double[] emptyEmbedding = embeddingClient.getSentenceEmbedding("");
        assertNotNull(emptyEmbedding, "Empty string should still return an embedding");

        // Test very long text
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("This is a very long text that needs to be processed. ");
        }
        double[] longEmbedding = embeddingClient.getSentenceEmbedding(longText.toString());
        assertNotNull(longEmbedding, "Long text should return an embedding");
        assertEquals(1536, longEmbedding.length, "Long text embedding should have correct dimensions");
    }

    @Test
    void testGetEmbedding_SpecialCharacters() {
        // Test text with special characters and formatting
        String specialText = "Special chars: !@#$%^&*()_+ \n\t Tab and newline";
        double[] specialEmbedding = embeddingClient.getSentenceEmbedding(specialText);

        assertNotNull(specialEmbedding, "Text with special characters should return an embedding");
        assertEquals(1536, specialEmbedding.length, "Special character embedding should have correct dimensions");
    }

    private double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            logger.warning("One of the vectors has zero magnitude, returning similarity as 0.0");
            return 0.0;
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        logger.info("Cosine similarity calculated: " + similarity);
        return similarity;
    }
}