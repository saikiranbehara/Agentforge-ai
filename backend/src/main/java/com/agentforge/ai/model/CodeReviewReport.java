package com.agentforge.ai.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeReviewReport {
    private int qualityScore; // 0 - 100
    private boolean approved;
    private String summary;
    private List<ReviewIssue> issues;
    private List<String> suggestions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewIssue {
        private String filePath;
        private String severity; // INFO, WARNING, ERROR, CRITICAL
        private String category; // SECURITY, BUG, PERFORMANCE, STYLE
        private int lineNumber;
        private String issueDescription;
        private String suggestedFix;
    }
}
