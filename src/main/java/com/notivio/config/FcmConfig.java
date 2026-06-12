package com.notivio.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Initializes Firebase Admin SDK for FCM push notifications.
 * Supports both file path and Base64-encoded service account JSON.
 */
@Slf4j
@Configuration
public class FcmConfig {

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${firebase.service-account-base64:}")
    private String serviceAccountBase64;

    @PostConstruct
    public void initializeFirebase() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                GoogleCredentials credentials = loadCredentials();
                if (credentials == null) {
                    log.warn("Firebase credentials not configured — push notifications disabled");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");

            } catch (IOException e) {
                log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
            }
        }
    }

    private GoogleCredentials loadCredentials() throws IOException {
        // Prefer Base64 env var (cloud-friendly, no file needed)
        if (StringUtils.hasText(serviceAccountBase64)) {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountBase64);
            return GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
        }
        // Fallback to file path
        if (StringUtils.hasText(serviceAccountPath)) {
            return GoogleCredentials.fromStream(new FileInputStream(serviceAccountPath));
        }
        return null;
    }
}
