package com.agentforge.ai.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementAnalysisInput {
    private Long projectId;
    private String projectName;
    private String naturalLanguageRequirements;
    private String techStackPreference;
    private String feedbackMessage;
}
