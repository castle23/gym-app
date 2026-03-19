package com.gym.notification.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {

    /**
     * Initializes Firebase Admin SDK for Cloud Messaging.
     * Handles graceful degradation if credentials are not found (useful for development/testing).
     */
    @Bean
    public void initializeFirebase() {
        try {
            // Check if Firebase app is already initialized
            try {
                FirebaseApp.getInstance();
                log.info("Firebase already initialized, skipping initialization");
                return;
            } catch (IllegalStateException e) {
                // Firebase not initialized yet, proceed with initialization
                log.debug("Firebase not yet initialized, proceeding with initialization");
            }

            // Try to load credentials from environment variable or default location
            String credentialsPath = System.getenv("FIREBASE_CONFIG_PATH");
            if (credentialsPath == null) {
                credentialsPath = System.getenv("FIREBASE_CREDENTIALS");
            }

            if (credentialsPath != null) {
                try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
                    GoogleCredentials credentials =
                            GoogleCredentials.fromStream(serviceAccount);

                    FirebaseApp.initializeApp(
                            FirebaseOptions.builder()
                                    .setCredentials(credentials)
                                    .build()
                    );

                    log.info("Firebase Admin SDK initialized successfully with credentials from: {}", credentialsPath);
                } catch (FileNotFoundException e) {
                    log.warn("Firebase credentials file not found at path: {}. Running without Firebase Admin SDK. " +
                            "This is acceptable for development/testing environments.", credentialsPath);
                }
            } else {
                log.warn("No Firebase credentials path found in environment variables (FIREBASE_CONFIG_PATH or FIREBASE_CREDENTIALS). " +
                        "Attempting to use Application Default Credentials...");
                try {
                    GoogleCredentials credentials =
                            GoogleCredentials.getApplicationDefault();

                    FirebaseApp.initializeApp(
                            FirebaseOptions.builder()
                                    .setCredentials(credentials)
                                    .build()
                    );

                    log.info("Firebase Admin SDK initialized successfully with Application Default Credentials");
                } catch (IOException e) {
                    log.warn("Application Default Credentials not available. Running without Firebase Admin SDK. " +
                            "This is acceptable for development/testing environments.");
                }
            }
        } catch (IOException e) {
            log.error("Error initializing Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    /**
     * Provides FirebaseMessaging instance for sending messages.
     * This bean depends on the initializeFirebase() bean being executed first.
     *
     * @return FirebaseMessaging instance if Firebase is initialized, null otherwise
     */
    @Bean
    @Nullable
    public FirebaseMessaging firebaseMessaging() {
        try {
            FirebaseApp.getInstance();
            log.debug("Firebase is initialized, returning FirebaseMessaging instance");
            return FirebaseMessaging.getInstance();
        } catch (IllegalStateException e) {
            log.warn("Firebase is not initialized. FirebaseMessaging bean will not be functional. " +
                    "Configure Firebase credentials for full functionality.");
            return null;
        }
    }
}
