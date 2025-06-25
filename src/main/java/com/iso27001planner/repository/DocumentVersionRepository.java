package com.iso27001planner.repository;

import com.iso27001planner.entity.Document;
import com.iso27001planner.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocument_IdOrderByUploadedAtDesc(Long documentId);
    @Query("SELECT v FROM DocumentVersion v WHERE lower(v.previewContent) LIKE lower(concat('%', :query, '%'))")
    List<DocumentVersion> searchByContent(@Param("query") String query);

    Optional<DocumentVersion> findTopByDocumentOrderByUploadedAtDesc(Document doc);
}
