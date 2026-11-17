package com.agentforge.ai.springai;

import com.agentforge.ai.client.AiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiChatModel implements ChatModel {

    private final AiClientService aiClientService;

    @Override
    public ChatResponse call(Prompt prompt) {
        StringBuilder systemBuilder = new StringBuilder();
        StringBuilder userBuilder = new StringBuilder();

        if (prompt != null && prompt.getMessages() != null) {
            for (Message msg : prompt.getMessages()) {
                if (msg.getMessageType() == MessageType.SYSTEM) {
                    if (systemBuilder.length() > 0) systemBuilder.append("\n");
                    systemBuilder.append(msg.getContent());
                } else if (msg.getMessageType() == MessageType.USER) {
                    if (userBuilder.length() > 0) userBuilder.append("\n");
                    userBuilder.append(msg.getContent());
                }
            }
        }

        String systemPrompt = systemBuilder.toString();
        String userPrompt = userBuilder.toString();

        log.debug("[Spring AI ChatModel] Calling LLM with system prompt ({} chars) and user prompt ({} chars)",
                systemPrompt.length(), userPrompt.length());

        String generatedText = aiClientService.generateText(systemPrompt, userPrompt);

        AssistantMessage assistantMessage = new AssistantMessage(generatedText);
        Generation generation = new Generation(assistantMessage);

        return ChatResponse.builder()
                .generations(List.of(generation))
                .build();
    }

    /**
     * Strongly-typed entity invocation delegating to the underlying LLM provider / Structured output converter.
     */
    public <T> T callEntity(String systemPrompt, String userPrompt, Class<T> entityType) {
        log.debug("[Spring AI ChatModel] Calling LLM for structured entity: {}", entityType.getSimpleName());
        return aiClientService.generateStructuredJson(systemPrompt, userPrompt, entityType);
    }
}
