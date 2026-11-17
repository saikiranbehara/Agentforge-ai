package com.agentforge;

import com.agentforge.ai.agent.*;
import com.agentforge.ai.agent.dto.TestingAgentInput;
import com.agentforge.ai.agent.dto.TestingAgentOutput;
import com.agentforge.ai.client.AiClientService;
import com.agentforge.ai.client.MockAiProvider;
import com.agentforge.ai.model.*;
import com.agentforge.ai.orchestrator.AgentOrchestrator;
import com.agentforge.ai.orchestrator.DiagnosticAnalyzer;
import com.agentforge.ai.springai.ChatClient;
import com.agentforge.ai.springai.SpringAiChatModel;
import com.agentforge.entity.*;
import com.agentforge.repository.*;
import com.agentforge.sandbox.ExecutionSandbox;
import com.agentforge.sandbox.SandboxManager;
import com.agentforge.sandbox.SandboxResult;
import com.agentforge.service.SseNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Autonomous Code Generation, Testing, and Self-Healing Debugging Tests")
public class AutonomousDebuggingWorkflowTest {

    private DiagnosticAnalyzer diagnosticAnalyzer;
    private ObjectMapper objectMapper;
    private Project testProject;
    private List<GeneratedFile> savedFiles;
    private List<AgentExecution> savedExecutions;
    private List<TestResult> savedTestResults;
    private List<CodeReview> savedReviews;
    private ChatClient chatClient;
    private SseNotificationService sseService;

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @BeforeEach
    void setUp() {
        diagnosticAnalyzer = new DiagnosticAnalyzer();
        objectMapper = new ObjectMapper();
        savedFiles = new ArrayList<>();
        savedExecutions = new ArrayList<>();
        savedTestResults = new ArrayList<>();
        savedReviews = new ArrayList<>();

        testProject = Project.builder()
                .id(100L)
                .projectName("OmniCommerce")
                .requirements("Build an e-commerce platform with products, shopping cart, and orders.")
                .backendFramework("Spring Boot 3.3")
                .frontendFramework("React + TypeScript")
                .databaseType("PostgreSQL")
                .status("CREATED")
                .progressPercentage(0)
                .retryCount(0)
                .requirementsApproved(true)
                .architectureApproved(true)
                .build();

        MockAiProvider mockAiProvider = new MockAiProvider(objectMapper);
        AiClientService aiClientService = new AiClientService(
                objectMapper,
                mockAiProvider,
                "",
                "",
                "MOCK",
                "gemini-1.5-pro",
                true
        );
        SpringAiChatModel chatModel = new SpringAiChatModel(aiClientService);
        chatClient = ChatClient.builder(chatModel).objectMapper(objectMapper).build();

        sseService = new SseNotificationService() {
            @Override
            public void broadcastEvent(Long projectId, String eventName, Object data) {
                // no-op for tests
            }
        };
    }

    @Test
    @DisplayName("DiagnosticAnalyzer: Accurately identifies responsible Java file, line, and backend agent from compiler error")
    void testDiagnosticAnalyzerBackendCompilerError() {
        List<GeneratedSourceFile> availableFiles = List.of(
                GeneratedSourceFile.builder()
                        .filePath("backend/src/main/java/com/app/service/ProductService.java")
                        .language("java")
                        .build(),
                GeneratedSourceFile.builder()
                        .filePath("frontend/src/components/ProductCard.tsx")
                        .language("typescript")
                        .build()
        );

        String compilerStderr = """
                [ERROR] /workspace/backend/src/main/java/com/app/service/ProductService.java:[42,18] cannot find symbol
                [ERROR]   symbol:   variable price
                [ERROR]   location: class com.app.service.ProductService
                """;

        SandboxResult sandboxResult = SandboxResult.builder()
                .success(false)
                .exitCode(1)
                .stderr(compilerStderr)
                .executionTimeMs(1500)
                .isolationTier("DOCKER_CONTAINER")
                .build();

        FailureDiagnostic diagnostic = diagnosticAnalyzer.analyzeFailure(
                sandboxResult, null, null, availableFiles
        );

        assertTrue(diagnostic.isHasFailures(), "Diagnostic must detect failure");
        assertEquals("backend/src/main/java/com/app/service/ProductService.java", diagnostic.getResponsibleFilePath());
        assertEquals("BACKEND_DEVELOPER", diagnostic.getResponsibleAgentType());
        assertEquals(42, diagnostic.getLineNumber());
        assertTrue(diagnostic.getErrorSummary().contains("cannot find symbol"));
    }

    @Test
    @DisplayName("DiagnosticAnalyzer: Accurately routes TypeScript compiler errors to Frontend Developer Agent")
    void testDiagnosticAnalyzerFrontendCompilerError() {
        List<GeneratedSourceFile> availableFiles = List.of(
                GeneratedSourceFile.builder()
                        .filePath("backend/src/main/java/com/app/service/ProductService.java")
                        .language("java")
                        .build(),
                GeneratedSourceFile.builder()
                        .filePath("frontend/src/components/ProductCard.tsx")
                        .language("typescript")
                        .build()
        );

        String tsStderr = """
                frontend/src/components/ProductCard.tsx:28:15 - error TS2322: Type 'number' is not assignable to type 'string'.
                """;

        SandboxResult sandboxResult = SandboxResult.builder()
                .success(false)
                .exitCode(2)
                .stderr(tsStderr)
                .executionTimeMs(800)
                .isolationTier("DOCKER_CONTAINER")
                .build();

        FailureDiagnostic diagnostic = diagnosticAnalyzer.analyzeFailure(
                sandboxResult, null, null, availableFiles
        );

        assertTrue(diagnostic.isHasFailures());
        assertEquals("frontend/src/components/ProductCard.tsx", diagnostic.getResponsibleFilePath());
        assertEquals("FRONTEND_DEVELOPER", diagnostic.getResponsibleAgentType());
        assertEquals(28, diagnostic.getLineNumber());
    }

    @Test
    @DisplayName("DiagnosticAnalyzer: Infers responsible service class from failed JUnit test class name")
    void testDiagnosticAnalyzerInfersTargetFromFailedTest() {
        List<GeneratedSourceFile> availableFiles = List.of(
                GeneratedSourceFile.builder()
                        .filePath("backend/src/main/java/com/app/service/OrderService.java")
                        .language("java")
                        .build()
        );

        TestExecutionReport report = TestExecutionReport.builder()
                .totalTests(5)
                .passedCount(4)
                .failedCount(1)
                .allPassed(false)
                .testCases(List.of(
                        TestExecutionReport.TestCaseResult.builder()
                                .testClass("com.app.service.OrderServiceTest")
                                .testName("testProcessOrderWithInsufficientStock")
                                .status("FAILED")
                                .errorMessage("Expected InsufficientStockException but was NullPointerException")
                                .build()
                ))
                .build();

        FailureDiagnostic diagnostic = diagnosticAnalyzer.analyzeFailure(
                null, report, null, availableFiles
        );

        assertTrue(diagnostic.isHasFailures());
        assertEquals("backend/src/main/java/com/app/service/OrderService.java", diagnostic.getResponsibleFilePath());
        assertEquals("BACKEND_DEVELOPER", diagnostic.getResponsibleAgentType());
        assertTrue(diagnostic.getErrorSummary().contains("Expected InsufficientStockException"));
    }

    @Test
    @DisplayName("TestingAgent: Never simulates success when sandbox exit code != 0 or errors occur")
    void testTestingAgentHonorsSandboxFailuresWithoutSimulation() {
        ExecutionSandbox mockSandbox = new ExecutionSandbox() {
            @Override
            public boolean isAvailable() { return true; }

            @Override
            public String getProviderName() { return "MOCK_DOCKER_CONTAINER"; }

            @Override
            public SandboxResult executeCode(Long projectId, List<GeneratedSourceFile> files, String command, long timeoutSeconds) {
                return SandboxResult.builder()
                        .success(false)
                        .exitCode(1)
                        .stdout("[INFO] Compiling sources...")
                        .stderr("[ERROR] Compilation failure: cannot find symbol")
                        .executionTimeMs(1200)
                        .isolationTier("DOCKER_CONTAINER")
                        .build();
            }
        };

        SandboxManager sandboxManager = new SandboxManager(null, null) {
            @Override
            public SandboxResult runInSandbox(Long projectId, List<GeneratedSourceFile> files, String command, long timeoutSeconds) {
                return mockSandbox.executeCode(projectId, files, command, timeoutSeconds);
            }
        };

        TestingAgent testingAgent = new TestingAgent(chatClient, sandboxManager, objectMapper);

        TestingAgentInput input = TestingAgentInput.builder()
                .projectId(100L)
                .projectName("OmniCommerce")
                .sourceFiles(List.of(GeneratedSourceFile.builder()
                        .filePath("backend/src/main/java/com/app/service/ProductService.java")
                        .content("public class ProductService {}")
                        .language("java")
                        .build()))
                .build();

        TestingAgentOutput output = testingAgent.runTests(input);

        assertTrue(output.isSuccess(), "Agent invocation must succeed");
        assertNotNull(output.getReport());
        // CRITICAL: Must not simulate success
        assertFalse(output.getReport().isAllPassed(), "Must reflect actual failure when sandbox exit code != 0");
        assertTrue(output.getReport().getFailedCount() > 0, "Failed count must be greater than zero");
        assertNotNull(output.getSandboxResult());
        assertEquals(1, output.getSandboxResult().getExitCode());
        assertTrue(output.getReport().getExecutionLogs().contains("[ERROR] Compilation failure"));
    }

    @Test
    @DisplayName("Version History: Saves every code version, increments version number, and preserves old versions")
    void testVersionHistoryIncrementationAndPreservation() {
        Map<String, List<GeneratedFile>> fileStore = new HashMap<>();

        GeneratedFileRepository fileRepository = createProxy(GeneratedFileRepository.class, (proxy, method, args) -> {
            if ("findMaxVersion".equals(method.getName())) {
                String path = (String) args[1];
                List<GeneratedFile> versions = fileStore.getOrDefault(path, List.of());
                return versions.stream().mapToInt(GeneratedFile::getVersion).max().orElse(0);
            }
            if ("findByProjectIdAndFilePathAndIsLatestTrue".equals(method.getName())) {
                String path = (String) args[1];
                List<GeneratedFile> versions = fileStore.getOrDefault(path, List.of());
                return versions.stream().filter(GeneratedFile::getIsLatest).findFirst();
            }
            if ("save".equals(method.getName())) {
                GeneratedFile gf = (GeneratedFile) args[0];
                savedFiles.add(gf);
                List<GeneratedFile> list = fileStore.computeIfAbsent(gf.getFilePath(), k -> new ArrayList<>());
                if (!list.contains(gf)) {
                    list.add(gf);
                }
                return gf;
            }
            return null;
        });

        // Setup orchestrator with fileRepository
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                null, null, null, fileRepository, null, null, null,
                null, null, null, null, null, null, null,
                diagnosticAnalyzer, sseService, objectMapper
        );

        String targetPath = "backend/src/main/java/com/app/service/ProductService.java";

        // Version 1: Initial generation
        GeneratedSourceFile fileV1 = GeneratedSourceFile.builder()
                .filePath(targetPath)
                .content("public class ProductService { // v1 with syntax bug }")
                .language("java")
                .build();
        orchestrator.saveGeneratedFiles(testProject, List.of(fileV1), "BACKEND_DEVELOPER");

        assertEquals(1, fileStore.get(targetPath).size());
        assertEquals(1, fileStore.get(targetPath).get(0).getVersion());
        assertTrue(fileStore.get(targetPath).get(0).getIsLatest());

        // Version 2: Self-healed generation
        GeneratedSourceFile fileV2 = GeneratedSourceFile.builder()
                .filePath(targetPath)
                .content("public class ProductService { // v2 corrected and compiled }")
                .language("java")
                .build();
        orchestrator.saveGeneratedFiles(testProject, List.of(fileV2), "BACKEND_DEVELOPER");

        List<GeneratedFile> history = fileStore.get(targetPath);
        assertEquals(2, history.size(), "Both versions must be preserved in history");

        GeneratedFile v1 = history.stream().filter(f -> f.getVersion() == 1).findFirst().orElseThrow();
        GeneratedFile v2 = history.stream().filter(f -> f.getVersion() == 2).findFirst().orElseThrow();

        assertFalse(v1.getIsLatest(), "Version 1 must now be marked as not latest");
        assertTrue(v2.getIsLatest(), "Version 2 must be marked as latest");
        assertEquals("public class ProductService { // v2 corrected and compiled }", v2.getFileContent());
    }

    @Test
    @DisplayName("Self-Healing Circuit Breaker: Prevents infinite loops and halts at max retries")
    void testSelfHealingLoopCircuitBreaker() {
        ProjectRepository projectRepository = createProxy(ProjectRepository.class, (proxy, method, args) -> {
            if ("findById".equals(method.getName())) return Optional.of(testProject);
            if ("save".equals(method.getName())) return args[0];
            return null;
        });

        AgentRepository agentRepository = createProxy(AgentRepository.class, (proxy, method, args) -> {
            if ("findByAgentType".equals(method.getName())) {
                return Optional.of(Agent.builder().id(1L).agentType((String) args[0]).build());
            }
            if ("save".equals(method.getName())) return args[0];
            return null;
        });

        AgentExecutionRepository executionRepository = createProxy(AgentExecutionRepository.class, (proxy, method, args) -> {
            if ("save".equals(method.getName())) return args[0];
            if ("findByProjectIdOrderByStartedAtAsc".equals(method.getName())) return List.of();
            return null;
        });

        GeneratedFileRepository fileRepository = createProxy(GeneratedFileRepository.class, (proxy, method, args) -> {
            if ("findMaxVersion".equals(method.getName())) return 1;
            if ("findByProjectIdAndFilePathAndIsLatestTrue".equals(method.getName())) return Optional.empty();
            if ("findByProjectIdAndIsLatestTrueOrderByFilePathAsc".equals(method.getName())) return List.of();
            if ("save".equals(method.getName())) return args[0];
            return null;
        });

        TestResultRepository testResultRepository = createProxy(TestResultRepository.class, (proxy, method, args) -> null);
        CodeReviewRepository codeReviewRepository = createProxy(CodeReviewRepository.class, (proxy, method, args) -> null);
        ProjectDocumentRepository documentRepository = createProxy(ProjectDocumentRepository.class, (proxy, method, args) -> {
            if ("findByProjectIdAndDocumentType".equals(method.getName())) return Optional.empty();
            if ("save".equals(method.getName())) return args[0];
            return null;
        });

        RequirementAnalystAgent reqAgent = new RequirementAnalystAgent(chatClient, objectMapper);
        SystemArchitectAgent archAgent = new SystemArchitectAgent(chatClient, objectMapper);
        BackendDeveloperAgent beAgent = new BackendDeveloperAgent(chatClient, objectMapper);
        FrontendDeveloperAgent feAgent = new FrontendDeveloperAgent(chatClient, objectMapper);

        // Always-failing TestingAgent (simulates persistent failure)
        ExecutionSandbox mockFailingSandbox = new ExecutionSandbox() {
            @Override
            public boolean isAvailable() { return true; }

            @Override
            public String getProviderName() { return "MOCK_FAILING_DOCKER"; }

            @Override
            public SandboxResult executeCode(Long projectId, List<GeneratedSourceFile> files, String command, long timeoutSeconds) {
                return SandboxResult.builder()
                        .success(false)
                        .exitCode(1)
                        .stderr("[ERROR] Persistent compilation error in ProductService.java:10")
                        .executionTimeMs(500)
                        .build();
            }
        };

        SandboxManager failingSandboxManager = new SandboxManager(null, null) {
            @Override
            public SandboxResult runInSandbox(Long projectId, List<GeneratedSourceFile> files, String command, long timeoutSeconds) {
                return mockFailingSandbox.executeCode(projectId, files, command, timeoutSeconds);
            }
        };

        TestingAgent testingAgent = new TestingAgent(chatClient, failingSandboxManager, objectMapper);
        CodeReviewerAgent codeReviewer = new CodeReviewerAgent(chatClient, objectMapper);
        DocumentationAgent docAgent = new DocumentationAgent(chatClient, objectMapper);

        AgentOrchestrator orchestrator = new AgentOrchestrator(
                projectRepository,
                agentRepository,
                executionRepository,
                fileRepository,
                testResultRepository,
                codeReviewRepository,
                documentRepository,
                reqAgent,
                archAgent,
                beAgent,
                feAgent,
                testingAgent,
                codeReviewer,
                docAgent,
                diagnosticAnalyzer,
                sseService,
                objectMapper
        );

        ReflectionTestUtils.setField(orchestrator, "requireRequirementsApproval", false);
        ReflectionTestUtils.setField(orchestrator, "requireArchitectureApproval", false);
        ReflectionTestUtils.setField(orchestrator, "maxSelfHealingRetries", 2);

        // Run workflow synchronously
        orchestrator.runWorkflowAsync(100L).join();

        // Verify retries reached limit of 2 and safely terminated without infinite loop
        assertEquals(2, testProject.getRetryCount(), "Self-healing retry count must cap exactly at maxSelfHealingRetries");
    }
}
