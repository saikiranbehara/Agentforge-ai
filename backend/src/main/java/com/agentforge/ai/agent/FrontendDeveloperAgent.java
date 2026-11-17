package com.agentforge.ai.agent;

import com.agentforge.ai.agent.dto.FrontendDeveloperInput;
import com.agentforge.ai.agent.dto.FrontendDeveloperOutput;
import com.agentforge.ai.model.GeneratedFileBundle;
import com.agentforge.ai.model.GeneratedSourceFile;
import com.agentforge.ai.model.SystemArchitectureSpec;
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
public class FrontendDeveloperAgent implements BaseAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "FRONTEND_DEVELOPER";
    }

    @Override
    public String getAgentName() {
        return "Frontend Developer";
    }

    /**
     * Strongly-typed independent execution entry point for Agent 4.
     */
    public FrontendDeveloperOutput generateFrontend(FrontendDeveloperInput input) {
        long start = System.currentTimeMillis();
        log.info("[FrontendDeveloperAgent] Initiating React + TypeScript frontend code generation for project: {} ({})",
                input.getProjectId(), input.getProjectName());

        try {
            List<GeneratedSourceFile> generatedFiles = new ArrayList<>();

            // 1. Check if targeted single-file regeneration was requested
            if (input.getTargetFilePath() != null && !input.getTargetFilePath().isBlank()) {
                log.info("[FrontendDeveloperAgent] Targeted single-file regeneration requested for: {}", input.getTargetFilePath());
                GeneratedSourceFile fixedFile = regenerateSingleFile(input);
                generatedFiles.add(fixedFile);
            } else {
                // 2. Full frontend suite generation via LLM
                generatedFiles.addAll(generateFullFrontendSuite(input));
            }

            long duration = System.currentTimeMillis() - start;
            String summary = String.format("Generated %d React + TypeScript source files (Components, Pages, API Clients, Types) in %d ms.",
                    generatedFiles.size(), duration);

            log.info("[FrontendDeveloperAgent] Frontend generation completed: {}", summary);

            return FrontendDeveloperOutput.builder()
                    .success(true)
                    .files(generatedFiles)
                    .summary(summary)
                    .executionDurationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[FrontendDeveloperAgent] Error during frontend generation: {}", e.getMessage(), e);
            return FrontendDeveloperOutput.builder()
                    .success(false)
                    .errorMessage("Frontend code generation failed: " + e.getMessage())
                    .executionDurationMs(duration)
                    .build();
        }
    }

    private List<GeneratedSourceFile> generateFullFrontendSuite(FrontendDeveloperInput input) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Project: ").append(input.getProjectName() != null ? input.getProjectName() : "Application").append("\n\n");

        if (input.getArchitectureSpec() != null) {
            SystemArchitectureSpec arch = input.getArchitectureSpec();
            if (arch.getApiEndpoints() != null && !arch.getApiEndpoints().isEmpty()) {
                userPrompt.append("Backend REST Endpoints to integrate:\n");
                for (SystemArchitectureSpec.ApiEndpointSpec ep : arch.getApiEndpoints()) {
                    userPrompt.append("- ").append(ep.getMethod()).append(" ").append(ep.getPath())
                            .append(" : ").append(ep.getSummary()).append("\n");
                }
                userPrompt.append("\n");
            }
            if (arch.getDatabaseSchema() != null && !arch.getDatabaseSchema().isEmpty()) {
                userPrompt.append("Data Models:\n");
                for (SystemArchitectureSpec.TableSpec t : arch.getDatabaseSchema()) {
                    userPrompt.append("- ").append(t.getTableName()).append(": ").append(t.getDescription()).append("\n");
                }
                userPrompt.append("\n");
            }
        } else if (input.getRequirementSpec() != null) {
            userPrompt.append("Requirements: ").append(input.getRequirementSpec().getSummary()).append("\n\n");
        }

        userPrompt.append("Generate complete, production-ready React 18 / TypeScript / Tailwind CSS Single Page Application source files.\n");
        userPrompt.append("Provide a JSON response with a 'files' array containing:\n");
        userPrompt.append("- filePath: relative path (e.g. frontend/src/pages/CatalogPage.tsx, frontend/src/types/domain.ts)\n");
        userPrompt.append("- content: full TypeScript/React source code with modern hooks and clean styling\n");
        userPrompt.append("- language: typescript\n");
        userPrompt.append("- description: brief component summary\n");

        log.debug("[FrontendDeveloperAgent] Dispatching frontend generation prompt to LLM via Spring AI ChatClient");

        GeneratedFileBundle bundle = chatClient.prompt()
                .system(PromptTemplates.FRONTEND_DEVELOPER_SYSTEM)
                .user(userPrompt.toString())
                .call()
                .entity(GeneratedFileBundle.class);

        if (bundle != null && bundle.getFiles() != null && !bundle.getFiles().isEmpty()) {
            return bundle.getFiles();
        }

        return new ArrayList<>();
    }

    private GeneratedSourceFile regenerateSingleFile(FrontendDeveloperInput input) {
        String filePath = input.getTargetFilePath();
        StringBuilder prompt = new StringBuilder();
        prompt.append("File to regenerate/fix: ").append(filePath).append("\n\n");

        if (input.getFeedbackMessage() != null && !input.getFeedbackMessage().isBlank()) {
            prompt.append("Instructions / Issues to address:\n").append(input.getFeedbackMessage()).append("\n\n");
        }

        prompt.append("Please rewrite the complete corrected file content for ").append(filePath).append(".\n");
        prompt.append("Return ONLY the clean source code for this single file without markdown formatting.");

        String correctedContent = chatClient.prompt()
                .system(PromptTemplates.FRONTEND_DEVELOPER_SYSTEM)
                .user(prompt.toString())
                .call()
                .content();

        if (correctedContent.startsWith("```typescript") || correctedContent.startsWith("```tsx")) {
            correctedContent = correctedContent.substring(correctedContent.indexOf('\n') + 1);
        } else if (correctedContent.startsWith("```")) {
            correctedContent = correctedContent.substring(3);
        }
        if (correctedContent.endsWith("```")) {
            correctedContent = correctedContent.substring(0, correctedContent.length() - 3);
        }

        return GeneratedSourceFile.builder()
                .filePath(filePath)
                .language("typescript")
                .description("Regenerated frontend file")
                .content(correctedContent.trim())
                .build();
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        FrontendDeveloperInput input = FrontendDeveloperInput.builder()
                .projectId(context.getProjectId())
                .projectName(context.getProject() != null ? context.getProject().getProjectName() : null)
                .requirementSpec(context.getRequirementSpec())
                .architectureSpec(context.getArchitectureSpec())
                .targetFilePath(context.getTargetFilePath())
                .feedbackMessage(context.getFeedbackMessage())
                .build();

        FrontendDeveloperOutput output = generateFrontend(input);

        if (output.isSuccess()) {
            return AgentExecutionResult.builder()
                    .success(true)
                    .files(output.getFiles())
                    .summary(output.getSummary())
                    .outputData(output.getSummary())
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
