package com.agentforge.controller;

import com.agentforge.entity.TestResult;
import com.agentforge.service.TestingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{id}/tests")
@RequiredArgsConstructor
public class TestingController {

    private final TestingService testingService;

    @GetMapping
    public ResponseEntity<List<TestResult>> getTests(@PathVariable Long id) {
        return ResponseEntity.ok(testingService.getProjectTests(id));
    }

    @PostMapping("/run")
    public ResponseEntity<List<TestResult>> runTests(@PathVariable Long id) {
        return ResponseEntity.ok(testingService.runTests(id));
    }
}
