package com.agentforge.ai.agent;

import com.agentforge.ai.client.AiClientService;
import com.agentforge.ai.model.CodeReviewReport;
import com.agentforge.ai.prompt.PromptTemplates;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeReviewerAgent implements BaseAgent {

    private final AiClientService aiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "CODE_REVIEWER";
    }

    @Override
    public String getAgentName() {
        return "Code Reviewer";
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        long start = System.currentTimeMillis();
        try {
            log.info("Executing CodeReviewerAgent for project: {}", context.getProjectId());

            StringBuilder codeSummary = new StringBuilder();
            if (context.getGeneratedFiles() != null) {
                for (var f : context.getGeneratedFiles()) {
                    codeSummary.append("File: ").append(f.getFilePath()).append("\n");
                    String content = f.getFileContent();
                    if (content.length() > 500) {
                        codeSummary.append(content, 0, 500).append("\n...[truncated]...\n");
                    } else {
                        codeSummary.append(content).append("\n");
                    }
                    codeSummary.append("----------------------------\n");
                }
            }

            String prompt = "Review the following generated source code for security flaws, bugs, and performance bottlenecks:\n"
                    + codeSummary.toString();

            CodeReviewReport report = aiClient.generateStructuredJson(
                    PromptTemplates.CODE_REVIEWER_SYSTEM,
                    prompt,
                    CodeReviewReport.class
            );

            context.setReviewReport(report);
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);

            long duration = System.currentTimeMillis() - start;
            int issueCount = (report.getIssues() != null) ? report.getIssues().size() : 0;
            return AgentExecutionResult.builder()
                    .success(true)
                    .outputData(jsonOutput)
                    .summary("Code quality score: " + report.getQualityScore() + "/100. Status: " + (report.isApproved() ? "APPROVED" : "CHANGES REQUESTED") + " (" + issueCount + " items noted).")
                    .executionTimeMs(duration)
                    .build();

        } catch (Exception e) {
            log.error("CodeReviewerAgent failed", e);
            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(duration)
                    .build();
        }
    }
}
