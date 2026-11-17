package com.agentforge.ai.agent;

import com.agentforge.ai.client.AiClientService;
import com.agentforge.ai.model.DocumentationBundle;
import com.agentforge.ai.model.GeneratedSourceFile;
import com.agentforge.ai.prompt.PromptTemplates;
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

    private final AiClientService aiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "DOCUMENTATION_AGENT";
    }

    @Override
    public String getAgentName() {
        return "Documentation Agent";
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        long start = System.currentTimeMillis();
        try {
            log.info("Executing DocumentationAgent for project: {}", context.getProjectId());

            String prompt = "Synthesize comprehensive system documentation for this software project:\n"
                    + context.getUserPrompt();

            DocumentationBundle docs = aiClient.generateStructuredJson(
                    PromptTemplates.DOCUMENTATION_AGENT_SYSTEM,
                    prompt,
                    DocumentationBundle.class
            );

            context.setDocumentationBundle(docs);
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(docs);

            // Add markdown files to generated files list
            List<GeneratedSourceFile> docFiles = new ArrayList<>();
            docFiles.add(GeneratedSourceFile.builder()
                    .filePath("README.md")
                    .language("markdown")
                    .description("Project Master README")
                    .content(docs.getReadme())
                    .build());
            docFiles.add(GeneratedSourceFile.builder()
                    .filePath("docs/ARCHITECTURE.md")
                    .language("markdown")
                    .description("Detailed Architecture Guide")
                    .content(docs.getArchitectureDoc())
                    .build());
            docFiles.add(GeneratedSourceFile.builder()
                    .filePath("docs/API_REFERENCE.md")
                    .language("markdown")
                    .description("Complete REST API Reference")
                    .content(docs.getApiDocs())
                    .build());

            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(true)
                    .outputData(jsonOutput)
                    .summary("Generated complete documentation suite: README.md, Architecture Specs, Installation manual, and API Reference.")
                    .files(docFiles)
                    .executionTimeMs(duration)
                    .build();

        } catch (Exception e) {
            log.error("DocumentationAgent failed", e);
            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(duration)
                    .build();
        }
    }
}
