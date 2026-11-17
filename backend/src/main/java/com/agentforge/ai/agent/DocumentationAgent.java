package com.agentforge.ai.agent;

import com.agentforge.ai.agent.dto.DocumentationAgentInput;
import com.agentforge.ai.agent.dto.DocumentationAgentOutput;
import com.agentforge.ai.model.DocumentationBundle;
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
public class DocumentationAgent implements BaseAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "DOCUMENTATION_AGENT";
    }

    @Override
    public String getAgentName() {
        return "Documentation Agent";
    }

    /**
     * Strongly-typed independent execution entry point for Agent 7.
     */
    public DocumentationAgentOutput generateDocumentation(DocumentationAgentInput input) {
        long start = System.currentTimeMillis();
        log.info("[DocumentationAgent] Generating comprehensive documentation bundle for project: {} ({})",
                input.getProjectId(), input.getProjectName());

        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Project: ").append(input.getProjectName() != null ? input.getProjectName() : "Application").append("\n\n");

            if (input.getRequirementSpec() != null) {
                promptBuilder.append("Requirements Summary: ").append(input.getRequirementSpec().getSummary()).append("\n\n");
            }

            if (input.getArchitectureSpec() != null) {
                promptBuilder.append("Architecture Pattern: ").append(input.getArchitectureSpec().getArchitecturePattern()).append("\n");
                if (input.getArchitectureSpec().getApiEndpoints() != null) {
                    promptBuilder.append("API Endpoints Count: ").append(input.getArchitectureSpec().getApiEndpoints().size()).append("\n");
                }
                if (input.getArchitectureSpec().getDatabaseSchema() != null) {
                    promptBuilder.append("Database Tables Count: ").append(input.getArchitectureSpec().getDatabaseSchema().size()).append("\n");
                }
                promptBuilder.append("\n");
            }

            if (input.getSourceFiles() != null && !input.getSourceFiles().isEmpty()) {
                promptBuilder.append("Key Files Generated: ").append(input.getSourceFiles().size()).append(" source files.\n\n");
            }

            promptBuilder.append("Synthesize a complete documentation suite formatted as DocumentationBundle JSON:\n");
            promptBuilder.append("1. readme: Full Markdown README with badges, overview, stack, run steps, and curl examples.\n");
            promptBuilder.append("2. architectureDoc: System architecture, layers, and data flow.\n");
            promptBuilder.append("3. installationGuide: Prerequisites and step-by-step installation instructions.\n");
            promptBuilder.append("4. apiDocs: Comprehensive REST API endpoint reference.\n");
            promptBuilder.append("5. deploymentGuide: Docker containerization and cloud deployment instructions.");

            log.debug("[DocumentationAgent] Dispatching documentation synthesis prompt to LLM via Spring AI ChatClient");

            DocumentationBundle bundle = chatClient.prompt()
                    .system(PromptTemplates.DOCUMENTATION_AGENT_SYSTEM)
                    .user(promptBuilder.toString())
                    .call()
                    .entity(DocumentationBundle.class);

            long duration = System.currentTimeMillis() - start;
            String summary = String.format("Synthesized documentation bundle (README: %d chars, Architecture: %d chars, API: %d chars, Deployment: %d chars) in %d ms.",
                    bundle.getReadme() != null ? bundle.getReadme().length() : 0,
                    bundle.getArchitectureDoc() != null ? bundle.getArchitectureDoc().length() : 0,
                    bundle.getApiDocs() != null ? bundle.getApiDocs().length() : 0,
                    bundle.getDeploymentGuide() != null ? bundle.getDeploymentGuide().length() : 0,
                    duration);

            log.info("[DocumentationAgent] Documentation generation completed: {}", summary);

            return DocumentationAgentOutput.builder()
                    .success(true)
                    .documentationBundle(bundle)
                    .summary(summary)
                    .executionDurationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[DocumentationAgent] Error during documentation synthesis: {}", e.getMessage(), e);
            return DocumentationAgentOutput.builder()
                    .success(false)
                    .errorMessage("Documentation synthesis failed: " + e.getMessage())
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

        DocumentationAgentInput input = DocumentationAgentInput.builder()
                .projectId(context.getProjectId())
                .projectName(context.getProject() != null ? context.getProject().getProjectName() : null)
                .requirementSpec(context.getRequirementSpec())
                .architectureSpec(context.getArchitectureSpec())
                .sourceFiles(sources)
                .testReport(context.getTestReport())
                .reviewReport(context.getReviewReport())
                .build();

        DocumentationAgentOutput output = generateDocumentation(input);

        if (output.isSuccess()) {
            context.setDocumentationBundle(output.getDocumentationBundle());
            try {
                String rawJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output.getDocumentationBundle());
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
