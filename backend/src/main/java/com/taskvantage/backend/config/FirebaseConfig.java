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

        if (env.acceptsProfiles("prod") || env.acceptsProfiles("dev")) {
            logger.info("Using Firebase configuration from file: {}", firebaseConfigPath);
            InputStream serviceAccount = new FileInputStream(firebaseConfigPath);
            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
        } else {
            throw new IllegalStateException("No valid environment profile found!");
        }

        return FirebaseApp.initializeApp(options);
    }
}