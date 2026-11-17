package com.agentforge.service;

import com.agentforge.ai.agent.AgentContext;
import com.agentforge.ai.agent.CodeReviewerAgent;
import com.agentforge.entity.CodeReview;
import com.agentforge.entity.Project;
import com.agentforge.exception.ResourceNotFoundException;
import com.agentforge.repository.CodeReviewRepository;
import com.agentforge.repository.GeneratedFileRepository;
import com.agentforge.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeReviewService {

    private final CodeReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final GeneratedFileRepository fileRepository;
    private final CodeReviewerAgent reviewerAgent;

    @Transactional(readOnly = true)
    public List<CodeReview> getProjectReviews(Long projectId) {
        return reviewRepository.findByProjectIdOrderByIdAsc(projectId);
    }

    @Transactional
    public List<CodeReview> runReview(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        AgentContext context = AgentContext.builder()
                .projectId(projectId)
                .project(project)
                .userPrompt(project.getRequirements())
                .generatedFiles(fileRepository.findByProjectIdAndIsLatestTrueOrderByFilePathAsc(projectId))
                .build();

        reviewRepository.deleteByProjectId(projectId);

        reviewerAgent.execute(context);
        if (context.getReviewReport() != null && context.getReviewReport().getIssues() != null) {
            for (var issue : context.getReviewReport().getIssues()) {
                reviewRepository.save(CodeReview.builder()
                        .project(project)
                        .filePath(issue.getFilePath())
                        .severity(issue.getSeverity())
                        .category(issue.getCategory())
                        .issueDescription(issue.getIssueDescription())
                        .suggestedFix(issue.getSuggestedFix())
                        .lineNumber(issue.getLineNumber())
                        .build());
            }
        }

        return reviewRepository.findByProjectIdOrderByIdAsc(projectId);
    }
}
