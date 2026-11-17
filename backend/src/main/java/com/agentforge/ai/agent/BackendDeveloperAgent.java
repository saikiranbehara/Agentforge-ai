package com.agentforge.ai.agent;

import com.agentforge.ai.agent.dto.BackendDeveloperInput;
import com.agentforge.ai.agent.dto.BackendDeveloperOutput;
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
public class BackendDeveloperAgent implements BaseAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "BACKEND_DEVELOPER";
    }

    @Override
    public String getAgentName() {
        return "Backend Developer";
    }

    /**
     * Strongly-typed independent execution entry point for Agent 3.
     */
    public BackendDeveloperOutput generateBackend(BackendDeveloperInput input) {
        long start = System.currentTimeMillis();
        log.info("[BackendDeveloperAgent] Initiating Spring Boot backend code generation for project: {} ({})",
                input.getProjectId(), input.getProjectName());

        try {
            List<GeneratedSourceFile> generatedFiles = new ArrayList<>();

            // 1. Check if this is a targeted single-file regeneration or self-healing fix
            if (input.getTargetFilePath() != null && !input.getTargetFilePath().isBlank()) {
                log.info("[BackendDeveloperAgent] Targeted file regeneration requested for: {}", input.getTargetFilePath());
                GeneratedSourceFile fixedFile = regenerateSingleFile(input);
                generatedFiles.add(fixedFile);
            } else {
                // 2. Full backend generation via LLM
                generatedFiles.addAll(generateFullBackendSuite(input));
            }

            long duration = System.currentTimeMillis() - start;
            String summary = String.format("Generated %d Spring Boot Java source files (Entities, Repositories, Services, Controllers) in %d ms.",
                    generatedFiles.size(), duration);

            log.info("[BackendDeveloperAgent] Backend generation completed: {}", summary);

            return BackendDeveloperOutput.builder()
                    .success(true)
                    .files(generatedFiles)
                    .summary(summary)
                    .executionDurationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[BackendDeveloperAgent] Error during backend generation: {}", e.getMessage(), e);
            return BackendDeveloperOutput.builder()
                    .success(false)
                    .errorMessage("Backend code generation failed: " + e.getMessage())
                    .executionDurationMs(duration)
                    .build();
        }
    }

    private List<GeneratedSourceFile> generateFullBackendSuite(BackendDeveloperInput input) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Project: ").append(input.getProjectName() != null ? input.getProjectName() : "Application").append("\n\n");

        if (input.getArchitectureSpec() != null) {
            SystemArchitectureSpec arch = input.getArchitectureSpec();
            userPrompt.append("Architecture Pattern: ").append(arch.getArchitecturePattern()).append("\n\n");

            if (arch.getDatabaseSchema() != null && !arch.getDatabaseSchema().isEmpty()) {
                userPrompt.append("Database Tables to Implement:\n");
                for (SystemArchitectureSpec.TableSpec table : arch.getDatabaseSchema()) {
                    userPrompt.append("- Table: ").append(table.getTableName()).append(" (").append(table.getDescription()).append(")\n");
                    if (table.getColumns() != null) {
                        for (SystemArchitectureSpec.ColumnSpec col : table.getColumns()) {
                            userPrompt.append("    * ").append(col.getName()).append(" ").append(col.getType())
                                    .append(col.isPrimaryKey() ? " [PK]" : "").append("\n");
                        }
                    }
                }
                userPrompt.append("\n");
            }

            if (arch.getApiEndpoints() != null && !arch.getApiEndpoints().isEmpty()) {
                userPrompt.append("REST Endpoints to Implement:\n");
                for (SystemArchitectureSpec.ApiEndpointSpec ep : arch.getApiEndpoints()) {
                    userPrompt.append("- ").append(ep.getMethod()).append(" ").append(ep.getPath())
                            .append(" : ").append(ep.getSummary()).append("\n");
                }
                userPrompt.append("\n");
            }
        } else if (input.getRequirementSpec() != null) {
            userPrompt.append("Requirements Summary: ").append(input.getRequirementSpec().getSummary()).append("\n\n");
        }

        userPrompt.append("Generate complete, production-ready, compilable Java source code for Spring Boot 3.3 / Java 21.\n");
        userPrompt.append("Provide a JSON response with a 'files' array containing:\n");
        userPrompt.append("- filePath: relative path (e.g. backend/src/main/java/com/app/entity/Product.java)\n");
        userPrompt.append("- content: full java source code with proper imports, annotations, and logic\n");
        userPrompt.append("- language: java\n");
        userPrompt.append("- description: brief file summary\n");

        log.debug("[BackendDeveloperAgent] Dispatching code generation prompt to LLM via Spring AI ChatClient");

        GeneratedFileBundle bundle = chatClient.prompt()
                .system(PromptTemplates.BACKEND_DEVELOPER_SYSTEM)
                .user(userPrompt.toString())
                .call()
                .entity(GeneratedFileBundle.class);

        if (bundle != null && bundle.getFiles() != null && !bundle.getFiles().isEmpty()) {
            return bundle.getFiles();
        }

        return new ArrayList<>();
    }

    private GeneratedSourceFile regenerateSingleFile(BackendDeveloperInput input) {
        String filePath = input.getTargetFilePath();
        StringBuilder prompt = new StringBuilder();
        prompt.append("File to regenerate/fix: ").append(filePath).append("\n\n");

        if (input.getCompilerErrorDetails() != null && !input.getCompilerErrorDetails().isBlank()) {
            prompt.append("Compiler / Test Diagnostic Errors to Fix (Self-Healing):\n");
            prompt.append(input.getCompilerErrorDetails()).append("\n\n");
        }

        if (input.getFeedbackMessage() != null && !input.getFeedbackMessage().isBlank()) {
            prompt.append("User Feedback Instructions:\n");
            prompt.append(input.getFeedbackMessage()).append("\n\n");
        }

        prompt.append("Please rewrite the complete corrected file content for ").append(filePath).append(".\n");
        prompt.append("Return ONLY the clean source code for this single file without markdown formatting.");

        String correctedContent = chatClient.prompt()
                .system(PromptTemplates.BACKEND_DEVELOPER_SYSTEM)
                .user(prompt.toString())
                .call()
                .content();

        // Clean code ticks if any
        if (correctedContent.startsWith("```java")) {
            correctedContent = correctedContent.substring(7);
        } else if (correctedContent.startsWith("```")) {
            correctedContent = correctedContent.substring(3);
        }
        if (correctedContent.endsWith("```")) {
            correctedContent = correctedContent.substring(0, correctedContent.length() - 3);
        }

        return GeneratedSourceFile.builder()
                .filePath(filePath)
                .language("java")
                .description("Self-healed / regenerated file based on compiler diagnostics")
                .content(correctedContent.trim())
                .build();
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        BackendDeveloperInput input = BackendDeveloperInput.builder()
                .projectId(context.getProjectId())
                .projectName(context.getProject() != null ? context.getProject().getProjectName() : null)
                .requirementSpec(context.getRequirementSpec())
                .architectureSpec(context.getArchitectureSpec())
                .targetFilePath(context.getTargetFilePath())
                .compilerErrorDetails(context.getFeedbackMessage())
                .feedbackMessage(context.getCustomInstructions())
                .build();

        BackendDeveloperOutput output = generateBackend(input);

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
