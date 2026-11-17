package com.agentforge.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private Long id;
    private Long userId;
    private String projectName;
    private String description;
    private String requirements;
    private String status;
    private String backendFramework;
    private String frontendFramework;
    private String databaseType;
    private String aiModel;
    private Boolean requirementsApproved;
    private Boolean architectureApproved;
    private String currentAgent;
    private Integer progressPercentage;
    private Integer retryCount;
    private Long totalFiles;
    private Long totalTests;
    private Long passedTests;
    private Long failedTests;
    private Long codeReviewIssues;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
