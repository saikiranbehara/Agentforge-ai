package com.agentforge.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class AiClientService {

    private final ObjectMapper objectMapper;
    private final MockAiProvider mockAiProvider;
    private volatile HttpClient httpClient;

    @Getter
    @Setter
    private String geminiApiKey;

    @Getter
    @Setter
    private String openaiApiKey;

    @Getter
    @Setter
    private String activeProvider; // GEMINI, OPENAI, MOCK

    @Getter
    @Setter
    private String activeModel;

    @Getter
    @Setter
    private boolean forceMockMode;

    public AiClientService(
            ObjectMapper objectMapper,
            MockAiProvider mockAiProvider,
            @Value("${gemini.api.key:}") String geminiApiKey,
            @Value("${openai.api.key:}") String openaiApiKey,
            @Value("${ai.provider:AUTO}") String aiProvider,
            @Value("${ai.model:gemini-1.5-pro}") String aiModel,
            @Value("${ai.mock-mode:false}") boolean forceMockMode) {
        
        this.objectMapper = objectMapper;
        this.mockAiProvider = mockAiProvider;

        // Check system env overrides
        String envGemini = System.getenv("GEMINI_API_KEY");
        String envOpenAi = System.getenv("OPENAI_API_KEY");
        this.geminiApiKey = (envGemini != null && !envGemini.isBlank()) ? envGemini : geminiApiKey;
        this.openaiApiKey = (envOpenAi != null && !envOpenAi.isBlank()) ? envOpenAi : openaiApiKey;

        this.forceMockMode = forceMockMode;
        this.activeModel = aiModel;

        if (this.forceMockMode) {
            this.activeProvider = "MOCK";
        } else if (this.geminiApiKey != null && !this.geminiApiKey.isBlank()) {
            this.activeProvider = "GEMINI";
        } else if (this.openaiApiKey != null && !this.openaiApiKey.isBlank()) {
            this.activeProvider = "OPENAI";
        } else {
            this.activeProvider = "MOCK";
        }
    }

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(15))
                            .build();
                }
            }
        }
        return httpClient;
    }

    public boolean isMockExecution() {
        return "MOCK".equalsIgnoreCase(activeProvider) || forceMockMode
                || (("GEMINI".equalsIgnoreCase(activeProvider) && (geminiApiKey == null || geminiApiKey.isBlank()))
                && ("OPENAI".equalsIgnoreCase(activeProvider) && (openaiApiKey == null || openaiApiKey.isBlank())));
    }

    public String generateText(String systemPrompt, String userPrompt) {
        if (isMockExecution()) {
            log.info("[MOCK PROVIDER] Simulating AI generation for prompt: {}", truncate(userPrompt, 100));
            return mockAiProvider.generateMockText(userPrompt);
        }

        try {
            if ("GEMINI".equalsIgnoreCase(activeProvider) && geminiApiKey != null && !geminiApiKey.isBlank()) {
                return callGemini(systemPrompt, userPrompt);
            } else if ("OPENAI".equalsIgnoreCase(activeProvider) && openaiApiKey != null && !openaiApiKey.isBlank()) {
                return callOpenAi(systemPrompt, userPrompt);
            }
        } catch (Exception e) {
            log.warn("Real AI provider call failed (falling back to Mock Provider): {}", e.getMessage());
        }

        return mockAiProvider.generateMockText(userPrompt);
    }

    public <T> T generateStructuredJson(String systemPrompt, String userPrompt, Class<T> clazz) {
        String jsonPrompt = systemPrompt + "\n\nCRITICAL: Respond ONLY with a valid, clean JSON object matching the requested schema. Do NOT wrap with markdown ticks like ```json ... ``` or include any conversational filler.";

        if (!isMockExecution()) {
            try {
                String raw = generateText(jsonPrompt, userPrompt);
                String cleanJson = cleanJsonOutput(raw);
                return objectMapper.readValue(cleanJson, clazz);
            } catch (Exception e) {
                log.warn("Failed to parse LLM response into {}. Falling back to domain-specific Mock generator. Error: {}", clazz.getSimpleName(), e.getMessage());
            }
        }

        // Domain-specific smart mock provider
        return mockAiProvider.generateStructuredMock(userPrompt, clazz);
    }

    private String callGemini(String systemPrompt, String userPrompt) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" 
                + (activeModel != null && !activeModel.isBlank() ? activeModel : "gemini-1.5-pro") 
                + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> body = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", systemPrompt + "\n\nUser Request:\n" + userPrompt);
        content.put("parts", List.of(part));
        body.put("contents", List.of(content));

        String requestJson = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API returned status " + response.statusCode() + ": " + response.body());
        }

        // Parse Gemini response
        Map<String, Object> respMap = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) respMap.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            Map<String, Object> candidateContent = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
            if (parts != null && !parts.isEmpty()) {
                return (String) parts.get(0).get("text");
            }
        }
        throw new RuntimeException("Gemini returned empty candidate output");
    }

    private String callOpenAi(String systemPrompt, String userPrompt) throws Exception {
        String endpoint = "https://api.openai.com/v1/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", (activeModel != null && !activeModel.isBlank()) ? activeModel : "gpt-4o");
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("temperature", 0.3);

        String requestJson = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API returned status " + response.statusCode() + ": " + response.body());
        }

        Map<String, Object> respMap = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            return (String) msg.get("content");
        }
        throw new RuntimeException("OpenAI returned empty choice output");
    }

    private String cleanJsonOutput(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
