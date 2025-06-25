package com.iso27001planner.repository;

import com.iso27001planner.entity.Document;
import com.iso27001planner.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByCompany_Id(Long companyId);

    @Query("SELECT d FROM Document d WHERE d.deleted = false")
    List<Document> findAllActive();

    @Query("SELECT d FROM Document d WHERE d.company.id = :companyId AND d.deleted = false")
    List<Document> findByCompanyId(@Param("companyId") Long companyId);
}
