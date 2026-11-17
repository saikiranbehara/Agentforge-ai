package com.agentforge.ai.agent.dto;

import com.agentforge.ai.model.GeneratedSourceFile;
import com.agentforge.ai.model.RequirementSpec;
import com.agentforge.ai.model.SystemArchitectureSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestingAgentInput {
    private Long projectId;
    private String projectName;
    private RequirementSpec requirementSpec;
    private SystemArchitectureSpec architectureSpec;
    @Builder.Default
    private List<GeneratedSourceFile> sourceFiles = new ArrayList<>();
    private String testCommand;
    @Builder.Default
    private int timeoutSeconds = 30;
}
