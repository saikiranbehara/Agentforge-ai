package com.agentforge.ai.agent.dto;

import com.agentforge.ai.model.DocumentationBundle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationAgentOutput {
    private boolean success;
    private DocumentationBundle documentationBundle;
    private String summary;
    private String errorMessage;
    private long executionDurationMs;
}
