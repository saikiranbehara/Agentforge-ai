package com.agentforge.service;

import com.agentforge.entity.ProjectDocument;
import com.agentforge.repository.ProjectDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentationService {

    private final ProjectDocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public List<ProjectDocument> getProjectDocuments(Long projectId) {
        return documentRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<ProjectDocument> getDocumentByType(Long projectId, String docType) {
        return documentRepository.findByProjectIdAndDocumentType(projectId, docType);
    }
}
