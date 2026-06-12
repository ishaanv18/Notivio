package com.notivio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notivio.entity.ExtractedTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parses and validates AI JSON responses into structured task objects.
 * Handles malformed JSON gracefully with fallback extraction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * Parse AI response JSON into an extraction result.
     */
    public ExtractionResult parseExtractionResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return ExtractionResult.notRelevant();
        }

        try {
            // Extract JSON from response (AI sometimes adds markdown)
            String cleanJson = extractJson(aiResponse);
            JsonNode root = objectMapper.readTree(cleanJson);

            // Check relevance first
            boolean isRelevant = root.path("isRelevant").asBoolean(true);
            if (!isRelevant) {
                return ExtractionResult.notRelevant();
            }

            ExtractionResult result = new ExtractionResult();
            result.setIsRelevant(true);
            result.setTitle(getTextSafely(root, "title", "Untitled Task"));
            result.setDescription(getTextSafely(root, "description", null));
            result.setTaskType(parseTaskType(root.path("taskType").asText()));
            result.setPriority(parsePriority(root.path("priority").asText()));
            result.setDeadline(parseDateTime(root.path("deadline").asText(null)));
            result.setEventDate(parseDateTime(root.path("eventDate").asText(null)));
            result.setLocation(getTextSafely(root, "location", null));
            result.setOrganizer(getTextSafely(root, "organizer", null));
            result.setCourseName(getTextSafely(root, "courseName", null));
            result.setAiSummary(getTextSafely(root, "summary", null));

            double confidence = root.path("confidence").asDouble(50.0);
            result.setAiConfidence(BigDecimal.valueOf(Math.min(100.0, Math.max(0.0, confidence))));

            return result;

        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}. Raw: {}", e.getMessage(),
                    aiResponse.substring(0, Math.min(200, aiResponse.length())));
            return ExtractionResult.error("Parse error: " + e.getMessage());
        }
    }

    /** Strip markdown code blocks if AI wraps JSON in them */
    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        // Find first { and last }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private ZonedDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || dateStr.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            // Try ISO format with time
            return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                // Try without timezone
                LocalDateTime ldt = LocalDateTime.parse(dateStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                return ldt.atZone(ZoneId.of("UTC"));
            } catch (DateTimeParseException e2) {
                try {
                    // Date only
                    LocalDateTime ldt = LocalDateTime.parse(dateStr + "T23:59:00",
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                    return ldt.atZone(ZoneId.of("UTC"));
                } catch (Exception e3) {
                    log.warn("Could not parse date: {}", dateStr);
                    return null;
                }
            }
        }
    }

    private ExtractedTask.TaskType parseTaskType(String value) {
        if (value == null) return ExtractedTask.TaskType.OTHER;
        try {
            return ExtractedTask.TaskType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ExtractedTask.TaskType.OTHER;
        }
    }

    private ExtractedTask.Priority parsePriority(String value) {
        if (value == null) return ExtractedTask.Priority.MEDIUM;
        try {
            return ExtractedTask.Priority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ExtractedTask.Priority.MEDIUM;
        }
    }

    private String getTextSafely(JsonNode node, String field, String defaultValue) {
        JsonNode child = node.path(field);
        if (child.isMissingNode() || child.isNull()) return defaultValue;
        String val = child.asText();
        return (val.isBlank() || val.equalsIgnoreCase("null")) ? defaultValue : val;
    }

    // ── Result DTO ─────────────────────────────────────────────

    @lombok.Data
    public static class ExtractionResult {
        private Boolean isRelevant = false;
        private String title;
        private String description;
        private ExtractedTask.TaskType taskType = ExtractedTask.TaskType.OTHER;
        private ExtractedTask.Priority priority = ExtractedTask.Priority.MEDIUM;
        private ZonedDateTime deadline;
        private ZonedDateTime eventDate;
        private String location;
        private String organizer;
        private String courseName;
        private String aiSummary;
        private BigDecimal aiConfidence;
        private String errorMessage;

        public static ExtractionResult notRelevant() {
            ExtractionResult r = new ExtractionResult();
            r.setIsRelevant(false);
            return r;
        }

        public static ExtractionResult error(String msg) {
            ExtractionResult r = new ExtractionResult();
            r.setIsRelevant(false);
            r.setErrorMessage(msg);
            return r;
        }
    }
}
