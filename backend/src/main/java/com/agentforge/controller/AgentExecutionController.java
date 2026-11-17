package com.agentforge.controller;

import com.agentforge.dto.AgentExecutionResponse;
import com.agentforge.dto.ProjectResponse;
import com.agentforge.security.SecurityUtils;
import com.agentforge.service.AgentExecutionService;
import com.agentforge.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class AgentExecutionController {

    private final ProjectService projectService;
    private final AgentExecutionService executionService;
    private final SecurityUtils securityUtils;

    @PostMapping("/execute")
    public ResponseEntity<ProjectResponse> executeProject(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(projectService.executeProject(id, userId));
    }

    @GetMapping("/agents")
    public ResponseEntity<List<AgentExecutionResponse>> getProjectAgents(@PathVariable Long id) {
        return ResponseEntity.ok(executionService.getProjectExecutions(id));
    }

    @GetMapping("/executions")
    public ResponseEntity<List<AgentExecutionResponse>> getProjectExecutions(@PathVariable Long id) {
        return ResponseEntity.ok(executionService.getProjectExecutions(id));
    }
}
