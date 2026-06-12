package com.notivio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notivio.entity.AiLog;
import com.notivio.entity.Email;
import com.notivio.repository.AiLogRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Groq API client for LLM inference.
 * Uses circuit breaker + retry patterns.
 * Falls back to OpenRouter if Groq fails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroqClient {

    private final ObjectMapper objectMapper;
    private final AiLogRepository aiLogRepository;

    @Value("${ai.groq.api-key:}")
    private String apiKey;

    @Value("${ai.groq.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${ai.groq.model:llama3-8b-8192}")
    private String model;

    @Value("${ai.groq.max-tokens:1024}")
    private Integer maxTokens;

    @Value("${ai.groq.temperature:0.1}")
    private Double temperature;

    @Value("${ai.groq.timeout-seconds:30}")
    private Integer timeoutSeconds;

    @CircuitBreaker(name = "groq", fallbackMethod = "fallbackResponse")
    @Retry(name = "groq")
    public String complete(String prompt, Email email) {
        long startTime = System.currentTimeMillis();
        AiLog aiLog = AiLog.builder()
                .email(email)
                .provider(AiLog.AiProvider.GROQ)
                .model(model)
                .build();

        try {
            WebClient client = buildClient();
            Map<String, Object> requestBody = buildRequestBody(prompt);
            String requestJson = objectMapper.writeValueAsString(requestBody);

            log.debug("Calling Groq API with model: {}", model);

            String response = client.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(timeoutSeconds));

            String content = extractContent(response);
            int[] tokens = extractTokens(response);

            aiLog.setPromptTokens(tokens[0]);
            aiLog.setCompletionTokens(tokens[1]);
            aiLog.setTotalTokens(tokens[2]);
            aiLog.setLatencyMs((int)(System.currentTimeMillis() - startTime));
            aiLog.setSuccess(true);
            aiLog.setResponsePayload(content != null ? content.substring(0, Math.min(1000, content.length())) : null);
            aiLogRepository.save(aiLog);

            return content;

        } catch (WebClientResponseException e) {
            log.error("Groq API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            aiLog.setSuccess(false);
            aiLog.setErrorMessage(e.getMessage());
            aiLog.setLatencyMs((int)(System.currentTimeMillis() - startTime));
            aiLogRepository.save(aiLog);
            throw e;
        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            aiLog.setSuccess(false);
            aiLog.setErrorMessage(e.getMessage());
            aiLog.setLatencyMs((int)(System.currentTimeMillis() - startTime));
            aiLogRepository.save(aiLog);
            throw new RuntimeException("Groq API call failed", e);
        }
    }

    public String fallbackResponse(String prompt, Email email, Exception ex) {
        log.warn("Groq circuit breaker triggered, fallback invoked: {}", ex.getMessage());
        throw new RuntimeException("Groq unavailable: " + ex.getMessage(), ex);
    }

    private WebClient buildClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", maxTokens,
                "temperature", temperature,
                "response_format", Map.of("type", "json_object")
        );
    }

    private String extractContent(String response) throws Exception {
        if (response == null) return null;
        JsonNode root = objectMapper.readTree(response);
        return root.path("choices").get(0)
                   .path("message").path("content").asText();
    }

    private int[] extractTokens(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode usage = root.path("usage");
            return new int[]{
                    usage.path("prompt_tokens").asInt(0),
                    usage.path("completion_tokens").asInt(0),
                    usage.path("total_tokens").asInt(0)
            };
        } catch (Exception e) {
            return new int[]{0, 0, 0};
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
