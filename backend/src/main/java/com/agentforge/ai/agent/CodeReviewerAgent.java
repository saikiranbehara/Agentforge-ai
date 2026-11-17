package com.agentforge.ai.agent;

import com.agentforge.ai.agent.dto.CodeReviewerInput;
import com.agentforge.ai.agent.dto.CodeReviewerOutput;
import com.agentforge.ai.model.CodeReviewReport;
import com.agentforge.ai.model.GeneratedSourceFile;
import com.agentforge.ai.prompt.PromptTemplates;
import com.agentforge.ai.springai.ChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeReviewerAgent implements BaseAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "CODE_REVIEWER";
    }

    @Override
    public String getAgentName() {
        return "Code Reviewer";
    }

    /**
     * Strongly-typed independent execution entry point for Agent 6.
     */
    public CodeReviewerOutput reviewCode(CodeReviewerInput input) {
        long start = System.currentTimeMillis();
        log.info("[CodeReviewerAgent] Starting automated code quality & security review for project: {} ({})",
                input.getProjectId(), input.getProjectName());

        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Project: ").append(input.getProjectName() != null ? input.getProjectName() : "Application").append("\n\n");

            if (input.getSourceFiles() != null && !input.getSourceFiles().isEmpty()) {
                promptBuilder.append("Generated Codebase Files (").append(input.getSourceFiles().size()).append(" total):\n");
                for (GeneratedSourceFile file : input.getSourceFiles()) {
                    promptBuilder.append("--- File: ").append(file.getFilePath()).append(" ---\n");
                    // Truncate file content to avoid extreme context blowup while providing sufficient lines for review
                    String content = file.getContent() != null ? file.getContent() : "";
                    promptBuilder.append(content.length() > 600 ? content.substring(0, 600) + "\n...[truncated]..." : content).append("\n\n");
                }
            }

            if (input.getTestReport() != null) {
                promptBuilder.append("Test Suite Execution Outcome:\n");
                promptBuilder.append("- Total Tests: ").append(input.getTestReport().getTotalTests()).append("\n");
                promptBuilder.append("- Passed: ").append(input.getTestReport().getPassedCount()).append("\n");
                promptBuilder.append("- Failed: ").append(input.getTestReport().getFailedCount()).append("\n\n");
            }

            promptBuilder.append("Conduct a comprehensive static analysis and security audit.\n");
            promptBuilder.append("Check OWASP vulnerabilities, clean code practices, and efficiency.\n");
            promptBuilder.append("Generate a CodeReviewReport JSON with qualityScore (0-100), approved (boolean), summary, issues list, and suggestions.");

            log.debug("[CodeReviewerAgent] Dispatching code review request to LLM via Spring AI ChatClient");

            CodeReviewReport report = chatClient.prompt()
                    .system(PromptTemplates.CODE_REVIEWER_SYSTEM)
                    .user(promptBuilder.toString())
                    .call()
                    .entity(CodeReviewReport.class);

            long duration = System.currentTimeMillis() - start;
            int issueCount = report.getIssues() != null ? report.getIssues().size() : 0;
            String summary = String.format("Code Quality Score: %d/100. Status: %s. Identified %d issues and %d suggestions in %d ms.",
                    report.getQualityScore(),
                    report.isApproved() ? "APPROVED" : "REJECTED",
                    issueCount,
                    report.getSuggestions() != null ? report.getSuggestions().size() : 0,
                    duration);

            log.info("[CodeReviewerAgent] Code review completed: {}", summary);

            return CodeReviewerOutput.builder()
                    .success(true)
                    .report(report)
                    .summary(summary)
                    .executionDurationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[CodeReviewerAgent] Error during code review: {}", e.getMessage(), e);
            return CodeReviewerOutput.builder()
                    .success(false)
                    .errorMessage("Code review failed: " + e.getMessage())
                    .executionDurationMs(duration)
                    .build();
        }
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        List<GeneratedSourceFile> sources = new ArrayList<>();
        if (context.getGeneratedFiles() != null) {
            for (var f : context.getGeneratedFiles()) {
                sources.add(GeneratedSourceFile.builder()
                        .filePath(f.getFilePath())
                        .content(f.getFileContent())
                        .language(f.getLanguage())
                        .build());
            }
        }

        CodeReviewerInput input = CodeReviewerInput.builder()
                .projectId(context.getProjectId())
                .projectName(context.getProject() != null ? context.getProject().getProjectName() : null)
                .requirementSpec(context.getRequirementSpec())
                .architectureSpec(context.getArchitectureSpec())
                .sourceFiles(sources)
                .testReport(context.getTestReport())
                .build();

        CodeReviewerOutput output = reviewCode(input);

        if (output.isSuccess()) {
            context.setReviewReport(output.getReport());
            try {
                String rawJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output.getReport());
                return AgentExecutionResult.builder()
                        .success(true)
                        .outputData(rawJson)
                        .summary(output.getSummary())
                        .executionTimeMs(output.getExecutionDurationMs())
                        .build();
            } catch (Exception e) {
                return AgentExecutionResult.builder()
                        .success(true)
                        .summary(output.getSummary())
                        .executionTimeMs(output.getExecutionDurationMs())
                        .build();
            }
        } else {
            return AgentExecutionResult.builder()
                    .success(false)
                    .errorMessage(output.getErrorMessage())
                    .executionTimeMs(output.getExecutionDurationMs())
                    .build();
        }
    }
}
