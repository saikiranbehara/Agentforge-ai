package com.agentforge.repository;

import com.agentforge.entity.GeneratedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeneratedFileRepository extends JpaRepository<GeneratedFile, Long> {
    List<GeneratedFile> findByProjectIdAndIsLatestTrueOrderByFilePathAsc(Long projectId);
    List<GeneratedFile> findByProjectIdOrderByFilePathAsc(Long projectId);
    Optional<GeneratedFile> findByProjectIdAndFilePathAndIsLatestTrue(Long projectId, String filePath);
    List<GeneratedFile> findByProjectIdAndFilePathOrderByVersionDesc(Long projectId, String filePath);
    
    @Query("SELECT MAX(g.version) FROM GeneratedFile g WHERE g.project.id = :projectId AND g.filePath = :filePath")
    Integer findMaxVersion(Long projectId, String filePath);

    void deleteByProjectId(Long projectId);
}
