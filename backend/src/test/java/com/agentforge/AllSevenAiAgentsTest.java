package com.agentforge;

import com.agentforge.ai.agent.*;
import com.agentforge.ai.agent.dto.*;
import com.agentforge.ai.client.AiClientService;
import com.agentforge.ai.client.MockAiProvider;
import com.agentforge.ai.model.*;
import com.agentforge.ai.springai.ChatClient;
import com.agentforge.ai.springai.SpringAiChatModel;
import com.agentforge.sandbox.DockerExecutionSandbox;
import com.agentforge.sandbox.ProcessIsolatedSandbox;
import com.agentforge.sandbox.SandboxManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Comprehensive Independent Testing of All Seven AI Agents")
public class AllSevenAiAgentsTest {

    private ObjectMapper objectMapper;
    private ChatClient chatClient;
    private SandboxManager sandboxManager;

    private RequirementAnalystAgent requirementAnalystAgent;
    private SystemArchitectAgent systemArchitectAgent;
    private BackendDeveloperAgent backendDeveloperAgent;
    private FrontendDeveloperAgent frontendDeveloperAgent;
    private TestingAgent testingAgent;
    private CodeReviewerAgent codeReviewerAgent;
    private DocumentationAgent documentationAgent;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        MockAiProvider mockAiProvider = new MockAiProvider(objectMapper);
        AiClientService aiClientService = new AiClientService(
                objectMapper,
                mockAiProvider,
                "",
                "",
                "MOCK",
                "gemini-1.5-pro",
                true // force mock provider for deterministic tests
        );

        SpringAiChatModel chatModel = new SpringAiChatModel(aiClientService);
        chatClient = ChatClient.builder(chatModel)
                .objectMapper(objectMapper)
                .build();

        sandboxManager = new SandboxManager(
                new DockerExecutionSandbox(),
                new ProcessIsolatedSandbox()
        );

        requirementAnalystAgent = new RequirementAnalystAgent(chatClient, objectMapper);
        systemArchitectAgent = new SystemArchitectAgent(chatClient, objectMapper);
        backendDeveloperAgent = new BackendDeveloperAgent(chatClient, objectMapper);
        frontendDeveloperAgent = new FrontendDeveloperAgent(chatClient, objectMapper);
        testingAgent = new TestingAgent(chatClient, sandboxManager, objectMapper);
        codeReviewerAgent = new CodeReviewerAgent(chatClient, objectMapper);
        documentationAgent = new DocumentationAgent(chatClient, objectMapper);
    }

    @Test
    @DisplayName("Agent 1: RequirementAnalystAgent independently analyzes natural language requirements")
    void testAgent1RequirementAnalyst() {
        RequirementAnalysisInput input = RequirementAnalysisInput.builder()
                .projectId(101L)
                .projectName("OmniCommerce Hub")
                .naturalLanguageRequirements("Build a multi-vendor online shop with product management, shopping cart, and order checkout.")
                .techStackPreference("Spring Boot, React, PostgreSQL")
                .build();

        RequirementAnalysisOutput output = requirementAnalystAgent.analyzeRequirements(input);

        assertNotNull(output);
        assertTrue(output.isSuccess(), "Agent 1 execution should succeed");
        assertNotNull(output.getRequirementSpec());
        assertFalse(output.getRequirementSpec().getFunctionalRequirements().isEmpty());
        assertFalse(output.getRequirementSpec().getModules().isEmpty());
        assertNotNull(output.getSummary());
        assertTrue(output.getExecutionDurationMs() >= 0);
        assertEquals("REQUIREMENT_ANALYST", requirementAnalystAgent.getAgentType());
    }

    @Test
    @DisplayName("Agent 2: SystemArchitectAgent independently designs database schema and REST APIs")
    void testAgent2SystemArchitect() {
        RequirementSpec requirementSpec = RequirementSpec.builder()
                .applicationName("OmniCommerce Hub")
                .summary("Multi-vendor e-commerce platform")
                .functionalRequirements(List.of("User authentication", "Product catalog", "Order processing"))
                .modules(List.of(new RequirementSpec.ModuleSpec("Catalog", "Product management", List.of("Create product"))))
                .build();

        SystemArchitectInput input = SystemArchitectInput.builder()
                .projectId(101L)
                .projectName("OmniCommerce Hub")
                .requirementSpec(requirementSpec)
                .preferredBackend("Java 21 / Spring Boot 3")
                .preferredFrontend("React 19 / TypeScript")
                .preferredDatabase("PostgreSQL 16")
                .build();

        SystemArchitectOutput output = systemArchitectAgent.designArchitecture(input);

        assertNotNull(output);
        assertTrue(output.isSuccess(), "Agent 2 execution should succeed");
        assertNotNull(output.getArchitectureSpec());
        assertFalse(output.getArchitectureSpec().getDatabaseSchema().isEmpty());
        assertFalse(output.getArchitectureSpec().getApiEndpoints().isEmpty());
        assertNotNull(output.getSummary());
        assertEquals("SYSTEM_ARCHITECT", systemArchitectAgent.getAgentType());
    }

    @Test
    @DisplayName("Agent 3: BackendDeveloperAgent generates Spring Boot Java source files")
    void testAgent3BackendDeveloper() {
        SystemArchitectureSpec archSpec = SystemArchitectureSpec.builder()
                .architecturePattern("Hexagonal / Clean Layered")
                .databaseSchema(List.of(
                        SystemArchitectureSpec.TableSpec.builder()
                                .tableName("products")
                                .columns(List.of(
                                        new SystemArchitectureSpec.ColumnSpec("id", "BIGSERIAL", true, false, "PK"),
                                        new SystemArchitectureSpec.ColumnSpec("name", "VARCHAR(200)", false, false, "Product name")
                                ))
                                .build()
                ))
                .apiEndpoints(List.of(
                        new SystemArchitectureSpec.ApiEndpointSpec("GET", "/api/products", "List products", null, "List<Product>", "None")
                ))
                .build();

        BackendDeveloperInput input = BackendDeveloperInput.builder()
                .projectId(101L)
                .projectName("OmniCommerce Hub")
                .architectureSpec(archSpec)
                .build();

        BackendDeveloperOutput output = backendDeveloperAgent.generateBackend(input);

        assertNotNull(output);
        assertTrue(output.isSuccess(), "Agent 3 execution should succeed");
        assertNotNull(output.getFiles());
        assertFalse(output.getFiles().isEmpty(), "Generated backend files should not be empty");
        assertTrue(output.getFiles().stream().anyMatch(f -> f.getFilePath().endsWith(".java")));
        assertEquals("BACKEND_DEVELOPER", backendDeveloperAgent.getAgentType());
    }

    @Test
    @DisplayName("Agent 3: BackendDeveloperAgent handles targeted self-healing regeneration")
    void testAgent3TargetedSelfHealing() {
        BackendDeveloperInput input = BackendDeveloperInput.builder()
                .projectId(101L)
                .projectName("OmniCommerce Hub")
                .targetFilePath("backend/src/main/java/com/app/entity/Product.java")
                .compilerErrorDetails("error: cannot find symbol: class BigDecimal")
                .build();

        BackendDeveloperOutput output = backendDeveloperAgent.generateBackend(input);

        assertNotNull(output);
        assertTrue(output.isSuccess());
        assertEquals(1, output.getFiles().size());
        assertEquals("backend/src/main/java/com/app/entity/Product.java", output.getFiles().get(0).getFilePath());
    }

    @Test
    @DisplayName("Agent 4: FrontendDeveloperAgent generates React TypeScript components")
    void testAgent4FrontendDeveloper() {
        SystemArchitectureSpec archSpec = SystemArchitectureSpec.builder()
                .apiEndpoints(List.of(
                        new SystemArchitectureSpec.ApiEndpointSpec("GET", "/api/products", "List products", null, "Product[]", "None")
                ))
                .build();

        FrontendDeveloperInput input = FrontendDeveloperInput.builder()
                .projectId(101L)
                .projectName("OmniCommerce Hub")
                .architectureSpec(archSpec)
                .build();

        FrontendDeveloperOutput output = frontendDeveloperAgent.generateFrontend(input);

        assertNotNull(output);
        assertTrue(output.isSuccess(), "Agent 4 execution should succeed");
        assertNotNull(output.getFiles());
        assertFalse(output.getFiles().isEmpty(), "Generated frontend files should not be empty");
        assertTrue(output.getFiles().stream().anyMatch(f -> f.getFilePath().endsWith(".ts") || f.getFilePath().endsWith(".tsx")));
        assertEquals("FRONTEND_DEVELOPER", frontendDeveloperAgent.getAgentType());
    }

    @Test
    @DisplayName("Agent 5: TestingAgent generates automated tests and synthesizes test execution reports")
    void testAgent5TestingAgent() {
        TestingAgentInput input = TestingAgentInput.builder()
                .projectId(101L)
                .projectName("OmniCommerce Hub")
                .sourceFiles(List.of(
                        GeneratedSourceFile.builder()
                                .filePath("backend/src/main/java/com/app/service/ProductService.java")
                                .content("public class ProductService {}")
                                .language("java")
                                .build()
                ))
                .build();

        TestingAgentOutput output = testingAgent.runTests(input);

        assertNotNull(output);
        assertTrue(output.isSuccess(), "Agent 5 execution should succeed");
        assertNotNull(output.getReport());
        assertTrue(output.getReport().getTotalTests() > 0);
        assertNotNull(output.getTestFiles());
        assertEquals("TESTING_AGENT", testingAgent.getAgentType());
    }

    @Test
    @DisplayName("Agent 6: CodeReviewerAgent performs static code analysis and security auditing")
    void testAgent6CodeReviewer() {
        CodeReviewerInput input = CodeReviewerInput.builder()
                .projectId(101L)
                .projectName("OmniCommerce Hub")
                .sourceFiles(List.of(
                        GeneratedSourceFile.builder()
                                .filePath("backend/src/main/java/com/app/service/OrderService.java")
                                .content("public class OrderService { public void placeOrder() {} }")
                                .language("java")
                                .build()
                ))
                .build();

        CodeReviewerOutput output = codeReviewerAgent.reviewCode(input);

        assertNotNull(output);
        assertTrue(output.isSuccess(), "Agent 6 execution should succeed");
        assertNotNull(output.getReport());
        assertTrue(output.getReport().getQualityScore() >= 0 && output.getReport().getQualityScore() <= 100);
        assertNotNull(output.getReport().getIssues());
        assertEquals("CODE_REVIEWER", codeReviewerAgent.getAgentType());
    }

    @Test
    @DisplayName("Agent 7: DocumentationAgent synthesizes full documentation bundle")
    void testAgent7DocumentationAgent() {
        DocumentationAgentInput input = DocumentationAgentInput.builder()
                .projectId(101L)
                .projectName("OmniCommerce Hub")
                .requirementSpec(RequirementSpec.builder().summary("High scale e-commerce system").build())
                .architectureSpec(SystemArchitectureSpec.builder().architecturePattern("Hexagonal").build())
                .build();

        DocumentationAgentOutput output = documentationAgent.generateDocumentation(input);

        assertNotNull(output);
        assertTrue(output.isSuccess(), "Agent 7 execution should succeed");
        assertNotNull(output.getDocumentationBundle());
        assertNotNull(output.getDocumentationBundle().getReadme());
        assertNotNull(output.getDocumentationBundle().getArchitectureDoc());
        assertNotNull(output.getDocumentationBundle().getApiDocs());
        assertEquals("DOCUMENTATION_AGENT", documentationAgent.getAgentType());
    }

    @Test
    @DisplayName("Error Handling: Agents return graceful error results when invalid input is provided")
    void testAgentErrorHandling() {
        // Passing null requirement spec to SystemArchitectAgent should be handled gracefully
        SystemArchitectInput invalidInput = SystemArchitectInput.builder()
                .projectId(999L)
                .requirementSpec(null)
                .build();

        SystemArchitectOutput output = systemArchitectAgent.designArchitecture(invalidInput);

        assertNotNull(output);
        assertFalse(output.isSuccess(), "Agent should fail gracefully on null spec");
        assertNotNull(output.getErrorMessage());
        assertTrue(output.getErrorMessage().contains("RequirementSpec cannot be null"));
    }
}
