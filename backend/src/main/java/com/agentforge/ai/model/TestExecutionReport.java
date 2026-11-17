package com.agentforge.ai.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecutionReport {
    private int totalTests;
    private int passedCount;
    private int failedCount;
    private boolean allPassed;
    private String executionLogs;
    private List<TestCaseResult> testCases;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCaseResult {
        private String testName;
        private String testClass;
        private String status; // PASSED, FAILED, ERROR
        private String errorMessage;
        private String stackTrace;
        private long executionTimeMs;
    }
}
