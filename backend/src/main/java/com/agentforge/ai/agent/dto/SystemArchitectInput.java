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
public class SystemArchitectInput {
    private Long projectId;
    private String projectName;
    private RequirementSpec requirementSpec;
    private String preferredBackend;
    private String preferredFrontend;
    private String preferredDatabase;
    private String feedbackMessage;
}
