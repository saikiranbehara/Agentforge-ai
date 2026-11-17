package com.agentforge.ai.agent.dto;

import com.agentforge.ai.model.GeneratedSourceFile;
import com.agentforge.ai.model.RequirementSpec;
import com.agentforge.ai.model.SystemArchitectureSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackendDeveloperInput {
    private Long projectId;
    private String projectName;
    private RequirementSpec requirementSpec;
    private SystemArchitectureSpec architectureSpec;
    private String targetFilePath;
    private String compilerErrorDetails;
    private String feedbackMessage;
    private List<GeneratedSourceFile> existingFiles;
}
