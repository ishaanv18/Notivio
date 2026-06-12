package com.notivio.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Builds optimized prompts for AI email analysis.
 * Prompts are engineered for high accuracy on educational/work deadline extraction.
 */
@Slf4j
@Component
public class AiPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are an intelligent assistant that extracts deadline and task information from emails.
            Your job is to analyze email content and extract structured task information.
            
            IMPORTANT RULES:
            1. Extract tasks related to: assignments, exams, interviews, meetings,
               internships, events, submissions, deadlines, and reminders.
            2. Ignore purely promotional/marketing emails (no dates, no actions) — return {"isRelevant": false}.
            3. Always return valid JSON matching the exact schema provided.
            4. For deadlines, use ISO 8601: "YYYY-MM-DDTHH:mm:ss"
               If no time is given, assume 23:59:00. If no year, use current/next year.
            5. confidence: 0-100 based on how certain you are about the extraction.
            6. priority: HIGH if deadline/event within 48 hours or urgent, MEDIUM if within 1 week, LOW otherwise.
            
            INTERVIEW-SPECIFIC RULES (critical):
            - taskType = "INTERVIEW" for any interview email
            - eventDate = the scheduled interview time (when it happens)
            - deadline = same as eventDate (or prep deadline if explicitly mentioned)
            - location field MUST specify the mode:
              * on-site / in-person / face-to-face / at office / campus → "On-site: [address if given]"
              * zoom / google meet / ms teams / video call / virtual → "Virtual: [link if given]"
              * telephonic / phone → "Telephonic"
              * not specified → "TBD"
            - organizer = company name
            - title = include the interview round (e.g. "Round 1 - Technical Interview at [Company]")
            """;


    private static final String EXTRACTION_SCHEMA = """
            {
              "isRelevant": boolean,
              "title": "string (concise task title)",
              "taskType": "ASSIGNMENT|EXAM|INTERVIEW|MEETING|EVENT|INTERNSHIP|PLACEMENT|SUBMISSION|DEADLINE|GENERAL_REMINDER|OTHER",
              "priority": "HIGH|MEDIUM|LOW",
              "deadline": "YYYY-MM-DDTHH:mm:ss or null",
              "eventDate": "YYYY-MM-DDTHH:mm:ss or null (for events that happen at a specific time)",
              "description": "string (brief description, max 200 chars)",
              "location": "string or null",
              "organizer": "string or null",
              "courseName": "string or null (for academic tasks)",
              "confidence": 0-100,
              "summary": "string (one sentence AI summary of the task)"
            }
            """;

    /**
     * Build the full prompt for email deadline extraction.
     */
    public String buildExtractionPrompt(String emailSubject, String emailBody, String senderEmail) {
        String truncatedBody = truncate(emailBody, 3000); // Avoid token overflow

        return String.format("""
                %s
                
                Analyze the following email and extract task/deadline information.
                Return ONLY a valid JSON object matching this schema exactly:
                %s
                
                EMAIL DETAILS:
                From: %s
                Subject: %s
                Body:
                %s
                
                Return only the JSON object, no markdown, no explanation.
                If the email is not task-related (promotional, social, marketing), 
                return {"isRelevant": false} and nothing else.
                
                Current date for reference: %s
                """,
                SYSTEM_PROMPT,
                EXTRACTION_SCHEMA,
                senderEmail != null ? senderEmail : "unknown",
                emailSubject != null ? emailSubject : "(no subject)",
                truncatedBody,
                java.time.LocalDate.now().toString()
        );
    }

    /**
     * Build prompt for generating a daily digest summary.
     */
    public String buildDigestPrompt(String tasksJson) {
        return String.format("""
                You are a helpful assistant creating a daily task digest.
                
                Below are the user's upcoming tasks and deadlines in JSON format:
                %s
                
                Generate a concise, friendly daily digest message that:
                1. Highlights the most urgent tasks (due within 24 hours)
                2. Lists upcoming deadlines for the next 7 days
                3. Provides motivational encouragement
                4. Is formatted in clear, readable text (not JSON)
                5. Is under 300 words
                
                Return only the digest text, no JSON.
                """,
                tasksJson
        );
    }

    /**
     * Build prompt for duplicate detection.
     */
    public String buildDuplicateCheckPrompt(String newTask, String existingTasks) {
        return String.format("""
                Determine if the new task is a duplicate of any existing tasks.
                
                New task: %s
                
                Existing tasks: %s
                
                Return JSON: {"isDuplicate": boolean, "duplicateOf": "task title or null"}
                """,
                newTask, existingTasks
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}
