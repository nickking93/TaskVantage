package com.taskvantage.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp initializeFirebase() {
        try {
            // Check if running in production by checking if the environment variable exists
            String firebaseConfig = System.getenv("FIREBASE_CONFIG");

            FirebaseOptions options;

            if (firebaseConfig != null && !firebaseConfig.isEmpty()) {
                // In production, use the environment variable
                byte[] decodedConfig = Base64.getDecoder().decode(firebaseConfig);
                options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(decodedConfig)))
                        .build();
            } else {
                // In local development, use the JSON file
                FileInputStream serviceAccount = new FileInputStream("src/main/resources/taskvantage-c1425-firebase-adminsdk-yc2y8-76ac3c072f.json");
                options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
            }

            return FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }
}