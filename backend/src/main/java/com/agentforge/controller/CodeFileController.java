package com.agentforge.controller;

import com.agentforge.dto.RegenerateFileRequest;
import com.agentforge.entity.GeneratedFile;
import com.agentforge.service.CodeFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{id}/files")
@RequiredArgsConstructor
public class CodeFileController {

    private final CodeFileService fileService;

    @GetMapping
    public ResponseEntity<List<GeneratedFile>> getFiles(@PathVariable Long id) {
        return ResponseEntity.ok(fileService.getProjectFiles(id));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<GeneratedFile> getFile(@PathVariable Long id, @PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.getFileById(id, fileId));
    }

    @PostMapping("/{fileId}/regenerate")
    public ResponseEntity<GeneratedFile> regenerateFile(
            @PathVariable Long id,
            @PathVariable Long fileId,
            @RequestBody(required = false) RegenerateFileRequest request) {
        if (request == null) {
            request = new RegenerateFileRequest();
        }
        return ResponseEntity.ok(fileService.regenerateFile(id, fileId, request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<GeneratedFile>> getFileHistory(
            @PathVariable Long id,
            @RequestParam String path) {
        return ResponseEntity.ok(fileService.getFileHistory(id, path));
    }

    @PostMapping("/{fileId}/restore")
    public ResponseEntity<GeneratedFile> restoreVersion(
            @PathVariable Long id,
            @PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.restoreFileVersion(id, fileId));
    }
}
