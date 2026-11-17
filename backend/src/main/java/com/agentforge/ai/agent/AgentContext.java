package com.agentforge.ai.agent;

import com.agentforge.ai.model.*;
import com.agentforge.entity.GeneratedFile;
import com.agentforge.entity.Project;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentContext {
    private Long projectId;
    private Project project;
    private String userPrompt;
    private RequirementSpec requirementSpec;
    private SystemArchitectureSpec architectureSpec;
    
    @Builder.Default
    private List<GeneratedFile> generatedFiles = new ArrayList<>();
    
    private TestExecutionReport testReport;
    private CodeReviewReport reviewReport;
    private DocumentationBundle documentationBundle;
    
    @Builder.Default
    private int retryCount = 0;
    
    private String feedbackMessage;
    private String customInstructions;
    private String targetFilePath;
}
