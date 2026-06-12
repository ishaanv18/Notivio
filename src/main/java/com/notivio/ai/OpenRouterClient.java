package com.notivio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notivio.entity.AiLog;
import com.notivio.entity.Email;
import com.notivio.repository.AiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OpenRouter API client — fallback when Groq is unavailable.
 * Supports free models like meta-llama/llama-3-8b-instruct:free.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenRouterClient {

    private final ObjectMapper objectMapper;
    private final AiLogRepository aiLogRepository;

    @Value("${ai.openrouter.api-key:}")
    private String apiKey;

    @Value("${ai.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${ai.openrouter.model:meta-llama/llama-3-8b-instruct:free}")
    private String model;

    @Value("${ai.openrouter.max-tokens:1024}")
    private Integer maxTokens;

    @Value("${ai.openrouter.temperature:0.1}")
    private Double temperature;

    @Value("${ai.openrouter.timeout-seconds:45}")
    private Integer timeoutSeconds;

    public String complete(String prompt, Email email) {
        long startTime = System.currentTimeMillis();
        AiLog aiLog = AiLog.builder()
                .email(email)
                .provider(AiLog.AiProvider.OPENROUTER)
                .model(model)
                .build();

        try {
            org.springframework.web.reactive.function.client.WebClient client =
                    org.springframework.web.reactive.function.client.WebClient.builder()
                            .baseUrl(baseUrl)
                            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .defaultHeader("HTTP-Referer", "https://notivio.app")
                            .defaultHeader("X-Title", "Notivio")
                            .build();

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", maxTokens,
                    "temperature", temperature
            );

            log.debug("Calling OpenRouter API with model: {}", model);

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
            aiLogRepository.save(aiLog);

            return content;

        } catch (Exception e) {
            log.error("OpenRouter API call failed: {}", e.getMessage());
            aiLog.setSuccess(false);
            aiLog.setErrorMessage(e.getMessage());
            aiLog.setLatencyMs((int)(System.currentTimeMillis() - startTime));
            aiLogRepository.save(aiLog);
            throw new RuntimeException("OpenRouter API call failed", e);
        }
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
