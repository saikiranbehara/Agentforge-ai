package com.agentforge.controller;

import com.agentforge.entity.CodeReview;
import com.agentforge.service.CodeReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{id}/review")
@RequiredArgsConstructor
public class CodeReviewController {

    private final CodeReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<CodeReview>> getReviews(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getProjectReviews(id));
    }

    @PostMapping
    public ResponseEntity<List<CodeReview>> triggerReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.runReview(id));
    }
}
