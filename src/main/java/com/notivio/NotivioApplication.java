package com.notivio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notivio - AI-Powered Gmail Reminder and Deadline Detection System
 * Main entry point for the Spring Boot application.
 *
 * Features:
 * - Gmail OAuth2 integration for reading emails
 * - AI-powered deadline and task extraction (Groq + OpenRouter)
 * - Automatic reminder scheduling and push notifications (FCM)
 * - Google Calendar integration
 * - 24/7 uptime with keep-alive mechanism
 * - Admin monitoring for API quotas and health
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
public class NotivioApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotivioApplication.class, args);
    }
}
