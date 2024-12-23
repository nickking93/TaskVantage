package com.taskvantage.backend.config;

import com.taskvantage.backend.service.SentenceEmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Value("${azure.cognitive.endpoint}")
    private String azureEndpoint;

    @Value("${azure.cognitive.apiKey}")
    private String azureApiKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SentenceEmbeddingClient sentenceEmbeddingClient() {
        return new SentenceEmbeddingClient(azureEndpoint, azureApiKey);
    }
}