package com.agentforge.ai.agent;

import com.agentforge.ai.client.AiClientService;
import com.agentforge.ai.model.RequirementSpec;
import com.agentforge.ai.prompt.PromptTemplates;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequirementAnalystAgent implements BaseAgent {

    private final AiClientService aiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "REQUIREMENT_ANALYST";
    }

    @Override
    public String getAgentName() {
        return "Requirement Analyst";
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        long start = System.currentTimeMillis();
        try {
            log.info("Executing RequirementAnalystAgent for project: {}", context.getProjectId());
            String userPrompt = "Analyze and extract technical requirements for this software:\n" 
                    + context.getUserPrompt();

            if (context.getFeedbackMessage() != null && !context.getFeedbackMessage().isBlank()) {
                userPrompt += "\n\nUser feedback for revision:\n" + context.getFeedbackMessage();
            }

            RequirementSpec spec = aiClient.generateStructuredJson(
                    PromptTemplates.REQUIREMENT_ANALYST_SYSTEM,
                    userPrompt,
                    RequirementSpec.class
            );

            context.setRequirementSpec(spec);
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);

            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(true)
                    .outputData(jsonOutput)
                    .summary("Extracted " + (spec.getFunctionalRequirements() != null ? spec.getFunctionalRequirements().size() : 0) 
                            + " functional requirements and " + (spec.getModules() != null ? spec.getModules().size() : 0) + " core modules.")
                    .executionTimeMs(duration)
                    .build();

        } catch (Exception e) {
            log.error("RequirementAnalystAgent failed", e);
            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(duration)
                    .build();
        }
    }
}
