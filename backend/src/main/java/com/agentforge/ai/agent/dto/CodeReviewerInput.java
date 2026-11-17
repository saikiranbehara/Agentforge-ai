package com.agentforge.ai.agent.dto;

import com.agentforge.ai.model.GeneratedSourceFile;
import com.agentforge.ai.model.RequirementSpec;
import com.agentforge.ai.model.SystemArchitectureSpec;
import com.agentforge.ai.model.TestExecutionReport;
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
public class CodeReviewerInput {
    private Long projectId;
    private String projectName;
    private RequirementSpec requirementSpec;
    private SystemArchitectureSpec architectureSpec;
    @Builder.Default
    private List<GeneratedSourceFile> sourceFiles = new ArrayList<>();
    private TestExecutionReport testReport;
}
