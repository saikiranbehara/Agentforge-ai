package com.agentforge.ai.agent;

import com.agentforge.ai.agent.dto.RequirementAnalysisInput;
import com.agentforge.ai.agent.dto.RequirementAnalysisOutput;
import com.agentforge.ai.model.RequirementSpec;
import com.agentforge.ai.prompt.PromptTemplates;
import com.agentforge.ai.springai.ChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequirementAnalystAgent implements BaseAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "REQUIREMENT_ANALYST";
    }

    @Override
    public String getAgentName() {
        return "Requirement Analyst";
    }

    /**
     * Strongly-typed independent execution entry point for Agent 1.
     */
    public RequirementAnalysisOutput analyzeRequirements(RequirementAnalysisInput input) {
        long start = System.currentTimeMillis();
        log.info("[RequirementAnalystAgent] Starting requirement analysis for project: {} ({})",
                input.getProjectId(), input.getProjectName());

        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Software Project: ").append(input.getProjectName() != null ? input.getProjectName() : "Application").append("\n\n");
            promptBuilder.append("Natural Language Requirements:\n").append(input.getNaturalLanguageRequirements()).append("\n\n");

            if (input.getTechStackPreference() != null && !input.getTechStackPreference().isBlank()) {
                promptBuilder.append("Technology Preferences: ").append(input.getTechStackPreference()).append("\n\n");
            }
            if (input.getFeedbackMessage() != null && !input.getFeedbackMessage().isBlank()) {
                promptBuilder.append("User Feedback for Revision: ").append(input.getFeedbackMessage()).append("\n\n");
            }

            promptBuilder.append("Analyze these requirements thoroughly. Generate concrete functional requirements, non-functional requirements, ");
            promptBuilder.append("modules, user stories, technical dependencies, and assumptions.");

            log.debug("[RequirementAnalystAgent] Dispatching prompt to LLM via Spring AI ChatClient");

            RequirementSpec spec = chatClient.prompt()
                    .system(PromptTemplates.REQUIREMENT_ANALYST_SYSTEM)
                    .user(promptBuilder.toString())
                    .call()
                    .entity(RequirementSpec.class);

            long duration = System.currentTimeMillis() - start;
            String rawJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);

            int funcCount = spec.getFunctionalRequirements() != null ? spec.getFunctionalRequirements().size() : 0;
            int modCount = spec.getModules() != null ? spec.getModules().size() : 0;
            String summary = String.format("Extracted %d functional requirements, %d non-functional constraints, and %d modules in %d ms.",
                    funcCount,
                    spec.getNonFunctionalRequirements() != null ? spec.getNonFunctionalRequirements().size() : 0,
                    modCount,
                    duration);

            log.info("[RequirementAnalystAgent] Requirement analysis completed successfully: {}", summary);

            return RequirementAnalysisOutput.builder()
                    .success(true)
                    .requirementSpec(spec)
                    .summary(summary)
                    .rawJson(rawJson)
                    .executionDurationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[RequirementAnalystAgent] Error during requirement analysis: {}", e.getMessage(), e);
            return RequirementAnalysisOutput.builder()
                    .success(false)
                    .errorMessage("Requirement analysis failed: " + e.getMessage())
                    .executionDurationMs(duration)
                    .build();
        }
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        RequirementAnalysisInput input = RequirementAnalysisInput.builder()
                .projectId(context.getProjectId())
                .projectName(context.getProject() != null ? context.getProject().getProjectName() : null)
                .naturalLanguageRequirements(context.getUserPrompt())
                .feedbackMessage(context.getFeedbackMessage())
                .build();

        RequirementAnalysisOutput output = analyzeRequirements(input);

        if (output.isSuccess()) {
            context.setRequirementSpec(output.getRequirementSpec());
            return AgentExecutionResult.builder()
                    .success(true)
                    .outputData(output.getRawJson())
                    .summary(output.getSummary())
                    .executionTimeMs(output.getExecutionDurationMs())
                    .build();
        } else {
            return AgentExecutionResult.builder()
                    .success(false)
                    .errorMessage(output.getErrorMessage())
                    .executionTimeMs(output.getExecutionDurationMs())
                    .build();
        }
    }
}
