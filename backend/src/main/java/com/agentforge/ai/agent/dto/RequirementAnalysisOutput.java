package com.agentforge.ai.agent.dto;

import com.agentforge.ai.model.RequirementSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementAnalysisOutput {
    private boolean success;
    private RequirementSpec requirementSpec;
    private String summary;
    private String rawJson;
    private String errorMessage;
    private long executionDurationMs;
}
