package com.agentforge.service;

import com.agentforge.ai.agent.AgentContext;
import com.agentforge.ai.agent.TestingAgent;
import com.agentforge.entity.Project;
import com.agentforge.entity.TestResult;
import com.agentforge.exception.ResourceNotFoundException;
import com.agentforge.repository.GeneratedFileRepository;
import com.agentforge.repository.ProjectRepository;
import com.agentforge.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestingService {

    private final TestResultRepository testResultRepository;
    private final ProjectRepository projectRepository;
    private final GeneratedFileRepository fileRepository;
    private final TestingAgent testingAgent;

    @Transactional(readOnly = true)
    public List<TestResult> getProjectTests(Long projectId) {
        return testResultRepository.findByProjectIdOrderByIdAsc(projectId);
    }

    @Transactional
    public List<TestResult> runTests(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        AgentContext context = AgentContext.builder()
                .projectId(projectId)
                .project(project)
                .userPrompt(project.getRequirements())
                .generatedFiles(fileRepository.findByProjectIdAndIsLatestTrueOrderByFilePathAsc(projectId))
                .build();

        // Clear previous results
        testResultRepository.deleteByProjectId(projectId);

        var result = testingAgent.execute(context);
        if (context.getTestReport() != null && context.getTestReport().getTestCases() != null) {
            for (var tc : context.getTestReport().getTestCases()) {
                testResultRepository.save(TestResult.builder()
                        .project(project)
                        .testName(tc.getTestName())
                        .testClass(tc.getTestClass())
                        .testStatus(tc.getStatus())
                        .errorMessage(tc.getErrorMessage())
                        .stackTrace(tc.getStackTrace())
                        .executionTime(tc.getExecutionTimeMs())
                        .build());
            }
        }

        return testResultRepository.findByProjectIdOrderByIdAsc(projectId);
    }
}
