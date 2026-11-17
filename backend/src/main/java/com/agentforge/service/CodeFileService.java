package com.agentforge.service;

import com.agentforge.ai.agent.AgentContext;
import com.agentforge.ai.agent.BackendDeveloperAgent;
import com.agentforge.ai.agent.FrontendDeveloperAgent;
import com.agentforge.dto.RegenerateFileRequest;
import com.agentforge.entity.GeneratedFile;
import com.agentforge.entity.Project;
import com.agentforge.exception.ResourceNotFoundException;
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
public class CodeFileService {

    private final GeneratedFileRepository fileRepository;
    private final ProjectRepository projectRepository;
    private final BackendDeveloperAgent backendDeveloper;
    private final FrontendDeveloperAgent frontendDeveloper;

    @Transactional(readOnly = true)
    public List<GeneratedFile> getProjectFiles(Long projectId) {
        return fileRepository.findByProjectIdAndIsLatestTrueOrderByFilePathAsc(projectId);
    }

    @Transactional(readOnly = true)
    public GeneratedFile getFileById(Long projectId, Long fileId) {
        return fileRepository.findById(fileId)
                .filter(f -> f.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    @Transactional(readOnly = true)
    public List<GeneratedFile> getFileHistory(Long projectId, String filePath) {
        return fileRepository.findByProjectIdAndFilePathOrderByVersionDesc(projectId, filePath);
    }

    @Transactional
    public GeneratedFile regenerateFile(Long projectId, Long fileId, RegenerateFileRequest request) {
        GeneratedFile existingFile = getFileById(projectId, fileId);
        Project project = existingFile.getProject();

        AgentContext context = AgentContext.builder()
                .projectId(projectId)
                .project(project)
                .userPrompt(project.getRequirements())
                .targetFilePath(existingFile.getFilePath())
                .customInstructions(request.getCustomInstructions())
                .build();

        String newContent;
        if (existingFile.getFilePath().endsWith(".java")) {
            var result = backendDeveloper.execute(context);
            newContent = (result.getFiles() != null && !result.getFiles().isEmpty())
                    ? result.getFiles().get(0).getContent()
                    : existingFile.getFileContent();
        } else {
            var result = frontendDeveloper.execute(context);
            newContent = (result.getFiles() != null && !result.getFiles().isEmpty())
                    ? result.getFiles().get(0).getContent()
                    : existingFile.getFileContent();
        }

        // Increment version
        existingFile.setIsLatest(false);
        fileRepository.save(existingFile);

        GeneratedFile newVersion = GeneratedFile.builder()
                .project(project)
                .filePath(existingFile.getFilePath())
                .fileContent(newContent)
                .language(existingFile.getLanguage())
                .version(existingFile.getVersion() + 1)
                .isLatest(true)
                .generatedByAgent("MANUAL_REGENERATE")
                .build();

        return fileRepository.save(newVersion);
    }

    @Transactional
    public GeneratedFile restoreFileVersion(Long projectId, Long fileId) {
        GeneratedFile targetVersion = fileRepository.findById(fileId)
                .filter(f -> f.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Target version not found: " + fileId));

        // Mark current latest as not latest
        fileRepository.findByProjectIdAndFilePathAndIsLatestTrue(projectId, targetVersion.getFilePath())
                .ifPresent(curr -> {
                    curr.setIsLatest(false);
                    fileRepository.save(curr);
                });

        Integer maxVer = fileRepository.findMaxVersion(projectId, targetVersion.getFilePath());
        int nextVer = (maxVer != null) ? maxVer + 1 : 1;

        GeneratedFile restored = GeneratedFile.builder()
                .project(targetVersion.getProject())
                .filePath(targetVersion.getFilePath())
                .fileContent(targetVersion.getFileContent())
                .language(targetVersion.getLanguage())
                .version(nextVer)
                .isLatest(true)
                .generatedByAgent("RESTORED_V" + targetVersion.getVersion())
                .build();

        return fileRepository.save(restored);
    }
}
