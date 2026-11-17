package com.agentforge.controller;

import com.agentforge.entity.ProjectDocument;
import com.agentforge.service.DocumentationService;
import com.agentforge.service.ProjectExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class ExportController {

    private final ProjectExportService exportService;
    private final DocumentationService documentationService;

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadProjectZip(@PathVariable Long id) throws IOException {
        byte[] zipBytes = exportService.exportProjectAsZip(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"agentforge-project-" + id + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }

    @GetMapping("/documents")
    public ResponseEntity<List<ProjectDocument>> getProjectDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(documentationService.getProjectDocuments(id));
    }
}
