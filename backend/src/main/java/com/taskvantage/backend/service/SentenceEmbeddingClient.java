package com.taskvantage.backend.service;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public class SentenceEmbeddingClient {

    private final String endpoint;
    private final String apiKey;

    public SentenceEmbeddingClient(String endpoint, String apiKey) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public double[] getSentenceEmbedding(String sentence) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("api-key", apiKey);

        String requestBody = "{\"input\": \"" + sentence + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseBody.get("data");
        List<Double> embeddingList = (List<Double>) dataList.get(0).get("embedding");

        return embeddingList.stream().mapToDouble(Double::doubleValue).toArray();
    }
}