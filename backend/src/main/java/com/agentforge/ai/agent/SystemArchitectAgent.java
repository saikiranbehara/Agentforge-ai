package com.agentforge.ai.agent;

import com.agentforge.ai.client.AiClientService;
import com.agentforge.ai.model.RequirementSpec;
import com.agentforge.ai.model.SystemArchitectureSpec;
import com.agentforge.ai.prompt.PromptTemplates;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemArchitectAgent implements BaseAgent {

    private final AiClientService aiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "SYSTEM_ARCHITECT";
    }

    @Override
    public String getAgentName() {
        return "System Architect";
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        long start = System.currentTimeMillis();
        try {
            log.info("Executing SystemArchitectAgent for project: {}", context.getProjectId());
            RequirementSpec reqSpec = context.getRequirementSpec();
            String reqJson = (reqSpec != null) ? objectMapper.writeValueAsString(reqSpec) : context.getUserPrompt();

            String prompt = "Design the technical architecture, database schemas, and REST APIs based on these approved requirements:\n" + reqJson;
            if (context.getFeedbackMessage() != null && !context.getFeedbackMessage().isBlank()) {
                prompt += "\n\nUser Architectural Revision Guidance:\n" + context.getFeedbackMessage();
            }

            SystemArchitectureSpec archSpec = aiClient.generateStructuredJson(
                    PromptTemplates.SYSTEM_ARCHITECT_SYSTEM,
                    prompt,
                    SystemArchitectureSpec.class
            );

            context.setArchitectureSpec(archSpec);
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(archSpec);

            long duration = System.currentTimeMillis() - start;
            int tableCount = (archSpec.getDatabaseSchema() != null) ? archSpec.getDatabaseSchema().size() : 0;
            int endpointCount = (archSpec.getApiEndpoints() != null) ? archSpec.getApiEndpoints().size() : 0;

            return AgentExecutionResult.builder()
                    .success(true)
                    .outputData(jsonOutput)
                    .summary("Architected system with " + tableCount + " relational tables and " + endpointCount + " REST endpoints.")
                    .executionTimeMs(duration)
                    .build();

        } catch (Exception e) {
            log.error("SystemArchitectAgent failed", e);
            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(duration)
                    .build();
        }
    }
}
