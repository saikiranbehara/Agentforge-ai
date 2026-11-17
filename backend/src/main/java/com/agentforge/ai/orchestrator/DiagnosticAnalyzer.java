package com.agentforge.ai.orchestrator;

import com.agentforge.ai.model.CodeReviewReport;
import com.agentforge.ai.model.FailureDiagnostic;
import com.agentforge.ai.model.GeneratedSourceFile;
import com.agentforge.ai.model.TestExecutionReport;
import com.agentforge.sandbox.SandboxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Diagnostic Analyzer:
 * Parses sandbox compiler errors, JUnit stack traces, and code review findings to:
 * 1. Pinpoint exact responsible source files and line numbers.
 * 2. Determine whether Backend or Frontend Developer agent is responsible.
 * 3. Package clean diagnostic payloads for targeted autonomous self-healing.
 */
@Slf4j
@Component
public class DiagnosticAnalyzer {

    // Regex pattern matching Java compiler errors: /path/to/File.java:[line,col] error: message
    private static final Pattern COMPILER_ERROR_PATTERN = Pattern.compile(
            "([a-zA-Z0-9_./\\\\-]+\\.(java|ts|tsx|js)):\\[?(\\d+)[,:]?(\\d+)?\\]?:?\\s*(.*)"
    );

    public FailureDiagnostic analyzeFailure(SandboxResult sandboxResult,
                                           TestExecutionReport testReport,
                                           CodeReviewReport reviewReport,
                                           List<GeneratedSourceFile> availableFiles) {

        String stdout = sandboxResult != null && sandboxResult.getStdout() != null ? sandboxResult.getStdout() : "";
        String stderr = sandboxResult != null && sandboxResult.getStderr() != null ? sandboxResult.getStderr() : "";
        String combinedLogs = stdout + "\n" + stderr;

        boolean hasSandboxFailure = sandboxResult != null && (!sandboxResult.isSuccess() || sandboxResult.getExitCode() != 0);
        boolean hasTestFailure = testReport != null && !testReport.isAllPassed();
        boolean hasReviewRejection = reviewReport != null && !reviewReport.isApproved();

        if (!hasSandboxFailure && !hasTestFailure && !hasReviewRejection) {
            return FailureDiagnostic.builder()
                    .hasFailures(false)
                    .errorSummary("No failures detected. Build, tests, and code review all passed.")
                    .build();
        }

        String responsibleFile = null;
        String responsibleAgent = "BACKEND_DEVELOPER";
        int lineNumber = 0;
        String errorSummary = "";
        List<String> failedTestNames = new ArrayList<>();

        // 1. Check Sandbox Compiler & Build Output
        if (hasSandboxFailure || combinedLogs.contains("[ERROR]") || combinedLogs.contains("COMPILATION ERROR")) {
            log.info("[DiagnosticAnalyzer] Scanning sandbox output for compiler diagnostics...");
            String[] lines = combinedLogs.split("\\r?\\n");
            for (String line : lines) {
                Matcher matcher = COMPILER_ERROR_PATTERN.matcher(line);
                if (matcher.find()) {
                    String matchedPath = matcher.group(1);
                    responsibleFile = resolveMatchingFile(matchedPath, availableFiles);
                    try {
                        lineNumber = Integer.parseInt(matcher.group(3));
                    } catch (NumberFormatException ignored) {}
                    errorSummary = matcher.group(5) != null ? matcher.group(5).trim() : line;
                    break;
                }
            }
        }

        // 2. Check JUnit / Test Execution Report
        if (hasTestFailure && testReport != null && testReport.getTestCases() != null) {
            log.info("[DiagnosticAnalyzer] Analyzing failed test cases...");
            for (TestExecutionReport.TestCaseResult tc : testReport.getTestCases()) {
                if (!"PASSED".equalsIgnoreCase(tc.getStatus())) {
                    failedTestNames.add(tc.getTestName() + " (" + tc.getTestClass() + ")");
                    if (responsibleFile == null) {
                        // Infer target file from test class name (e.g. ProductServiceTest -> ProductService.java)
                        String testClass = tc.getTestClass();
                        if (testClass != null) {
                            String targetName = testClass.replace("Test", "").replace("IT", "");
                            int lastDot = targetName.lastIndexOf('.');
                            if (lastDot >= 0) targetName = targetName.substring(lastDot + 1);
                            for (GeneratedSourceFile f : availableFiles) {
                                if (f.getFilePath().contains(targetName)) {
                                    responsibleFile = f.getFilePath();
                                    break;
                                }
                            }
                        }
                        if (tc.getErrorMessage() != null && !tc.getErrorMessage().isBlank()) {
                            errorSummary = tc.getErrorMessage();
                        }
                    }
                }
            }
        }

        // 3. Check Code Review Findings
        if (responsibleFile == null && hasReviewRejection && reviewReport != null && reviewReport.getIssues() != null) {
            for (CodeReviewReport.ReviewIssue issue : reviewReport.getIssues()) {
                if ("CRITICAL".equalsIgnoreCase(issue.getSeverity()) || "ERROR".equalsIgnoreCase(issue.getSeverity())) {
                    responsibleFile = issue.getFilePath();
                    lineNumber = issue.getLineNumber();
                    errorSummary = issue.getIssueDescription() + " | Suggested Fix: " + issue.getSuggestedFix();
                    break;
                }
            }
        }

        // Fallback file resolution if specific file could not be parsed from logs
        if (responsibleFile == null && availableFiles != null && !availableFiles.isEmpty()) {
            // Pick primary service or controller
            for (GeneratedSourceFile f : availableFiles) {
                if (f.getFilePath().contains("Service.java") || f.getFilePath().contains("Controller.java")) {
                    responsibleFile = f.getFilePath();
                    break;
                }
            }
            if (responsibleFile == null) {
                responsibleFile = availableFiles.get(0).getFilePath();
            }
        }

        // Determine responsible agent based on file extension
        if (responsibleFile != null) {
            if (responsibleFile.endsWith(".ts") || responsibleFile.endsWith(".tsx") || responsibleFile.endsWith(".jsx")
                    || responsibleFile.endsWith(".css") || responsibleFile.contains("frontend/")) {
                responsibleAgent = "FRONTEND_DEVELOPER";
            } else {
                responsibleAgent = "BACKEND_DEVELOPER";
            }
        }

        if (errorSummary.isBlank()) {
            if (hasTestFailure) {
                errorSummary = "Test assertions failed: " + String.join(", ", failedTestNames);
            } else if (hasSandboxFailure) {
                errorSummary = "Compiler or execution error detected with exit code " + (sandboxResult != null ? sandboxResult.getExitCode() : -1);
            } else if (hasReviewRejection) {
                errorSummary = reviewReport != null ? reviewReport.getSummary() : "Code review quality standards not met.";
            }
        }

        log.info("[DiagnosticAnalyzer] Identified responsible file: {} (Agent: {}, Line: {})",
                responsibleFile, responsibleAgent, lineNumber);

        return FailureDiagnostic.builder()
                .hasFailures(true)
                .responsibleFilePath(responsibleFile)
                .responsibleAgentType(responsibleAgent)
                .lineNumber(lineNumber)
                .errorSummary(errorSummary)
                .fullErrorLogs(combinedLogs)
                .failedTestCases(failedTestNames)
                .suggestedFixAction("Diagnose failure in " + responsibleFile + " and generate a corrected implementation.")
                .build();
    }

    private String resolveMatchingFile(String rawPath, List<GeneratedSourceFile> availableFiles) {
        if (rawPath == null || availableFiles == null) return rawPath;
        String normalized = rawPath.replace('\\', '/');
        for (GeneratedSourceFile f : availableFiles) {
            String fNorm = f.getFilePath().replace('\\', '/');
            if (fNorm.endsWith(normalized) || normalized.endsWith(fNorm) || fNorm.contains(normalized)) {
                return f.getFilePath();
            }
        }
        return normalized;
    }
}
