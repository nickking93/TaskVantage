package com.taskvantage.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_CONFIG:}")
    private String firebaseConfig;

    private final Environment env;

    public FirebaseConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        FirebaseOptions options;

        if (env.acceptsProfiles("prod")) {
            // Production environment: FIREBASE_CONFIG contains JSON content
            InputStream serviceAccount = new ByteArrayInputStream(firebaseConfig.getBytes());
            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
        } else {
            // Development environment: FIREBASE_CONFIG contains the path to the JSON file
            InputStream serviceAccount = new FileInputStream(firebaseConfig);
            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
        }

        return FirebaseApp.initializeApp(options);
    }
}
