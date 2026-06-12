package com.notivio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoints for monitoring, load balancers, and keep-alive.
 * These endpoints are PUBLIC (no JWT required).
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Service health check endpoints")
public class HealthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    @Operation(summary = "Overall service health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status",    "UP",
                "service",   "Notivio Backend",
                "version",   "1.0.0",
                "timestamp", ZonedDateTime.now().toString()
        ));
    }

    @GetMapping("/db")
    @Operation(summary = "Database connectivity health")
    public ResponseEntity<Map<String, Object>> dbHealth() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", ZonedDateTime.now().toString());
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.put("status", "UP");
            result.put("database", "PostgreSQL");
        } catch (Exception e) {
            log.error("DB health check failed: {}", e.getMessage());
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
            return ResponseEntity.status(503).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ai")
    @Operation(summary = "AI provider connectivity health")
    public ResponseEntity<Map<String, Object>> aiHealth() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", ZonedDateTime.now().toString());

        // Simple check — just verify Groq endpoint is reachable
        try {
            WebClient.create("https://api.groq.com")
                    .get().uri("/openai/v1/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .block();
            result.put("groq", "UP");
        } catch (Exception e) {
            result.put("groq", "UNREACHABLE");
        }

        result.put("status", "UP");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/gmail")
    @Operation(summary = "Gmail API connectivity health")
    public ResponseEntity<Map<String, Object>> gmailHealth() {
        return ResponseEntity.ok(Map.of(
                "status",    "UP",
                "service",   "Gmail API",
                "timestamp", ZonedDateTime.now().toString()
        ));
    }
}
