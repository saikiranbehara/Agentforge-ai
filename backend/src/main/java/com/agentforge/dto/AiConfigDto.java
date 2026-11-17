package com.agentforge.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConfigDto {
    private String activeProvider; // GEMINI, OPENAI, MOCK
    private String geminiApiKeyMasked;
    private String openaiApiKeyMasked;
    private String geminiApiKey; // when setting
    private String openaiApiKey; // when setting
    private String defaultModel;
    private Boolean mockModeEnabled;
    private String statusMessage;
}
