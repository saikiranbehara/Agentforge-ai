package com.agentforge.controller;

import com.agentforge.ai.client.AiClientService;
import com.agentforge.dto.AiConfigDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiClientService aiClientService;

    @GetMapping
    public ResponseEntity<AiConfigDto> getConfig() {
        return ResponseEntity.ok(buildConfigDto("AI configuration loaded"));
    }

    @PostMapping
    public ResponseEntity<AiConfigDto> updateConfig(@RequestBody AiConfigDto dto) {
        if (dto.getGeminiApiKey() != null && !dto.getGeminiApiKey().isBlank()) {
            aiClientService.setGeminiApiKey(dto.getGeminiApiKey().trim());
        }
        if (dto.getOpenaiApiKey() != null && !dto.getOpenaiApiKey().isBlank()) {
            aiClientService.setOpenaiApiKey(dto.getOpenaiApiKey().trim());
        }
        if (dto.getActiveProvider() != null) {
            aiClientService.setActiveProvider(dto.getActiveProvider());
        }
        if (dto.getDefaultModel() != null) {
            aiClientService.setActiveModel(dto.getDefaultModel());
        }
        if (dto.getMockModeEnabled() != null) {
            aiClientService.setForceMockMode(dto.getMockModeEnabled());
        }

        return ResponseEntity.ok(buildConfigDto("AI settings updated successfully"));
    }

    private AiConfigDto buildConfigDto(String msg) {
        String geminiKey = aiClientService.getGeminiApiKey();
        String openAiKey = aiClientService.getOpenaiApiKey();

        String maskedGemini = (geminiKey != null && geminiKey.length() > 6)
                ? geminiKey.substring(0, 4) + "..." + geminiKey.substring(geminiKey.length() - 2)
                : (geminiKey != null && !geminiKey.isBlank() ? "******" : "Not Set");

        String maskedOpenAi = (openAiKey != null && openAiKey.length() > 6)
                ? openAiKey.substring(0, 4) + "..." + openAiKey.substring(openAiKey.length() - 2)
                : (openAiKey != null && !openAiKey.isBlank() ? "******" : "Not Set");

        return AiConfigDto.builder()
                .activeProvider(aiClientService.getActiveProvider())
                .defaultModel(aiClientService.getActiveModel())
                .geminiApiKeyMasked(maskedGemini)
                .openaiApiKeyMasked(maskedOpenAi)
                .mockModeEnabled(aiClientService.isForceMockMode())
                .statusMessage(msg)
                .build();
    }
}
