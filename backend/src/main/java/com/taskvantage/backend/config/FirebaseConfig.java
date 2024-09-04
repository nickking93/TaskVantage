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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${FIREBASE_CONFIG_PATH:classpath:taskvantage-c1425-firebase-adminsdk-yc2y8-9b453309eb.json}")
    private String firebaseConfigPath;

    private final Environment env;
    private final ResourceLoader resourceLoader;

    public FirebaseConfig(Environment env, ResourceLoader resourceLoader) {
        this.env = env;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Check if FirebaseApp is already initialized
        if (FirebaseApp.getApps().isEmpty()) {
            logger.info("Active profiles: {}", (Object) env.getActiveProfiles());

            // Load the Firebase configuration file from the classpath
            Resource resource = resourceLoader.getResource(firebaseConfigPath);
            if (!resource.exists()) {
                throw new IllegalStateException("Firebase configuration file not found at: " + firebaseConfigPath);
            }

            logger.info("Using Firebase configuration from file: {}", firebaseConfigPath);

            try (InputStream serviceAccount = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                return FirebaseApp.initializeApp(options);
            } catch (IOException e) {
                logger.error("Failed to initialize Firebase: {}", e.getMessage());
                throw e;  // Re-throwing the exception after logging
            }
        } else {
            // If FirebaseApp is already initialized, return the existing instance
            return FirebaseApp.getInstance();
        }
    }
}