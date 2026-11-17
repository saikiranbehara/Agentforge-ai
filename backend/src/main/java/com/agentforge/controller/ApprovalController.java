package com.agentforge.controller;

import com.agentforge.dto.ApprovalRequest;
import com.agentforge.dto.ProjectResponse;
import com.agentforge.security.SecurityUtils;
import com.agentforge.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class ApprovalController {

    private final ProjectService projectService;
    private final SecurityUtils securityUtils;

    @PostMapping("/approve-requirements")
    public ResponseEntity<ProjectResponse> approveRequirements(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalRequest approval) {
        if (approval == null) {
            approval = ApprovalRequest.builder().approved(true).build();
        }
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(projectService.approveRequirements(id, userId, approval));
    }

    @PostMapping("/approve-architecture")
    public ResponseEntity<ProjectResponse> approveArchitecture(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalRequest approval) {
        if (approval == null) {
            approval = ApprovalRequest.builder().approved(true).build();
        }
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(projectService.approveArchitecture(id, userId, approval));
    }
}
