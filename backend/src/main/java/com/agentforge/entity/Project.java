package com.agentforge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requirements;

    @Column(nullable = false)
    @Builder.Default
    private String status = "CREATED"; // CREATED, ANALYZING, ARCHITECTING, PENDING_APPROVAL, GENERATING, TESTING, REVIEWING, SELF_HEALING, COMPLETED, FAILED

    @Column(name = "backend_framework")
    @Builder.Default
    private String backendFramework = "Spring Boot 3.x";

    @Column(name = "frontend_framework")
    @Builder.Default
    private String frontendFramework = "React + TypeScript";

    @Column(name = "database_type")
    @Builder.Default
    private String databaseType = "PostgreSQL";

    @Column(name = "ai_model")
    @Builder.Default
    private String aiModel = "Gemini 1.5 Pro";

    @Column(name = "requirements_approved")
    @Builder.Default
    private Boolean requirementsApproved = false;

    @Column(name = "architecture_approved")
    @Builder.Default
    private Boolean architectureApproved = false;

    @Column(name = "current_agent")
    private String currentAgent;

    @Column(name = "progress_percentage")
    @Builder.Default
    private Integer progressPercentage = 0;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
