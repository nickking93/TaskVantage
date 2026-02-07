package com.taskvantage.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating text embeddings using configurable embedding providers.
 * Supports both Ollama (local) and OpenAI-compatible APIs.
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${embedding.service.url}")
    private String embeddingServiceUrl;

    @Value("${embedding.service.model}")
    private String embeddingModel;

    @Value("${embedding.service.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EmbeddingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate embedding for a text string.
     * Combines title and description for better semantic representation.
     */
    public List<Double> generateEmbedding(String title, String description) {
        String text = buildTextForEmbedding(title, description);
        return generateEmbedding(text);
    }

    /**
     * Generate embedding for a single text string.
     */
    public List<Double> generateEmbedding(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                logger.warn("Attempted to generate embedding for empty text");
                return null;
            }

            // Detect which API we're using based on URL
            boolean isOllama = embeddingServiceUrl.contains("ollama") || embeddingServiceUrl.contains("11434");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);

            if (isOllama) {
                // Ollama format
                requestBody.put("prompt", text);
            } else {
                // OpenAI format
                requestBody.put("input", text);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Add API key if provided (for OpenAI/Anthropic)
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.debug("Generating embedding for text: {} (length: {})",
                text.substring(0, Math.min(50, text.length())), text.length());

            ResponseEntity<String> response = restTemplate.exchange(
                embeddingServiceUrl,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Double> embedding = parseEmbedding(root, isOllama);

                if (embedding != null && !embedding.isEmpty()) {
                    logger.info("Successfully generated embedding with {} dimensions", embedding.size());
                    return embedding;
                } else {
                    logger.error("Failed to parse embedding from response");
                    return null;
                }
            } else {
                logger.error("Embedding service returned error: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            logger.error("Error generating embedding: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse embedding from JSON response based on API format.
     */
    private List<Double> parseEmbedding(JsonNode root, boolean isOllama) {
        try {
            List<Double> embedding = new ArrayList<>();

            if (isOllama) {
                // Ollama format: { "embedding": [0.1, 0.2, ...] }
                JsonNode embeddingNode = root.get("embedding");
                if (embeddingNode != null && embeddingNode.isArray()) {
                    for (JsonNode value : embeddingNode) {
                        embedding.add(value.asDouble());
                    }
                }
            } else {
                // OpenAI format: { "data": [{ "embedding": [0.1, 0.2, ...] }] }
                JsonNode dataNode = root.get("data");
                if (dataNode != null && dataNode.isArray() && dataNode.size() > 0) {
                    JsonNode embeddingNode = dataNode.get(0).get("embedding");
                    if (embeddingNode != null && embeddingNode.isArray()) {
                        for (JsonNode value : embeddingNode) {
                            embedding.add(value.asDouble());
                        }
                    }
                }
            }

            return embedding;
        } catch (Exception e) {
            logger.error("Error parsing embedding: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build text for embedding by combining title and description.
     */
    private String buildTextForEmbedding(String title, String description) {
        StringBuilder text = new StringBuilder();

        if (title != null && !title.trim().isEmpty()) {
            text.append(title.trim());
        }

        if (description != null && !description.trim().isEmpty()) {
            if (text.length() > 0) {
                text.append(". ");
            }
            text.append(description.trim());
        }

        return text.toString();
    }

    /**
     * Calculate cosine similarity between two embedding vectors.
     * Returns a value between 0 and 1, where 1 means identical.
     */
    public double cosineSimilarity(List<Double> embedding1, List<Double> embedding2) {
        if (embedding1 == null || embedding2 == null ||
            embedding1.isEmpty() || embedding2.isEmpty() ||
            embedding1.size() != embedding2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.size(); i++) {
            dotProduct += embedding1.get(i) * embedding2.get(i);
            norm1 += embedding1.get(i) * embedding1.get(i);
            norm2 += embedding2.get(i) * embedding2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Convert embedding list to JSON string for storage.
     */
    public String embeddingToJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            logger.error("Error converting embedding to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse embedding from JSON string.
     */
    public List<Double> jsonToEmbedding(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
        } catch (Exception e) {
            logger.error("Error parsing embedding from JSON: {}", e.getMessage());
            return null;
        }
    }
}
