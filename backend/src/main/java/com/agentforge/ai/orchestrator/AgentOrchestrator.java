package com.agentforge.ai.orchestrator;

import com.agentforge.ai.agent.*;
import com.agentforge.ai.agent.dto.*;
import com.agentforge.ai.model.*;
import com.agentforge.entity.*;
import com.agentforge.repository.*;
import com.agentforge.service.SseNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central Multi-Agent Orchestrator for AgentForge.
 * Coordinates all 7 AI agents in a stateful Directed Acyclic Graph (DAG) with:
 * - Structured DTO passing between agents
 * - Persistent execution history in PostgreSQL
 * - Human-in-the-loop approval checkpoints (Requirements & Architecture)
 * - Self-healing code repair feedback loop with configurable retries
 * - Infinite loop guards & concurrency locks
 * - Real-time Server-Sent Events (SSE) telemetry broadcast to React dashboard
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final ProjectRepository projectRepository;
    private final AgentRepository agentRepository;
    private final AgentExecutionRepository executionRepository;
    private final GeneratedFileRepository fileRepository;
    private final TestResultRepository testResultRepository;
    private final CodeReviewRepository codeReviewRepository;
    private final ProjectDocumentRepository documentRepository;

    private final RequirementAnalystAgent requirementAnalyst;
    private final SystemArchitectAgent systemArchitect;
    private final BackendDeveloperAgent backendDeveloper;
    private final FrontendDeveloperAgent frontendDeveloper;
    private final TestingAgent testingAgent;
    private final CodeReviewerAgent codeReviewer;
    private final DocumentationAgent documentationAgent;
    private final DiagnosticAnalyzer diagnosticAnalyzer;

    private final SseNotificationService sseService;
    private final ObjectMapper objectMapper;

    @Value("${agentforge.orchestrator.max-self-healing-retries:2}")
    private int maxSelfHealingRetries = 2;

    @Value("${agentforge.orchestrator.max-agent-retries:2}")
    private int maxAgentRetries = 2;

    @Value("${agentforge.orchestrator.require-requirements-approval:true}")
    private boolean requireRequirementsApproval = true;

    @Value("${agentforge.orchestrator.require-architecture-approval:true}")
    private boolean requireArchitectureApproval = true;

    private static final int MAX_GLOBAL_CIRCUIT_BREAKER_STEPS = 25;

    // Concurrency guard: Ensures only one workflow execution thread per project
    private final Set<Long> runningProjects = ConcurrentHashMap.newKeySet();
    private final Set<Long> pausedProjects = ConcurrentHashMap.newKeySet();

    /**
     * Entry point to launch or resume multi-agent workflow asynchronously.
     */
    @Async
    public CompletableFuture<Void> runWorkflowAsync(Long projectId) {
        if (!runningProjects.add(projectId)) {
            log.warn("[AgentOrchestrator] Project {} is already actively running. Skipping duplicate execution request.", projectId);
            return CompletableFuture.completedFuture(null);
        }

        pausedProjects.remove(projectId);

        try {
            executeWorkflowPipeline(projectId);
        } catch (Exception e) {
            log.error("[AgentOrchestrator] Fatal unhandled error executing workflow for project {}", projectId, e);
            markProjectFailed(projectId, "Pipeline error: " + e.getMessage());
        } finally {
            runningProjects.remove(projectId);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Pause execution of a running project workflow.
     */
    public void pauseWorkflow(Long projectId) {
        log.info("[AgentOrchestrator] Pausing workflow for project: {}", projectId);
        pausedProjects.add(projectId);
        projectRepository.findById(projectId).ifPresent(p -> {
            p.setStatus("PAUSED");
            projectRepository.save(p);
            broadcastStatus(p, p.getCurrentAgent() != null ? p.getCurrentAgent() : "ORCHESTRATOR", "PAUSED", p.getProgressPercentage(), "Workflow paused by user.");
        });
    }

    /**
     * Central state machine execution loop.
     */
    private void executeWorkflowPipeline(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        log.info("[AgentOrchestrator] Starting multi-agent pipeline for project {} ('{}')", projectId, project.getProjectName());

        AgentContext context = buildAgentContext(project);
        int totalStepCounter = 0;

        // =========================================================================
        // AGENT 1: REQUIREMENT ANALYST
        // =========================================================================
        if (context.getRequirementSpec() == null) {
            if (checkIfPaused(projectId)) return;

            updateProjectState(project, "ANALYZING_REQUIREMENTS", "REQUIREMENT_ANALYST", 10);
            broadcastWorkflow(project, "REQUIREMENT_ANALYST", "RUNNING", 10, "Analyzing natural language requirements and user stories...");

            RequirementAnalysisInput input = RequirementAnalysisInput.builder()
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .naturalLanguageRequirements(project.getRequirements())
                    .techStackPreference(String.format("Backend: %s, Frontend: %s, Database: %s",
                            project.getBackendFramework(), project.getFrontendFramework(), project.getDatabaseType()))
                    .feedbackMessage(context.getFeedbackMessage())
                    .build();

            RequirementAnalysisOutput output = executeWithRetry("REQUIREMENT_ANALYST", () -> requirementAnalyst.analyzeRequirements(input));

            if (!output.isSuccess() || output.getRequirementSpec() == null) {
                markProjectFailed(project.getId(), "Requirement analysis failed: " + output.getErrorMessage());
                return;
            }

            context.setRequirementSpec(output.getRequirementSpec());
            persistExecutionHistory(project, "REQUIREMENT_ANALYST", "Requirement Analyst", input, output, output.isSuccess(),
                    output.getSummary(), output.getExecutionDurationMs(), output.getErrorMessage());

            broadcastWorkflow(project, "REQUIREMENT_ANALYST", "COMPLETED", 20, output.getSummary());

            // Checkpoint 1: Human-in-the-Loop Requirements Approval
            if (requireRequirementsApproval && !Boolean.TRUE.equals(project.getRequirementsApproved())) {
                log.info("[AgentOrchestrator] Halting at Human-in-the-Loop Checkpoint: Requirements Approval needed for project {}", project.getId());
                updateProjectState(project, "PENDING_REQUIREMENTS_APPROVAL", "REQUIREMENT_ANALYST", 20);
                broadcastStatus(project, "REQUIREMENT_ANALYST", "WAITING_APPROVAL", 20, "Requirements synthesized. Awaiting user approval to proceed with Architecture.");
                return; // Stop pipeline until human approves
            }
        }

        // =========================================================================
        // AGENT 2: SYSTEM ARCHITECT
        // =========================================================================
        if (context.getArchitectureSpec() == null) {
            if (checkIfPaused(projectId)) return;

            updateProjectState(project, "DESIGNING_ARCHITECTURE", "SYSTEM_ARCHITECT", 30);
            broadcastWorkflow(project, "SYSTEM_ARCHITECT", "RUNNING", 30, "Designing relational database schemas, REST APIs, and architecture...");

            SystemArchitectInput input = SystemArchitectInput.builder()
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .requirementSpec(context.getRequirementSpec())
                    .preferredBackend(project.getBackendFramework())
                    .preferredFrontend(project.getFrontendFramework())
                    .preferredDatabase(project.getDatabaseType())
                    .feedbackMessage(context.getFeedbackMessage())
                    .build();

            SystemArchitectOutput output = executeWithRetry("SYSTEM_ARCHITECT", () -> systemArchitect.designArchitecture(input));

            if (!output.isSuccess() || output.getArchitectureSpec() == null) {
                markProjectFailed(project.getId(), "System Architecture design failed: " + output.getErrorMessage());
                return;
            }

            context.setArchitectureSpec(output.getArchitectureSpec());
            persistExecutionHistory(project, "SYSTEM_ARCHITECT", "System Architect", input, output, output.isSuccess(),
                    output.getSummary(), output.getExecutionDurationMs(), output.getErrorMessage());

            broadcastWorkflow(project, "SYSTEM_ARCHITECT", "COMPLETED", 40, output.getSummary());

            // Checkpoint 2: Human-in-the-Loop Architecture Approval
            if (requireArchitectureApproval && !Boolean.TRUE.equals(project.getArchitectureApproved())) {
                log.info("[AgentOrchestrator] Halting at Human-in-the-Loop Checkpoint: Architecture Approval needed for project {}", project.getId());
                updateProjectState(project, "PENDING_ARCHITECTURE_APPROVAL", "SYSTEM_ARCHITECT", 40);
                broadcastStatus(project, "SYSTEM_ARCHITECT", "WAITING_APPROVAL", 40, "Architecture and database schema designed. Awaiting user approval to generate code.");
                return; // Stop pipeline until human approves
            }
        }

        // =========================================================================
        // AGENT 3 & 4: PARALLEL BACKEND & FRONTEND CODE GENERATION
        // =========================================================================
        if (checkIfPaused(projectId)) return;

        // Stage 3: Backend Developer
        updateProjectState(project, "GENERATING_BACKEND", "BACKEND_DEVELOPER", 50);
        broadcastWorkflow(project, "BACKEND_DEVELOPER", "RUNNING", 50, "Generating Spring Boot 3.3 / Java 21 entities, repositories, services, and controllers...");

        BackendDeveloperInput beInput = BackendDeveloperInput.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .requirementSpec(context.getRequirementSpec())
                .architectureSpec(context.getArchitectureSpec())
                .build();

        BackendDeveloperOutput beOutput = executeWithRetry("BACKEND_DEVELOPER", () -> backendDeveloper.generateBackend(beInput));

        if (!beOutput.isSuccess()) {
            markProjectFailed(project.getId(), "Backend code generation failed: " + beOutput.getErrorMessage());
            return;
        }

        saveGeneratedFiles(project, beOutput.getFiles(), "BACKEND_DEVELOPER");
        persistExecutionHistory(project, "BACKEND_DEVELOPER", "Backend Developer", beInput, beOutput, beOutput.isSuccess(),
                beOutput.getSummary(), beOutput.getExecutionDurationMs(), beOutput.getErrorMessage());

        broadcastWorkflow(project, "BACKEND_DEVELOPER", "COMPLETED", 60, beOutput.getSummary());

        if (checkIfPaused(projectId)) return;

        // Stage 4: Frontend Developer
        updateProjectState(project, "GENERATING_FRONTEND", "FRONTEND_DEVELOPER", 65);
        broadcastWorkflow(project, "FRONTEND_DEVELOPER", "RUNNING", 65, "Generating React 18 / TypeScript / Tailwind CSS pages and API client services...");

        FrontendDeveloperInput feInput = FrontendDeveloperInput.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .requirementSpec(context.getRequirementSpec())
                .architectureSpec(context.getArchitectureSpec())
                .build();

        FrontendDeveloperOutput feOutput = executeWithRetry("FRONTEND_DEVELOPER", () -> frontendDeveloper.generateFrontend(feInput));

        if (!feOutput.isSuccess()) {
            markProjectFailed(project.getId(), "Frontend code generation failed: " + feOutput.getErrorMessage());
            return;
        }

        saveGeneratedFiles(project, feOutput.getFiles(), "FRONTEND_DEVELOPER");
        persistExecutionHistory(project, "FRONTEND_DEVELOPER", "Frontend Developer", feInput, feOutput, feOutput.isSuccess(),
                feOutput.getSummary(), feOutput.getExecutionDurationMs(), feOutput.getErrorMessage());

        broadcastWorkflow(project, "FRONTEND_DEVELOPER", "COMPLETED", 70, feOutput.getSummary());

        // Refresh latest files from DB into context
        List<GeneratedFile> latestFiles = fileRepository.findByProjectIdAndIsLatestTrueOrderByFilePathAsc(project.getId());
        context.setGeneratedFiles(latestFiles);

        List<GeneratedSourceFile> sourceFiles = latestFiles.stream()
                .map(f -> GeneratedSourceFile.builder()
                        .filePath(f.getFilePath())
                        .content(f.getFileContent())
                        .language(f.getLanguage())
                        .build())
                .toList();

        // =========================================================================
        // AGENT 5: TESTING AGENT (SANDBOX TEST RUNNER)
        // =========================================================================
        if (checkIfPaused(projectId)) return;

        updateProjectState(project, "TESTING", "TESTING_AGENT", 75);
        broadcastWorkflow(project, "TESTING_AGENT", "RUNNING", 75, "Generating automated test suites and executing in isolated sandbox...");

        TestingAgentInput testInput = TestingAgentInput.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .requirementSpec(context.getRequirementSpec())
                .architectureSpec(context.getArchitectureSpec())
                .sourceFiles(sourceFiles)
                .build();

        TestingAgentOutput testOutput = executeWithRetry("TESTING_AGENT", () -> testingAgent.runTests(testInput));

        if (testOutput.isSuccess() && testOutput.getReport() != null) {
            context.setTestReport(testOutput.getReport());
            if (testOutput.getTestFiles() != null && !testOutput.getTestFiles().isEmpty()) {
                saveGeneratedFiles(project, testOutput.getTestFiles(), "TESTING_AGENT");
            }
            persistTestResults(project, testOutput.getReport());
        }

        persistExecutionHistory(project, "TESTING_AGENT", "Testing Agent", testInput, testOutput, testOutput.isSuccess(),
                testOutput.getSummary(), testOutput.getExecutionDurationMs(), testOutput.getErrorMessage());

        broadcastWorkflow(project, "TESTING_AGENT", "COMPLETED", 80, testOutput.getSummary());

        // =========================================================================
        // AGENT 6: CODE REVIEWER & AUTONOMOUS SELF-HEALING LOOP
        // =========================================================================
        if (checkIfPaused(projectId)) return;

        updateProjectState(project, "REVIEWING", "CODE_REVIEWER", 85);
        broadcastWorkflow(project, "CODE_REVIEWER", "RUNNING", 85, "Auditing code quality, OWASP security vulnerabilities, and architecture alignment...");

        CodeReviewerInput reviewInput = CodeReviewerInput.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .requirementSpec(context.getRequirementSpec())
                .architectureSpec(context.getArchitectureSpec())
                .sourceFiles(sourceFiles)
                .testReport(context.getTestReport())
                .build();

        CodeReviewerOutput reviewOutput = executeWithRetry("CODE_REVIEWER", () -> codeReviewer.reviewCode(reviewInput));

        if (reviewOutput.isSuccess() && reviewOutput.getReport() != null) {
            context.setReviewReport(reviewOutput.getReport());
            persistCodeReviews(project, reviewOutput.getReport());
        }

        persistExecutionHistory(project, "CODE_REVIEWER", "Code Reviewer", reviewInput, reviewOutput, reviewOutput.isSuccess(),
                reviewOutput.getSummary(), reviewOutput.getExecutionDurationMs(), reviewOutput.getErrorMessage());

        broadcastWorkflow(project, "CODE_REVIEWER", "COMPLETED", 90, reviewOutput.getSummary());

        // Autonomous Self-Healing Retry Loop (Guarded against infinite cycles)
        boolean hasTestFailures = context.getTestReport() != null && !context.getTestReport().isAllPassed();
        boolean hasReviewRejection = context.getReviewReport() != null && !context.getReviewReport().isApproved();

        int currentRetry = project.getRetryCount() != null ? project.getRetryCount() : 0;
        com.agentforge.sandbox.SandboxResult lastSandboxResult = testOutput.getSandboxResult();

        while ((hasTestFailures || hasReviewRejection) && currentRetry < maxSelfHealingRetries) {
            currentRetry++;
            totalStepCounter++;

            // Global infinite loop prevention circuit-breaker
            if (totalStepCounter > MAX_GLOBAL_CIRCUIT_BREAKER_STEPS) {
                log.warn("[AgentOrchestrator] Global circuit-breaker step limit ({}) exceeded. Halting self-healing loop.", MAX_GLOBAL_CIRCUIT_BREAKER_STEPS);
                break;
            }

            log.info("[AgentOrchestrator] Triggering Self-Healing Loop iteration {}/{} for project {}",
                    currentRetry, maxSelfHealingRetries, project.getId());

            project.setRetryCount(currentRetry);

            // 1 & 2: Capture error logs and identify the responsible files and agent
            FailureDiagnostic diagnostic = diagnosticAnalyzer.analyzeFailure(
                    lastSandboxResult, context.getTestReport(), context.getReviewReport(), sourceFiles
            );

            String responsibleAgent = diagnostic.getResponsibleAgentType();
            String targetFile = diagnostic.getResponsibleFilePath();
            int lineNumber = diagnostic.getLineNumber();
            String errorSummary = diagnostic.getErrorSummary();

            log.info("[AgentOrchestrator] Self-healing diagnostic: Responsible Agent={}, File={}, Line={}, Error='{}'",
                    responsibleAgent, targetFile, lineNumber, errorSummary);

            updateProjectState(project, "SELF_HEALING", responsibleAgent, 82);
            broadcastStatus(project, responsibleAgent, "RUNNING", 82,
                    String.format("Self-Healing Iteration %d/%d: %s repairing %s (Line %d)...",
                            currentRetry, maxSelfHealingRetries, responsibleAgent,
                            targetFile != null ? targetFile : "source files", lineNumber));

            // 3 & 4: Send errors to appropriate developer agent and generate code corrections
            if ("FRONTEND_DEVELOPER".equalsIgnoreCase(responsibleAgent)) {
                FrontendDeveloperInput repairInput = FrontendDeveloperInput.builder()
                        .projectId(project.getId())
                        .projectName(project.getProjectName())
                        .requirementSpec(context.getRequirementSpec())
                        .architectureSpec(context.getArchitectureSpec())
                        .targetFilePath(targetFile)
                        .feedbackMessage(String.format("Self-healing repair for %s: %s\nFull error logs:\n%s",
                                targetFile, errorSummary, diagnostic.getFullErrorLogs()))
                        .build();

                FrontendDeveloperOutput feRepairOutput = frontendDeveloper.generateFrontend(repairInput);
                if (feRepairOutput.isSuccess() && feRepairOutput.getFiles() != null) {
                    saveGeneratedFiles(project, feRepairOutput.getFiles(), "FRONTEND_DEVELOPER");
                }
            } else {
                BackendDeveloperInput repairInput = BackendDeveloperInput.builder()
                        .projectId(project.getId())
                        .projectName(project.getProjectName())
                        .requirementSpec(context.getRequirementSpec())
                        .architectureSpec(context.getArchitectureSpec())
                        .targetFilePath(targetFile)
                        .compilerErrorDetails(String.format("Diagnostic for %s at line %d: %s\n\nFull logs:\n%s",
                                targetFile, lineNumber, errorSummary, diagnostic.getFullErrorLogs()))
                        .feedbackMessage("Fix compiler/test errors and maintain architectural integrity.")
                        .build();

                BackendDeveloperOutput beRepairOutput = backendDeveloper.generateBackend(repairInput);
                if (beRepairOutput.isSuccess() && beRepairOutput.getFiles() != null) {
                    saveGeneratedFiles(project, beRepairOutput.getFiles(), "BACKEND_DEVELOPER");
                }
            }

            // 7. Save every code version: refresh context with latest files from repository
            latestFiles = fileRepository.findByProjectIdAndIsLatestTrueOrderByFilePathAsc(project.getId());
            context.setGeneratedFiles(latestFiles);
            sourceFiles = latestFiles.stream()
                    .map(f -> GeneratedSourceFile.builder()
                            .filePath(f.getFilePath())
                            .content(f.getFileContent())
                            .language(f.getLanguage())
                            .build())
                    .toList();

            // 5: Execute tests again inside isolated sandbox
            updateProjectState(project, "TESTING", "TESTING_AGENT", 84);
            broadcastWorkflow(project, "TESTING_AGENT", "RUNNING", 84,
                    String.format("Re-testing corrected code in isolated sandbox (Iteration %d/%d)...", currentRetry, maxSelfHealingRetries));

            TestingAgentOutput reTestOutput = testingAgent.runTests(TestingAgentInput.builder()
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .requirementSpec(context.getRequirementSpec())
                    .architectureSpec(context.getArchitectureSpec())
                    .sourceFiles(sourceFiles)
                    .build());

            if (reTestOutput.isSuccess() && reTestOutput.getReport() != null) {
                context.setTestReport(reTestOutput.getReport());
                lastSandboxResult = reTestOutput.getSandboxResult();
                persistTestResults(project, reTestOutput.getReport());
            }

            // 6: Send updated code to Code Reviewer
            updateProjectState(project, "REVIEWING", "CODE_REVIEWER", 88);
            broadcastWorkflow(project, "CODE_REVIEWER", "RUNNING", 88,
                    String.format("Re-auditing corrected code quality and security (Iteration %d/%d)...", currentRetry, maxSelfHealingRetries));

            CodeReviewerOutput reReviewOutput = codeReviewer.reviewCode(CodeReviewerInput.builder()
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .requirementSpec(context.getRequirementSpec())
                    .architectureSpec(context.getArchitectureSpec())
                    .sourceFiles(sourceFiles)
                    .testReport(context.getTestReport())
                    .build());

            if (reReviewOutput.isSuccess() && reReviewOutput.getReport() != null) {
                context.setReviewReport(reReviewOutput.getReport());
                persistCodeReviews(project, reReviewOutput.getReport());
            }

            hasTestFailures = context.getTestReport() != null && !context.getTestReport().isAllPassed();
            hasReviewRejection = context.getReviewReport() != null && !context.getReviewReport().isApproved();
        }

        // =========================================================================
        // AGENT 7: DOCUMENTATION AGENT
        // =========================================================================
        if (checkIfPaused(projectId)) return;

        updateProjectState(project, "GENERATING_DOCUMENTATION", "DOCUMENTATION_AGENT", 95);
        broadcastWorkflow(project, "DOCUMENTATION_AGENT", "RUNNING", 95, "Synthesizing comprehensive README, architecture specifications, and API documentation...");

        DocumentationAgentInput docInput = DocumentationAgentInput.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .requirementSpec(context.getRequirementSpec())
                .architectureSpec(context.getArchitectureSpec())
                .sourceFiles(sourceFiles)
                .testReport(context.getTestReport())
                .reviewReport(context.getReviewReport())
                .build();

        DocumentationAgentOutput docOutput = executeWithRetry("DOCUMENTATION_AGENT", () -> documentationAgent.generateDocumentation(docInput));

        if (docOutput.isSuccess() && docOutput.getDocumentationBundle() != null) {
            context.setDocumentationBundle(docOutput.getDocumentationBundle());
            persistProjectDocuments(project, docOutput.getDocumentationBundle());
        }

        persistExecutionHistory(project, "DOCUMENTATION_AGENT", "Documentation Agent", docInput, docOutput, docOutput.isSuccess(),
                docOutput.getSummary(), docOutput.getExecutionDurationMs(), docOutput.getErrorMessage());

        broadcastWorkflow(project, "DOCUMENTATION_AGENT", "COMPLETED", 98, docOutput.getSummary());

        // =========================================================================
        // PIPELINE COMPLETION
        // =========================================================================
        project.setStatus("COMPLETED");
        project.setProgressPercentage(100);
        project.setCurrentAgent("DOCUMENTATION_AGENT");
        projectRepository.save(project);

        broadcastStatus(project, "DOCUMENTATION_AGENT", "COMPLETED", 100, "All 7 AI Agents completed successfully! Full-stack project is ready for download.");
        log.info("[AgentOrchestrator] Multi-agent software engineering workflow successfully completed for project {}!", project.getId());
    }

    /**
     * Executes an agent task with configurable retries and exponential backoff.
     */
    private <T> T executeWithRetry(String agentType, AgentSupplier<T> supplier) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < maxAgentRetries) {
            attempts++;
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                log.warn("[AgentOrchestrator] Agent {} failed attempt {}/{}: {}", agentType, attempts, maxAgentRetries, e.getMessage());
                try {
                    Thread.sleep(1000L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Agent execution interrupted", ie);
                }
            }
        }

        log.error("[AgentOrchestrator] Agent {} exhausted all {} retry attempts.", agentType, maxAgentRetries);
        throw new RuntimeException(String.format("Agent %s failed after %d attempts: %s", agentType, maxAgentRetries,
                lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    @FunctionalInterface
    private interface AgentSupplier<T> {
        T get() throws Exception;
    }

    private boolean checkIfPaused(Long projectId) {
        if (pausedProjects.contains(projectId)) {
            log.info("[AgentOrchestrator] Halting workflow for project {} because it was paused.", projectId);
            return true;
        }
        return false;
    }

    private String buildSelfHealingDiagnostic(TestExecutionReport testReport, CodeReviewReport reviewReport) {
        StringBuilder diag = new StringBuilder();
        if (testReport != null && testReport.getTestCases() != null) {
            for (var tc : testReport.getTestCases()) {
                if (!"PASSED".equalsIgnoreCase(tc.getStatus())) {
                    diag.append("Test Failure: ").append(tc.getTestName()).append(" in ").append(tc.getTestClass()).append("\n");
                    diag.append("Error Message: ").append(tc.getErrorMessage()).append("\n");
                    if (tc.getStackTrace() != null) diag.append("Stack Trace: ").append(tc.getStackTrace()).append("\n");
                }
            }
        }
        if (reviewReport != null && reviewReport.getIssues() != null) {
            for (var issue : reviewReport.getIssues()) {
                if ("ERROR".equalsIgnoreCase(issue.getSeverity()) || "CRITICAL".equalsIgnoreCase(issue.getSeverity())) {
                    diag.append("Review Issue in ").append(issue.getFilePath()).append(": ").append(issue.getIssueDescription()).append("\n");
                    diag.append("Suggested Fix: ").append(issue.getSuggestedFix()).append("\n");
                }
            }
        }
        return diag.toString();
    }

    @Transactional
    public void saveGeneratedFiles(Project project, List<GeneratedSourceFile> files, String agentType) {
        if (files == null || files.isEmpty()) return;

        for (GeneratedSourceFile file : files) {
            Integer maxVersion = fileRepository.findMaxVersion(project.getId(), file.getFilePath());
            int newVersion = (maxVersion != null) ? maxVersion + 1 : 1;

            fileRepository.findByProjectIdAndFilePathAndIsLatestTrue(project.getId(), file.getFilePath())
                    .ifPresent(existing -> {
                        existing.setIsLatest(false);
                        fileRepository.save(existing);
                    });

            GeneratedFile gf = GeneratedFile.builder()
                    .project(project)
                    .filePath(file.getFilePath())
                    .fileContent(file.getContent())
                    .language(file.getLanguage())
                    .version(newVersion)
                    .isLatest(true)
                    .generatedByAgent(agentType)
                    .build();
            fileRepository.save(gf);
        }
    }

    private void persistExecutionHistory(Project project, String agentType, String agentName, Object inputDto, Object outputDto,
                                         boolean success, String summary, long durationMs, String errorMessage) {
        try {
            Agent agentEntity = agentRepository.findByAgentType(agentType)
                    .orElseGet(() -> agentRepository.save(Agent.builder()
                            .agentName(agentName)
                            .agentType(agentType)
                            .description(summary)
                            .build()));

            String inputJson = inputDto != null ? objectMapper.writeValueAsString(inputDto) : null;
            String outputJson = outputDto != null ? objectMapper.writeValueAsString(outputDto) : null;

            AgentExecution execution = AgentExecution.builder()
                    .project(project)
                    .agent(agentEntity)
                    .executionStatus(success ? "COMPLETED" : "FAILED")
                    .currentTask(summary)
                    .inputData(inputJson)
                    .outputData(outputJson)
                    .progressPercentage(success ? 100 : 0)
                    .durationMs(durationMs)
                    .errorMessage(errorMessage)
                    .startedAt(LocalDateTime.now().minusNanos(durationMs * 1_000_000))
                    .completedAt(LocalDateTime.now())
                    .build();

            executionRepository.save(execution);

        } catch (Exception e) {
            log.error("[AgentOrchestrator] Error persisting agent execution history: {}", e.getMessage(), e);
        }
    }

    private void persistTestResults(Project project, TestExecutionReport report) {
        if (report == null || report.getTestCases() == null) return;
        for (var tc : report.getTestCases()) {
            TestResult tr = TestResult.builder()
                    .project(project)
                    .testName(tc.getTestName())
                    .testClass(tc.getTestClass())
                    .testStatus(tc.getStatus())
                    .errorMessage(tc.getErrorMessage())
                    .stackTrace(tc.getStackTrace())
                    .executionTime(tc.getExecutionTimeMs())
                    .build();
            testResultRepository.save(tr);
        }
    }

    private void persistCodeReviews(Project project, CodeReviewReport report) {
        if (report == null || report.getIssues() == null) return;
        for (var issue : report.getIssues()) {
            CodeReview cr = CodeReview.builder()
                    .project(project)
                    .filePath(issue.getFilePath())
                    .severity(issue.getSeverity())
                    .category(issue.getCategory())
                    .issueDescription(issue.getIssueDescription())
                    .suggestedFix(issue.getSuggestedFix())
                    .lineNumber(issue.getLineNumber())
                    .build();
            codeReviewRepository.save(cr);
        }
    }

    private void persistProjectDocuments(Project project, DocumentationBundle bundle) {
        if (bundle == null) return;
        saveDoc(project, "README", "Project README", bundle.getReadme());
        saveDoc(project, "ARCHITECTURE", "Architecture Specification", bundle.getArchitectureDoc());
        saveDoc(project, "INSTALLATION", "Installation Guide", bundle.getInstallationGuide());
        saveDoc(project, "API_SPEC", "API Documentation", bundle.getApiDocs());
        saveDoc(project, "DEPLOYMENT", "Deployment Guide", bundle.getDeploymentGuide());
    }

    private void saveDoc(Project project, String type, String title, String content) {
        if (content == null || content.isBlank()) return;
        ProjectDocument doc = documentRepository.findByProjectIdAndDocumentType(project.getId(), type)
                .orElseGet(() -> ProjectDocument.builder().project(project).documentType(type).build());
        doc.setTitle(title);
        doc.setDocumentContent(content);
        documentRepository.save(doc);
    }

    private void updateProjectState(Project project, String status, String currentAgent, int progress) {
        project.setStatus(status);
        project.setCurrentAgent(currentAgent);
        project.setProgressPercentage(progress);
        projectRepository.save(project);
    }

    private void markProjectFailed(Long projectId, String message) {
        projectRepository.findById(projectId).ifPresent(p -> {
            p.setStatus("FAILED");
            projectRepository.save(p);
            broadcastStatus(p, p.getCurrentAgent() != null ? p.getCurrentAgent() : "ORCHESTRATOR", "FAILED", p.getProgressPercentage(), message);
        });
    }

    private void broadcastWorkflow(Project project, String agentType, String status, int progress, String task) {
        sseService.broadcastEvent(project.getId(), "AGENT_STATUS", OrchestrationEvent.builder()
                .projectId(project.getId())
                .agentType(agentType)
                .status(status)
                .progressPercentage(progress)
                .currentTask(task)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private void broadcastStatus(Project project, String agentType, String status, int progress, String message) {
        sseService.broadcastEvent(project.getId(), "WORKFLOW_STATUS", OrchestrationEvent.builder()
                .projectId(project.getId())
                .agentType(agentType)
                .status(status)
                .progressPercentage(progress)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private AgentContext buildAgentContext(Project project) {
        AgentContext context = AgentContext.builder()
                .projectId(project.getId())
                .project(project)
                .userPrompt(project.getRequirements())
                .retryCount(project.getRetryCount() != null ? project.getRetryCount() : 0)
                .generatedFiles(fileRepository.findByProjectIdAndIsLatestTrueOrderByFilePathAsc(project.getId()))
                .build();

        executionRepository.findByProjectIdOrderByStartedAtAsc(project.getId()).forEach(exec -> {
            if ("REQUIREMENT_ANALYST".equals(exec.getAgent().getAgentType()) && exec.getOutputData() != null) {
                try {
                    RequirementAnalysisOutput out = objectMapper.readValue(exec.getOutputData(), RequirementAnalysisOutput.class);
                    if (out != null && out.getRequirementSpec() != null) {
                        context.setRequirementSpec(out.getRequirementSpec());
                    }
                } catch (Exception ignored) {}
            }
            if ("SYSTEM_ARCHITECT".equals(exec.getAgent().getAgentType()) && exec.getOutputData() != null) {
                try {
                    SystemArchitectOutput out = objectMapper.readValue(exec.getOutputData(), SystemArchitectOutput.class);
                    if (out != null && out.getArchitectureSpec() != null) {
                        context.setArchitectureSpec(out.getArchitectureSpec());
                    }
                } catch (Exception ignored) {}
            }
        });

        return context;
    }
}
