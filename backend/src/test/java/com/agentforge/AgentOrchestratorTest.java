package com.agentforge;

import com.agentforge.ai.agent.*;
import com.agentforge.ai.client.AiClientService;
import com.agentforge.ai.client.MockAiProvider;
import com.agentforge.ai.orchestrator.AgentOrchestrator;
import com.agentforge.ai.orchestrator.DiagnosticAnalyzer;
import com.agentforge.ai.springai.ChatClient;
import com.agentforge.ai.springai.SpringAiChatModel;
import com.agentforge.entity.*;
import com.agentforge.repository.*;
import com.agentforge.sandbox.DockerExecutionSandbox;
import com.agentforge.sandbox.ProcessIsolatedSandbox;
import com.agentforge.sandbox.SandboxManager;
import com.agentforge.service.SseNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentOrchestrator Central Multi-Agent Workflow Tests")
public class AgentOrchestratorTest {

    private ObjectMapper objectMapper;
    private Project testProject;
    private List<AgentExecution> savedExecutions;
    private List<GeneratedFile> savedFiles;
    private List<ProjectDocument> savedDocuments;
    private List<String> broadcastEvents;

    private AgentOrchestrator orchestrator;

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        savedExecutions = new ArrayList<>();
        savedFiles = new ArrayList<>();
        savedDocuments = new ArrayList<>();
        broadcastEvents = new ArrayList<>();

        testProject = Project.builder()
                .id(1L)
                .projectName("OmniShop")
                .requirements("Build an e-commerce platform with products, shopping cart, and orders.")
                .backendFramework("Spring Boot 3.3")
                .frontendFramework("React + TypeScript")
                .databaseType("PostgreSQL")
                .status("CREATED")
                .progressPercentage(0)
                .retryCount(0)
                .requirementsApproved(false)
                .architectureApproved(false)
                .build();

        ProjectRepository projectRepository = createProxy(ProjectRepository.class, (proxy, method, args) -> {
            if ("findById".equals(method.getName())) return Optional.of(testProject);
            if ("save".equals(method.getName())) return args[0];
            return null;
        });

        AgentRepository agentRepository = createProxy(AgentRepository.class, (proxy, method, args) -> {
            if ("findByAgentType".equals(method.getName())) {
                String type = (String) args[0];
                return Optional.of(Agent.builder().id(1L).agentType(type).agentName(type).build());
            }
            if ("save".equals(method.getName())) return args[0];
            return null;
        });

        AgentExecutionRepository executionRepository = createProxy(AgentExecutionRepository.class, (proxy, method, args) -> {
            if ("save".equals(method.getName())) {
                AgentExecution exec = (AgentExecution) args[0];
                savedExecutions.add(exec);
                return exec;
            }
            if ("findByProjectIdOrderByStartedAtAsc".equals(method.getName())) {
                return new ArrayList<>(savedExecutions);
            }
            return null;
        });

        GeneratedFileRepository fileRepository = createProxy(GeneratedFileRepository.class, (proxy, method, args) -> {
            if ("findMaxVersion".equals(method.getName())) return 1;
            if ("findByProjectIdAndFilePathAndIsLatestTrue".equals(method.getName())) return Optional.empty();
            if ("findByProjectIdAndIsLatestTrueOrderByFilePathAsc".equals(method.getName())) return new ArrayList<>(savedFiles);
            if ("save".equals(method.getName())) {
                GeneratedFile gf = (GeneratedFile) args[0];
                savedFiles.add(gf);
                return gf;
            }
            return null;
        });

        TestResultRepository testResultRepository = createProxy(TestResultRepository.class, (proxy, method, args) -> {
            if ("save".equals(method.getName())) return args[0];
            return null;
        });

        CodeReviewRepository codeReviewRepository = createProxy(CodeReviewRepository.class, (proxy, method, args) -> {
            if ("save".equals(method.getName())) return args[0];
            return null;
        });

        ProjectDocumentRepository documentRepository = createProxy(ProjectDocumentRepository.class, (proxy, method, args) -> {
            if ("findByProjectIdAndDocumentType".equals(method.getName())) return Optional.empty();
            if ("save".equals(method.getName())) {
                ProjectDocument doc = (ProjectDocument) args[0];
                savedDocuments.add(doc);
                return doc;
            }
            return null;
        });

        SseNotificationService sseService = new SseNotificationService() {
            @Override
            public void broadcastEvent(Long projectId, String eventName, Object data) {
                broadcastEvents.add(eventName + ":" + data);
            }
        };

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
        ChatClient chatClient = ChatClient.builder(chatModel).objectMapper(objectMapper).build();

        SandboxManager sandboxManager = new SandboxManager(
                new DockerExecutionSandbox(),
                new ProcessIsolatedSandbox()
        );

        RequirementAnalystAgent requirementAnalyst = new RequirementAnalystAgent(chatClient, objectMapper);
        SystemArchitectAgent systemArchitect = new SystemArchitectAgent(chatClient, objectMapper);
        BackendDeveloperAgent backendDeveloper = new BackendDeveloperAgent(chatClient, objectMapper);
        FrontendDeveloperAgent frontendDeveloper = new FrontendDeveloperAgent(chatClient, objectMapper);
        TestingAgent testingAgent = new TestingAgent(chatClient, sandboxManager, objectMapper);
        CodeReviewerAgent codeReviewer = new CodeReviewerAgent(chatClient, objectMapper);
        DocumentationAgent documentationAgent = new DocumentationAgent(chatClient, objectMapper);

        orchestrator = new AgentOrchestrator(
                projectRepository,
                agentRepository,
                executionRepository,
                fileRepository,
                testResultRepository,
                codeReviewRepository,
                documentRepository,
                requirementAnalyst,
                systemArchitect,
                backendDeveloper,
                frontendDeveloper,
                testingAgent,
                codeReviewer,
                documentationAgent,
                new DiagnosticAnalyzer(),
                sseService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Human-in-the-Loop Checkpoint 1: Workflow pauses at Requirements Approval")
    void testHumanInTheLoopRequirementsApprovalCheckpoint() {
        testProject.setRequirementsApproved(false);

        orchestrator.runWorkflowAsync(1L);

        assertEquals("PENDING_REQUIREMENTS_APPROVAL", testProject.getStatus());
        assertEquals("REQUIREMENT_ANALYST", testProject.getCurrentAgent());
        assertEquals(20, testProject.getProgressPercentage());
        assertTrue(savedExecutions.stream().anyMatch(e -> "REQUIREMENT_ANALYST".equals(e.getAgent().getAgentType())));
    }

    @Test
    @DisplayName("Human-in-the-Loop Checkpoint 2: Workflow resumes and pauses at Architecture Approval")
    void testHumanInTheLoopArchitectureApprovalCheckpoint() {
        testProject.setRequirementsApproved(true);
        testProject.setArchitectureApproved(false);

        orchestrator.runWorkflowAsync(1L);

        assertEquals("PENDING_ARCHITECTURE_APPROVAL", testProject.getStatus());
        assertEquals("SYSTEM_ARCHITECT", testProject.getCurrentAgent());
        assertEquals(40, testProject.getProgressPercentage());
        assertTrue(savedExecutions.stream().anyMatch(e -> "SYSTEM_ARCHITECT".equals(e.getAgent().getAgentType())));
    }

    @Test
    @DisplayName("Full Pipeline Completion: All 7 Agents execute when approvals are granted")
    void testFullPipelineExecution() {
        testProject.setRequirementsApproved(true);
        testProject.setArchitectureApproved(true);

        orchestrator.runWorkflowAsync(1L);

        assertEquals("COMPLETED", testProject.getStatus(), "Project workflow should complete successfully");
        assertEquals(100, testProject.getProgressPercentage());
        assertEquals("DOCUMENTATION_AGENT", testProject.getCurrentAgent());

        List<String> executedTypes = savedExecutions.stream().map(e -> e.getAgent().getAgentType()).toList();
        assertTrue(executedTypes.contains("REQUIREMENT_ANALYST"), "Requirement Analyst must execute");
        assertTrue(executedTypes.contains("SYSTEM_ARCHITECT"), "System Architect must execute");
        assertTrue(executedTypes.contains("BACKEND_DEVELOPER"), "Backend Developer must execute");
        assertTrue(executedTypes.contains("FRONTEND_DEVELOPER"), "Frontend Developer must execute");
        assertTrue(executedTypes.contains("TESTING_AGENT"), "Testing Agent must execute");
        assertTrue(executedTypes.contains("CODE_REVIEWER"), "Code Reviewer must execute");
        assertTrue(executedTypes.contains("DOCUMENTATION_AGENT"), "Documentation Agent must execute");

        assertFalse(savedFiles.isEmpty(), "Generated files must be saved");
        assertTrue(savedFiles.stream().anyMatch(f -> f.getFilePath().endsWith(".java")));
        assertTrue(savedFiles.stream().anyMatch(f -> f.getFilePath().endsWith(".ts") || f.getFilePath().endsWith(".tsx")));

        assertFalse(savedDocuments.isEmpty(), "Documents must be persisted");
        assertFalse(broadcastEvents.isEmpty(), "SSE telemetry must be broadcast");
    }

    @Test
    @DisplayName("Pause Workflow: Halts execution safely")
    void testPauseWorkflow() {
        orchestrator.pauseWorkflow(1L);

        assertEquals("PAUSED", testProject.getStatus());
    }

    @Test
    @DisplayName("Infinite Loop Prevention: Self-healing stops at max retries")
    void testInfiniteLoopPrevention() {
        ReflectionTestUtils.setField(orchestrator, "maxSelfHealingRetries", 2);

        testProject.setRequirementsApproved(true);
        testProject.setArchitectureApproved(true);
        testProject.setRetryCount(2);

        orchestrator.runWorkflowAsync(1L);

        assertEquals("COMPLETED", testProject.getStatus());
        assertTrue(testProject.getRetryCount() <= 2);
    }
}
