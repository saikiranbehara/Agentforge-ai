package com.agentforge;

import com.agentforge.ai.agent.*;
import com.agentforge.ai.client.AiClientService;
import com.agentforge.ai.client.MockAiProvider;
import com.agentforge.ai.model.*;
import com.agentforge.entity.Project;
import com.agentforge.sandbox.ProcessIsolatedSandbox;
import com.agentforge.sandbox.SandboxManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AiAgentExecutionTest {

    private ObjectMapper objectMapper;
    private MockAiProvider mockAiProvider;
    private AiClientService aiClientService;
    private SandboxManager sandboxManager;

    private RequirementAnalystAgent requirementAnalyst;
    private SystemArchitectAgent systemArchitect;
    private BackendDeveloperAgent backendDeveloper;
    private FrontendDeveloperAgent frontendDeveloper;
    private TestingAgent testingAgent;
    private CodeReviewerAgent codeReviewer;
    private DocumentationAgent documentationAgent;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockAiProvider = new MockAiProvider(objectMapper);
        aiClientService = new AiClientService(
                objectMapper,
                mockAiProvider,
                "",
                "",
                "MOCK",
                "gemini-1.5-pro",
                true // force mock mode
        );

        sandboxManager = new SandboxManager(
                new com.agentforge.sandbox.DockerExecutionSandbox(),
                new ProcessIsolatedSandbox()
        );

        requirementAnalyst = new RequirementAnalystAgent(aiClientService, objectMapper);
        systemArchitect = new SystemArchitectAgent(aiClientService, objectMapper);
        backendDeveloper = new BackendDeveloperAgent(aiClientService, objectMapper);
        frontendDeveloper = new FrontendDeveloperAgent(aiClientService, objectMapper);
        testingAgent = new TestingAgent(aiClientService, sandboxManager, objectMapper);
        codeReviewer = new CodeReviewerAgent(aiClientService, objectMapper);
        documentationAgent = new DocumentationAgent(aiClientService, objectMapper);
    }

    @Test
    void testRequirementAnalystExecution() {
        AgentContext context = AgentContext.builder()
                .projectId(1L)
                .userPrompt("Build an online shopping platform with cart and checkout")
                .build();

        AgentExecutionResult result = requirementAnalyst.execute(context);

        assertTrue(result.isSuccess());
        assertNotNull(context.getRequirementSpec());
        assertFalse(context.getRequirementSpec().getFunctionalRequirements().isEmpty());
        assertFalse(context.getRequirementSpec().getModules().isEmpty());
    }

    @Test
    void testSystemArchitectExecution() {
        AgentContext context = AgentContext.builder()
                .projectId(1L)
                .userPrompt("Build an online shopping platform with cart and checkout")
                .build();

        requirementAnalyst.execute(context);
        AgentExecutionResult result = systemArchitect.execute(context);

        assertTrue(result.isSuccess());
        assertNotNull(context.getArchitectureSpec());
        assertFalse(context.getArchitectureSpec().getDatabaseSchema().isEmpty());
        assertFalse(context.getArchitectureSpec().getApiEndpoints().isEmpty());
    }

    @Test
    void testDeveloperAgentsAndDocumentation() {
        AgentContext context = AgentContext.builder()
                .projectId(1L)
                .userPrompt("Build an online shopping platform with cart and checkout")
                .build();

        requirementAnalyst.execute(context);
        systemArchitect.execute(context);

        AgentExecutionResult beResult = backendDeveloper.execute(context);
        assertTrue(beResult.isSuccess());
        assertFalse(beResult.getFiles().isEmpty());

        AgentExecutionResult feResult = frontendDeveloper.execute(context);
        assertTrue(feResult.isSuccess());
        assertFalse(feResult.getFiles().isEmpty());

        AgentExecutionResult docResult = documentationAgent.execute(context);
        assertTrue(docResult.isSuccess());
        assertNotNull(context.getDocumentationBundle());
        assertNotNull(context.getDocumentationBundle().getReadme());
    }
}
