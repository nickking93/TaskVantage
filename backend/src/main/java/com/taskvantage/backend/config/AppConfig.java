package com.taskvantage.backend.config;

import com.taskvantage.backend.service.SentenceEmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Value("${azure.cognitive.endpoint:#{null}}")
    private String azureEndpoint;

    @Value("${azure.cognitive.apiKey:#{null}}")
    private String azureApiKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SentenceEmbeddingClient sentenceEmbeddingClient() {
        String endpoint = (azureEndpoint != null) ? azureEndpoint : System.getenv("AZURE_COGNITIVE_ENDPOINT");
        String apiKey = (azureApiKey != null) ? azureApiKey : System.getenv("AZURE_COGNITIVE_APIKEY");

        if (endpoint == null || apiKey == null) {
            throw new IllegalArgumentException("Azure Cognitive Services configuration is missing. Ensure 'AZURE_COGNITIVE_ENDPOINT' and 'AZURE_COGNITIVE_APIKEY' are set.");
        }

        return new SentenceEmbeddingClient(endpoint, apiKey);
    }
}