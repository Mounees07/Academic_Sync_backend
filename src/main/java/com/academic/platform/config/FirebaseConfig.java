package com.academic.platform.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@Configuration
public class FirebaseConfig {

    private static final Logger logger =
            Logger.getLogger(FirebaseConfig.class.getName());

    @PostConstruct
    public void initialize() {

        try {

            // Prevent duplicate initialization
            if (!FirebaseApp.getApps().isEmpty()) {
                logger.info("ℹ️ Firebase already initialized.");
                return;
            }

            FirebaseOptions options;

            // 🔥 1️⃣ Production (Render) — Use Environment Variable
            String firebaseConfig = System.getenv("FIREBASE_CONFIG");

            if (firebaseConfig != null && !firebaseConfig.isBlank()) {

                InputStream serviceAccount =
                        new ByteArrayInputStream(
                                firebaseConfig.getBytes(StandardCharsets.UTF_8)
                        );

                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                logger.info("🔥 Firebase initialized using ENV variable (Production Mode)");

            } else {

                // 🖥 2️⃣ Development — Use Local JSON File
                ClassPathResource resource =
                        new ClassPathResource("firebase-service-account.json");

                if (!resource.exists()) {
                    logger.severe("❌ Firebase config not found in ENV or resources folder.");
                    return;
                }

                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                        .build();

                logger.info("🔥 Firebase initialized using local JSON file (Dev Mode)");
            }

            FirebaseApp.initializeApp(options);

        } catch (Exception e) {
            logger.severe("❌ Firebase initialization failed: " + e.getMessage());
        }
    }
}