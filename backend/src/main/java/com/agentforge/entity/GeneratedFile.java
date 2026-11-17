package com.agentforge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "generated_files", indexes = {
    @Index(name = "idx_proj_filepath", columnList = "project_id, file_path")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_content", columnDefinition = "TEXT", nullable = false)
    private String fileContent;

    @Column(nullable = false)
    private String language; // java, typescript, tsx, json, sql, markdown, xml, etc.

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "generated_by_agent")
    private String generatedByAgent;

    @Column(name = "is_latest")
    @Builder.Default
    private Boolean isLatest = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
