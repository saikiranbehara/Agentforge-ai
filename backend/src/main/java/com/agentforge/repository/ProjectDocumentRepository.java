package com.agentforge.repository;

import com.agentforge.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {
    List<ProjectDocument> findByProjectIdOrderByCreatedAtAsc(Long projectId);
    Optional<ProjectDocument> findByProjectIdAndDocumentType(Long projectId, String documentType);
    void deleteByProjectId(Long projectId);
}
