package com.agentforge.controller;

import com.agentforge.dto.*;
import com.agentforge.security.SecurityUtils;
import com.agentforge.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return new ResponseEntity<>(projectService.createProject(request, userId), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getUserProjects() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(projectService.getUserProjects(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(projectService.getProjectById(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        projectService.deleteProject(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(projectService.getDashboardStats(userId));
    }
}
