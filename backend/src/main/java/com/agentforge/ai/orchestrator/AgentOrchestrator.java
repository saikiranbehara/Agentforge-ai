package com.agentforge.ai.orchestrator;

import com.agentforge.ai.agent.*;
import com.agentforge.ai.model.*;
import com.agentforge.entity.*;
import com.agentforge.repository.*;
import com.agentforge.service.SseNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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

    private final SseNotificationService sseService;
    private final ObjectMapper objectMapper;

    private static final int MAX_SELF_HEALING_RETRIES = 2;

    // Track active running projects to prevent duplicate concurrent runs
    private final Set<Long> runningProjects = ConcurrentHashMap.newKeySet();

    @Async
    public CompletableFuture<Void> runWorkflowAsync(Long projectId) {
        if (!runningProjects.add(projectId)) {
            log.warn("Project {} is already executing an active workflow.", projectId);
            return CompletableFuture.completedFuture(null);
        }

        try {
            executeWorkflow(projectId);
        } catch (Exception e) {
            log.error("Fatal error during workflow execution for project {}", projectId, e);
            markProjectFailed(projectId, e.getMessage());
        } finally {
            runningProjects.remove(projectId);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void executeWorkflow(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        AgentContext context = buildAgentContext(project);

        // ==========================================
        // STAGE 1: REQUIREMENT ANALYST
        // ==========================================
        if (context.getRequirementSpec() == null) {
            updateProjectStatus(project, "ANALYZING", "REQUIREMENT_ANALYST", 10);
            AgentExecutionResult reqResult = runAgentStep(project, requirementAnalyst, context, "Analyzing user requirements and extracting modules...");

            if (!reqResult.isSuccess()) {
                markProjectFailed(project.getId(), "Requirement analysis failed: " + reqResult.getErrorMessage());
                return;
            }

            try {
                RequirementSpec spec = objectMapper.readValue(reqResult.getOutputData(), RequirementSpec.class);
                context.setRequirementSpec(spec);
            } catch (Exception e) {
                log.warn("Could not parse requirement spec JSON: {}", e.getMessage());
            }

            // Pause if user approval required before architecture
            if (Boolean.FALSE.equals(project.getRequirementsApproved())) {
                project.setStatus("PENDING_REQUIREMENTS_APPROVAL");
                project.setCurrentAgent("REQUIREMENT_ANALYST");
                projectRepository.save(project);
                broadcastStatus(project, "REQUIREMENT_ANALYST", "WAITING_APPROVAL", 20, "Waiting for user to review and approve requirements");
                return;
            }
        }

        // ==========================================
        // STAGE 2: SYSTEM ARCHITECT
        // ==========================================
        if (context.getArchitectureSpec() == null) {
            updateProjectStatus(project, "ARCHITECTING", "SYSTEM_ARCHITECT", 25);
            AgentExecutionResult archResult = runAgentStep(project, systemArchitect, context, "Designing relational schema, REST endpoints, and system architecture...");

            if (!archResult.isSuccess()) {
                markProjectFailed(project.getId(), "System architecture design failed: " + archResult.getErrorMessage());
                return;
            }

            try {
                SystemArchitectureSpec spec = objectMapper.readValue(archResult.getOutputData(), SystemArchitectureSpec.class);
                context.setArchitectureSpec(spec);
            } catch (Exception e) {
                log.warn("Could not parse architecture spec JSON: {}", e.getMessage());
            }

            // Pause if user approval required before code generation
            if (Boolean.FALSE.equals(project.getArchitectureApproved())) {
                project.setStatus("PENDING_ARCHITECTURE_APPROVAL");
                project.setCurrentAgent("SYSTEM_ARCHITECT");
                projectRepository.save(project);
                broadcastStatus(project, "SYSTEM_ARCHITECT", "WAITING_APPROVAL", 35, "Waiting for user to review and approve system architecture");
                return;
            }
        }

        // ==========================================
        // STAGE 3 & 4: PARALLEL BACKEND & FRONTEND CODE GENERATION
        // ==========================================
        updateProjectStatus(project, "GENERATING", "BACKEND_DEVELOPER", 45);

        // Run Backend Developer
        AgentExecutionResult beResult = runAgentStep(project, backendDeveloper, context, "Generating Spring Boot entities, repositories, services, and controllers...");
        if (beResult.isSuccess() && beResult.getFiles() != null) {
            saveGeneratedFiles(project, beResult.getFiles(), "BACKEND_DEVELOPER");
        }

        updateProjectStatus(project, "GENERATING", "FRONTEND_DEVELOPER", 60);

        // Run Frontend Developer
        AgentExecutionResult feResult = runAgentStep(project, frontendDeveloper, context, "Generating React TypeScript components, styling, and API services...");
        if (feResult.isSuccess() && feResult.getFiles() != null) {
            saveGeneratedFiles(project, feResult.getFiles(), "FRONTEND_DEVELOPER");
        }

        // Refresh context with generated files from database
        context.setGeneratedFiles(fileRepository.findByProjectIdAndIsLatestTrueOrderByFilePathAsc(project.getId()));

        // ==========================================
        // STAGE 5: TESTING AGENT (ISOLATED SANDBOX)
        // ==========================================
        updateProjectStatus(project, "TESTING", "TESTING_AGENT", 75);
        AgentExecutionResult testResult = runAgentStep(project, testingAgent, context, "Executing automated test suites in isolated sandbox...");

        if (testResult.getFiles() != null) {
            saveGeneratedFiles(project, testResult.getFiles(), "TESTING_AGENT");
        }
        persistTestResults(project, context.getTestReport());

        // ==========================================
        // STAGE 6: CODE REVIEWER & SELF-HEALING LOOP
        // ==========================================
        updateProjectStatus(project, "REVIEWING", "CODE_REVIEWER", 85);
        AgentExecutionResult reviewResult = runAgentStep(project, codeReviewer, context, "Auditing code for security vulnerabilities, bugs, and performance...");
        persistCodeReviews(project, context.getReviewReport());

        // Check for Self-Healing requirement
        boolean needsHealing = context.getReviewReport() != null && !context.getReviewReport().isApproved();
        if (needsHealing && project.getRetryCount() < MAX_SELF_HEALING_RETRIES) {
            log.info("Self-healing triggered for project {}. Iteration {}/{}", project.getId(), project.getRetryCount() + 1, MAX_SELF_HEALING_RETRIES);

            project.setRetryCount(project.getRetryCount() + 1);
            project.setStatus("SELF_HEALING");
            projectRepository.save(project);

            broadcastStatus(project, "CODE_REVIEWER", "RUNNING", 80,
                    "Self-healing in progress: Attempting fix iteration " + project.getRetryCount() + "/" + MAX_SELF_HEALING_RETRIES);

            // Re-run developer with review feedback
            context.setFeedbackMessage("Code review identified issues that must be addressed:\n" + context.getReviewReport().getSummary());
            runAgentStep(project, backendDeveloper, context, "Applying self-healing fixes to backend code...");
            runAgentStep(project, testingAgent, context, "Re-verifying fixes in test sandbox...");
            runAgentStep(project, codeReviewer, context, "Final quality validation...");
        }

        // ==========================================
        // STAGE 7: DOCUMENTATION AGENT
        // ==========================================
        updateProjectStatus(project, "DOCUMENTING", "DOCUMENTATION_AGENT", 95);
        AgentExecutionResult docResult = runAgentStep(project, documentationAgent, context, "Generating README, architecture docs, and API reference...");

        if (docResult.getFiles() != null) {
            saveGeneratedFiles(project, docResult.getFiles(), "DOCUMENTATION_AGENT");
        }
        persistProjectDocuments(project, context.getDocumentationBundle());

        // ==========================================
        // COMPLETION
        // ==========================================
        project.setStatus("COMPLETED");
        project.setProgressPercentage(100);
        project.setCurrentAgent("DOCUMENTATION_AGENT");
        projectRepository.save(project);

        broadcastStatus(project, "DOCUMENTATION_AGENT", "COMPLETED", 100, "Project successfully generated and ready for download!");
        log.info("Workflow successfully completed for project {}", project.getId());
    }

    private AgentExecutionResult runAgentStep(Project project, BaseAgent agent, AgentContext context, String taskDescription) {
        Agent agentEntity = agentRepository.findByAgentType(agent.getAgentType())
                .orElseGet(() -> agentRepository.save(Agent.builder()
                        .agentName(agent.getAgentName())
                        .agentType(agent.getAgentType())
                        .build()));

        AgentExecution execution = AgentExecution.builder()
                .project(project)
                .agent(agentEntity)
                .executionStatus("RUNNING")
                .currentTask(taskDescription)
                .progressPercentage(10)
                .startedAt(LocalDateTime.now())
                .build();
        execution = executionRepository.save(execution);

        broadcastExecution(project, agent, "RUNNING", 20, taskDescription);

        long start = System.currentTimeMillis();
        AgentExecutionResult result = agent.execute(context);
        long duration = System.currentTimeMillis() - start;

        execution.setExecutionStatus(result.isSuccess() ? "COMPLETED" : "FAILED");
        execution.setProgressPercentage(result.isSuccess() ? 100 : 0);
        execution.setOutputData(result.getOutputData());
        execution.setErrorMessage(result.getErrorMessage());
        execution.setDurationMs(duration);
        execution.setCompletedAt(LocalDateTime.now());
        executionRepository.save(execution);

        broadcastExecution(project, agent, execution.getExecutionStatus(), 100, result.getSummary() != null ? result.getSummary() : taskDescription);
        return result;
    }

    @Transactional
    public void saveGeneratedFiles(Project project, List<GeneratedSourceFile> files, String agentType) {
        for (GeneratedSourceFile file : files) {
            // Check existing versions
            Integer maxVersion = fileRepository.findMaxVersion(project.getId(), file.getFilePath());
            int newVersion = (maxVersion != null) ? maxVersion + 1 : 1;

            // Mark previous version as not latest
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

    private void updateProjectStatus(Project project, String status, String currentAgent, int progress) {
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

    private void broadcastExecution(Project project, BaseAgent agent, String status, int progress, String task) {
        sseService.broadcastEvent(project.getId(), "AGENT_STATUS", OrchestrationEvent.builder()
                .projectId(project.getId())
                .agentType(agent.getAgentType())
                .agentName(agent.getAgentName())
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

        // Check if requirement execution exists
        executionRepository.findByProjectIdOrderByStartedAtAsc(project.getId()).forEach(exec -> {
            if ("REQUIREMENT_ANALYST".equals(exec.getAgent().getAgentType()) && exec.getOutputData() != null) {
                try {
                    context.setRequirementSpec(objectMapper.readValue(exec.getOutputData(), RequirementSpec.class));
                } catch (Exception ignored) {}
            }
            if ("SYSTEM_ARCHITECT".equals(exec.getAgent().getAgentType()) && exec.getOutputData() != null) {
                try {
                    context.setArchitectureSpec(objectMapper.readValue(exec.getOutputData(), SystemArchitectureSpec.class));
                } catch (Exception ignored) {}
            }
        });

        return context;
    }
}
