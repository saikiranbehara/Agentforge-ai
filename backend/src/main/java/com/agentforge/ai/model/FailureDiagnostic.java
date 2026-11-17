package com.agentforge.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureDiagnostic {
    private boolean hasFailures;
    private String responsibleFilePath;
    private String responsibleAgentType; // BACKEND_DEVELOPER or FRONTEND_DEVELOPER
    private int lineNumber;
    private String errorSummary;
    private String fullErrorLogs;
    @Builder.Default
    private List<String> failedTestCases = new ArrayList<>();
    private String suggestedFixAction;
}
