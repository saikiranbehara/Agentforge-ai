package com.agentforge.ai.springai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ObjectMapper objectMapper) {
        return ChatClient.builder(chatModel)
                .objectMapper(objectMapper)
                .build();
    }
}
