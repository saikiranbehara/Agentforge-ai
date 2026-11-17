package com.agentforge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String severity; // INFO, WARNING, ERROR, CRITICAL

    @Column(name = "category")
    private String category; // SECURITY, BUG, PERFORMANCE, STYLE, ARCHITECTURE

    @Column(name = "issue_description", columnDefinition = "TEXT", nullable = false)
    private String issueDescription;

    @Column(name = "suggested_fix", columnDefinition = "TEXT")
    private String suggestedFix;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
