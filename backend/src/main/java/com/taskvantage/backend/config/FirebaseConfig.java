package com.taskvantage.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${FIREBASE_CONFIG_PATH:}")
    private String firebaseConfigPath;

    private final Environment env;

    public FirebaseConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        FirebaseOptions options;

        logger.info("Active profiles: {}", (Object) env.getActiveProfiles());

        // Ensure the Firebase configuration path is provided
        if (firebaseConfigPath == null || firebaseConfigPath.isEmpty()) {
            throw new IllegalStateException("FIREBASE_CONFIG_PATH is not set or is empty.");
        }

        // Ensure the file exists at the provided path
        if (!Files.exists(Paths.get(firebaseConfigPath))) {
            throw new IllegalStateException("Firebase configuration file not found at: " + firebaseConfigPath);
        }

        logger.info("Using Firebase configuration from file: {}", firebaseConfigPath);

        try (InputStream serviceAccount = new FileInputStream(firebaseConfigPath)) {
            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
        } catch (IOException e) {
            logger.error("Failed to initialize Firebase: {}", e.getMessage());
            throw e;  // Re-throwing the exception after logging
        }

        return FirebaseApp.initializeApp(options);
    }
}