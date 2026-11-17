package com.agentforge.ai.springai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatOptions {
    private String model;
    @Builder.Default
    private Double temperature = 0.3;
    @Builder.Default
    private Integer maxTokens = 4096;
}
