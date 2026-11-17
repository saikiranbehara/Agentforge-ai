package com.agentforge.service;

import com.agentforge.entity.GeneratedFile;
import com.agentforge.entity.Project;
import com.agentforge.exception.ResourceNotFoundException;
import com.agentforge.repository.GeneratedFileRepository;
import com.agentforge.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectExportService {

    private final ProjectRepository projectRepository;
    private final GeneratedFileRepository fileRepository;

    @Transactional(readOnly = true)
    public byte[] exportProjectAsZip(Long projectId) throws IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        List<GeneratedFile> files = fileRepository.findByProjectIdAndIsLatestTrueOrderByFilePathAsc(projectId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (GeneratedFile file : files) {
                String entryName = sanitizePath(file.getFilePath());
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(file.getFileContent().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }

        return baos.toByteArray();
    }

    private String sanitizePath(String path) {
        if (path == null) return "file.txt";
        // Remove leading slashes and prevent directory traversal
        String cleaned = path.replace("\\", "/");
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned.replace("../", "");
    }
}
