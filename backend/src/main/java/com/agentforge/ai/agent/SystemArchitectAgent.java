package com.agentforge.ai.agent;

import com.agentforge.ai.agent.dto.SystemArchitectInput;
import com.agentforge.ai.agent.dto.SystemArchitectOutput;
import com.agentforge.ai.model.RequirementSpec;
import com.agentforge.ai.model.SystemArchitectureSpec;
import com.agentforge.ai.prompt.PromptTemplates;
import com.agentforge.ai.springai.ChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemArchitectAgent implements BaseAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "SYSTEM_ARCHITECT";
    }

    @Override
    public String getAgentName() {
        return "System Architect";
    }

    /**
     * Strongly-typed independent execution entry point for Agent 2.
     */
    public SystemArchitectOutput designArchitecture(SystemArchitectInput input) {
        long start = System.currentTimeMillis();
        log.info("[SystemArchitectAgent] Starting architectural design for project: {} ({})",
                input.getProjectId(), input.getProjectName());

        try {
            RequirementSpec req = input.getRequirementSpec();
            if (req == null) {
                throw new IllegalArgumentException("RequirementSpec cannot be null for SystemArchitectAgent.");
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("System Name: ").append(input.getProjectName() != null ? input.getProjectName() : req.getApplicationName()).append("\n\n");
            promptBuilder.append("Approved Requirements Summary:\n").append(req.getSummary()).append("\n\n");

            if (req.getFunctionalRequirements() != null) {
                promptBuilder.append("Functional Requirements:\n");
                for (String fr : req.getFunctionalRequirements()) {
                    promptBuilder.append("- ").append(fr).append("\n");
                }
                promptBuilder.append("\n");
            }

            if (req.getModules() != null) {
                promptBuilder.append("Target Modules:\n");
                for (RequirementSpec.ModuleSpec mod : req.getModules()) {
                    promptBuilder.append("- ").append(mod.getName()).append(": ").append(mod.getDescription()).append("\n");
                }
                promptBuilder.append("\n");
            }

            promptBuilder.append("Tech Stack Targets:\n");
            promptBuilder.append("- Backend: ").append(input.getPreferredBackend() != null ? input.getPreferredBackend() : "Java 21 / Spring Boot 3.3").append("\n");
            promptBuilder.append("- Frontend: ").append(input.getPreferredFrontend() != null ? input.getPreferredFrontend() : "React 18 / TypeScript / Tailwind CSS").append("\n");
            promptBuilder.append("- Database: ").append(input.getPreferredDatabase() != null ? input.getPreferredDatabase() : "PostgreSQL 16").append("\n\n");

            if (input.getFeedbackMessage() != null && !input.getFeedbackMessage().isBlank()) {
                promptBuilder.append("Architectural Revision Feedback: ").append(input.getFeedbackMessage()).append("\n\n");
            }

            promptBuilder.append("Design the complete system architecture: ERD database tables, REST API endpoints, folder hierarchy, ");
            promptBuilder.append("module dependencies, and OWASP security considerations.");

            log.debug("[SystemArchitectAgent] Dispatching architectural design request to LLM via Spring AI ChatClient");

            SystemArchitectureSpec spec = chatClient.prompt()
                    .system(PromptTemplates.SYSTEM_ARCHITECT_SYSTEM)
                    .user(promptBuilder.toString())
                    .call()
                    .entity(SystemArchitectureSpec.class);

            long duration = System.currentTimeMillis() - start;
            String rawJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);

            int tableCount = spec.getDatabaseSchema() != null ? spec.getDatabaseSchema().size() : 0;
            int endpointCount = spec.getApiEndpoints() != null ? spec.getApiEndpoints().size() : 0;
            String summary = String.format("Designed %s architecture with %d database tables, %d REST endpoints, and %d design patterns in %d ms.",
                    spec.getArchitecturePattern() != null ? spec.getArchitecturePattern() : "Layered",
                    tableCount,
                    endpointCount,
                    spec.getDesignPatterns() != null ? spec.getDesignPatterns().size() : 0,
                    duration);

            log.info("[SystemArchitectAgent] Architecture design completed successfully: {}", summary);

            return SystemArchitectOutput.builder()
                    .success(true)
                    .architectureSpec(spec)
                    .summary(summary)
                    .rawJson(rawJson)
                    .executionDurationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[SystemArchitectAgent] Error during architecture design: {}", e.getMessage(), e);
            return SystemArchitectOutput.builder()
                    .success(false)
                    .errorMessage("Architecture design failed: " + e.getMessage())
                    .executionDurationMs(duration)
                    .build();
        }
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        SystemArchitectInput input = SystemArchitectInput.builder()
                .projectId(context.getProjectId())
                .projectName(context.getProject() != null ? context.getProject().getProjectName() : null)
                .requirementSpec(context.getRequirementSpec())
                .feedbackMessage(context.getFeedbackMessage())
                .build();

        SystemArchitectOutput output = designArchitecture(input);

        if (output.isSuccess()) {
            context.setArchitectureSpec(output.getArchitectureSpec());
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
