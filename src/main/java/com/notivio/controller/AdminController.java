package com.notivio.controller;

import com.notivio.entity.ApiAlert;
import com.notivio.entity.ApiKey;
import com.notivio.repository.AiLogRepository;
import com.notivio.repository.ApiAlertRepository;
import com.notivio.repository.ApiUsageLogRepository;
import com.notivio.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin monitoring endpoints for API quota, usage, and system health.
 * Requires JWT authentication (no separate admin role in free tier).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin monitoring and quota management")
public class AdminController {

    private final AiLogRepository aiLogRepository;
    private final ApiUsageLogRepository apiUsageLogRepository;
    private final ApiAlertRepository apiAlertRepository;
    private final UserRepository userRepository;

    @GetMapping("/api-usage")
    @Operation(summary = "Get API usage statistics (last 24h and 30d)")
    public ResponseEntity<Map<String, Object>> getApiUsage() {
        ZonedDateTime last24h  = ZonedDateTime.now().minusHours(24);
        ZonedDateTime last30d  = ZonedDateTime.now().minusDays(30);

        Map<String, Object> usage = new HashMap<>();

        // Groq usage
        usage.put("groq_tokens_24h",
                aiLogRepository.sumTokensByProviderSince(
                        com.notivio.entity.AiLog.AiProvider.GROQ, last24h));
        usage.put("groq_tokens_30d",
                aiLogRepository.sumTokensByProviderSince(
                        com.notivio.entity.AiLog.AiProvider.GROQ, last30d));
        usage.put("groq_failures_24h",
                aiLogRepository.countFailuresByProviderSince(
                        com.notivio.entity.AiLog.AiProvider.GROQ, last24h));

        // OpenRouter usage
        usage.put("openrouter_tokens_24h",
                aiLogRepository.sumTokensByProviderSince(
                        com.notivio.entity.AiLog.AiProvider.OPENROUTER, last24h));
        usage.put("openrouter_failures_24h",
                aiLogRepository.countFailuresByProviderSince(
                        com.notivio.entity.AiLog.AiProvider.OPENROUTER, last24h));

        // Gmail API usage
        usage.put("gmail_requests_24h",
                apiUsageLogRepository.sumRequestsByServiceSince(
                        ApiKey.ApiService.GMAIL, last24h));

        usage.put("timestamp", ZonedDateTime.now().toString());
        return ResponseEntity.ok(usage);
    }

    @GetMapping("/api-health")
    @Operation(summary = "Get current API health and circuit breaker status")
    public ResponseEntity<Map<String, Object>> getApiHealth() {
        List<ApiAlert> activeAlerts = apiAlertRepository
                .findByStatusOrderByCreatedAtDesc(ApiAlert.AlertStatus.ACTIVE);

        return ResponseEntity.ok(Map.of(
                "activeAlerts", activeAlerts.size(),
                "alerts", activeAlerts,
                "timestamp", ZonedDateTime.now().toString()
        ));
    }

    @GetMapping("/quota-status")
    @Operation(summary = "Get quota usage percentage for all APIs")
    public ResponseEntity<Map<String, Object>> getQuotaStatus() {
        ZonedDateTime startOfDay = ZonedDateTime.now().toLocalDate()
                .atStartOfDay(ZonedDateTime.now().getZone());

        Map<String, Object> status = new HashMap<>();

        Long groqDailyTokens = aiLogRepository.sumTokensByProviderSince(
                com.notivio.entity.AiLog.AiProvider.GROQ, startOfDay);

        // Groq free tier: ~14,400 requests/day, ~500K tokens/day
        double groqPct = (groqDailyTokens / 500_000.0) * 100;
        status.put("groq", Map.of(
                "tokensUsedToday", groqDailyTokens,
                "estimatedLimit", 500_000,
                "usagePercentage", Math.min(100, groqPct),
                "status", groqPct < 80 ? "OK" : groqPct < 90 ? "WARNING" : "CRITICAL"
        ));

        status.put("totalUsers", userRepository.count());
        status.put("activeUsers", userRepository.findAllActiveGmailConnectedUsers().size());
        status.put("timestamp", ZonedDateTime.now().toString());

        return ResponseEntity.ok(status);
    }

    @PatchMapping("/alerts/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge an API alert")
    public ResponseEntity<ApiAlert> acknowledgeAlert(@PathVariable java.util.UUID alertId) {
        ApiAlert alert = apiAlertRepository.findById(alertId)
                .orElseThrow(() -> new com.notivio.exception.ResourceNotFoundException(
                        "Alert not found: " + alertId));
        alert.setStatus(ApiAlert.AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(ZonedDateTime.now());
        return ResponseEntity.ok(apiAlertRepository.save(alert));
    }
}
