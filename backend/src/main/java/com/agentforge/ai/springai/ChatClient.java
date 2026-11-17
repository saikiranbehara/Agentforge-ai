package com.agentforge.ai.springai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatClient {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public ChatClient(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public static ChatClient create(ChatModel chatModel) {
        return new ChatClient(chatModel, new ObjectMapper());
    }

    public static Builder builder(ChatModel chatModel) {
        return new Builder(chatModel);
    }

    public static class Builder {
        private final ChatModel chatModel;
        private ObjectMapper objectMapper = new ObjectMapper();

        public Builder(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public ChatClient build() {
            return new ChatClient(chatModel, objectMapper);
        }
    }

    public PromptSpec prompt() {
        return new PromptSpec();
    }

    public class PromptSpec {
        private String systemText;
        private String userText;
        private ChatOptions options;

        public PromptSpec system(String systemText) {
            this.systemText = systemText;
            return this;
        }

        public PromptSpec user(String userText) {
            this.userText = userText;
            return this;
        }

        public PromptSpec options(ChatOptions options) {
            this.options = options;
            return this;
        }

        public CallResponseSpec call() {
            List<Message> messages = new ArrayList<>();
            if (systemText != null && !systemText.isBlank()) {
                messages.add(new SystemMessage(systemText));
            }
            if (userText != null && !userText.isBlank()) {
                messages.add(new UserMessage(userText));
            }
            Prompt prompt = new Prompt(messages, options);
            return new CallResponseSpec(prompt, systemText, userText);
        }
    }

    public class CallResponseSpec {
        private final Prompt prompt;
        private final String systemText;
        private final String userText;

        public CallResponseSpec(Prompt prompt, String systemText, String userText) {
            this.prompt = prompt;
            this.systemText = systemText;
            this.userText = userText;
        }

        public ChatResponse chatResponse() {
            return chatModel.call(prompt);
        }

        public String content() {
            ChatResponse resp = chatResponse();
            return (resp != null && resp.getResult() != null) ? resp.getResult().getText() : "";
        }

        public <T> T entity(Class<T> type) {
            if (chatModel instanceof SpringAiChatModel springAiChatModel) {
                return springAiChatModel.callEntity(systemText, userText, type);
            }

            // Fallback for generic chat models
            String enhancedSystem = (systemText != null ? systemText : "") 
                    + "\n\nCRITICAL: Respond ONLY with a valid, clean JSON object matching the requested schema. "
                    + "Do NOT wrap with markdown ticks like ```json ... ``` or include conversational filler.";
            
            Prompt structuredPrompt = new Prompt(
                    List.of(new SystemMessage(enhancedSystem), new UserMessage(userText != null ? userText : "")),
                    prompt.getOptions()
            );

            ChatResponse response = chatModel.call(structuredPrompt);
            String raw = (response != null && response.getResult() != null) ? response.getResult().getText() : "";
            String clean = cleanJson(raw);
            try {
                return objectMapper.readValue(clean, type);
            } catch (Exception e) {
                log.error("Failed to parse JSON into {}: {}. Raw content was: {}", type.getSimpleName(), e.getMessage(), raw);
                throw new RuntimeException("Spring AI ChatClient failed to parse entity " + type.getSimpleName() + ": " + e.getMessage(), e);
            }
        }

        private String cleanJson(String raw) {
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
    }
}
