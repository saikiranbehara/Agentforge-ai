package com.agentforge.ai.agent.dto;

import com.agentforge.ai.model.GeneratedSourceFile;
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
public class TestingAgentOutput {
    private boolean success;
    private TestExecutionReport report;
    @Builder.Default
    private List<GeneratedSourceFile> testFiles = new ArrayList<>();
    private String summary;
    private String errorMessage;
    private long executionDurationMs;
}
