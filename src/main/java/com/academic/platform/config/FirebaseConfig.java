package com.academic.platform.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.logging.Logger;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = Logger.getLogger(FirebaseConfig.class.getName());

    @PostConstruct
    public void initialize() {
        try {
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");

            if (resource.exists()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    logger.info("✅ Firebase Admin SDK initialized successfully.");
                } else {
                    logger.info("ℹ️ Firebase App already exists.");
                }
            } else {
                logger.severe(
                        "❌ CRITICAL: firebase-service-account.json NOT FOUND in src/main/resources. All authenticated requests will fail with 403.");
            }
        } catch (IOException e) {
            logger.severe("❌ Error initializing Firebase Admin SDK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
