package com.agentforge.service;

import com.agentforge.ai.orchestrator.AgentOrchestrator;
import com.agentforge.dto.*;
import com.agentforge.entity.*;
import com.agentforge.exception.ResourceNotFoundException;
import com.agentforge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AgentExecutionRepository executionRepository;
    private final GeneratedFileRepository fileRepository;
    private final TestResultRepository testResultRepository;
    private final CodeReviewRepository codeReviewRepository;
    private final ProjectDocumentRepository documentRepository;
    private final AgentOrchestrator orchestrator;

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = Project.builder()
                .user(user)
                .projectName(request.getProjectName())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .status("CREATED")
                .backendFramework(request.getBackendFramework() != null ? request.getBackendFramework() : "Spring Boot 3.x")
                .frontendFramework(request.getFrontendFramework() != null ? request.getFrontendFramework() : "React + TypeScript")
                .databaseType(request.getDatabaseType() != null ? request.getDatabaseType() : "PostgreSQL")
                .aiModel(request.getAiModel() != null ? request.getAiModel() : "Gemini 1.5 Pro")
                .progressPercentage(0)
                .build();

        project = projectRepository.save(project);
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects(Long userId) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId, Long userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        return mapToResponse(project);
    }

    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        executionRepository.deleteByProjectId(projectId);
        fileRepository.deleteByProjectId(projectId);
        testResultRepository.deleteByProjectId(projectId);
        codeReviewRepository.deleteByProjectId(projectId);
        documentRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }

    @Transactional
    public ProjectResponse executeProject(Long projectId, Long userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        orchestrator.runWorkflowAsync(projectId);
        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse approveRequirements(Long projectId, Long userId, ApprovalRequest approval) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        if (Boolean.TRUE.equals(approval.getApproved())) {
            project.setRequirementsApproved(true);
            project.setStatus("REQUIREMENTS_APPROVED");
            projectRepository.save(project);
            orchestrator.runWorkflowAsync(projectId);
        } else {
            project.setStatus("REQUIREMENTS_REJECTED");
            projectRepository.save(project);
        }

        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse approveArchitecture(Long projectId, Long userId, ApprovalRequest approval) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        if (Boolean.TRUE.equals(approval.getApproved())) {
            project.setArchitectureApproved(true);
            project.setStatus("ARCHITECTURE_APPROVED");
            projectRepository.save(project);
            orchestrator.runWorkflowAsync(projectId);
        } else {
            project.setStatus("ARCHITECTURE_REJECTED");
            projectRepository.save(project);
        }

        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(Long userId) {
        long total = projectRepository.countByUserId(userId);
        long completed = projectRepository.countByUserIdAndStatus(userId, "COMPLETED");
        long failed = projectRepository.countByUserIdAndStatus(userId, "FAILED");
        long active = total - completed - failed;
        if (active < 0) active = 0;

        List<ProjectResponse> recent = projectRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(5)
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return DashboardStatsDto.builder()
                .totalProjects(total)
                .activeProjects(active)
                .completedProjects(completed)
                .failedProjects(failed)
                .recentProjects(recent)
                .build();
    }

    public ProjectResponse mapToResponse(Project project) {
        long fileCount = fileRepository.findByProjectIdAndIsLatestTrueOrderByFilePathAsc(project.getId()).size();
        long testCount = testResultRepository.countByProjectId(project.getId());
        long passedCount = testResultRepository.countByProjectIdAndTestStatus(project.getId(), "PASSED");
        long failedCount = testResultRepository.countByProjectIdAndTestStatus(project.getId(), "FAILED");
        long issueCount = codeReviewRepository.countByProjectId(project.getId());

        return ProjectResponse.builder()
                .id(project.getId())
                .userId(project.getUser() != null ? project.getUser().getId() : null)
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .requirements(project.getRequirements())
                .status(project.getStatus())
                .backendFramework(project.getBackendFramework())
                .frontendFramework(project.getFrontendFramework())
                .databaseType(project.getDatabaseType())
                .aiModel(project.getAiModel())
                .requirementsApproved(project.getRequirementsApproved())
                .architectureApproved(project.getArchitectureApproved())
                .currentAgent(project.getCurrentAgent())
                .progressPercentage(project.getProgressPercentage())
                .retryCount(project.getRetryCount())
                .totalFiles(fileCount)
                .totalTests(testCount)
                .passedTests(passedCount)
                .failedTests(failedCount)
                .codeReviewIssues(issueCount)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
