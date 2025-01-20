package com.taskvantage.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class SentenceEmbeddingClient {

    private static final Logger logger = LoggerFactory.getLogger(SentenceEmbeddingClient.class);

    private final String endpoint;
    private final String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SentenceEmbeddingClient(String endpoint, String apiKey) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public double[] getSentenceEmbedding(String sentence) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("api-key", apiKey);

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(Map.of("input", sentence));
            logger.debug("Serialized request body: {}", requestBody);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize request body: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize request body", e);
        }

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        logger.info("Sending request to embedding endpoint: {}", endpoint);
        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
        } catch (Exception e) {
            logger.error("Error during REST call to embedding service: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve sentence embedding", e);
        }

        logger.debug("Received response from embedding service: {}", response.getBody());
        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null || !responseBody.containsKey("data")) {
            logger.error("Invalid response structure: {}", responseBody);
            throw new RuntimeException("Invalid response from embedding service");
        }

        List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseBody.get("data");
        if (dataList == null || dataList.isEmpty()) {
            logger.error("No data found in response: {}", responseBody);
            throw new RuntimeException("No data returned from embedding service");
        }

        List<Double> embeddingList;
        try {
            embeddingList = (List<Double>) dataList.get(0).get("embedding");
        } catch (Exception e) {
            logger.error("Failed to extract embedding from response: {}", e.getMessage());
            throw new RuntimeException("Failed to extract embedding from response", e);
        }

        if (embeddingList == null || embeddingList.isEmpty()) {
            logger.error("Empty embedding received: {}", responseBody);
            throw new RuntimeException("Empty embedding returned from embedding service");
        }

        logger.info("Successfully retrieved embedding with {} dimensions", embeddingList.size());
        return embeddingList.stream().mapToDouble(Double::doubleValue).toArray();
    }
}