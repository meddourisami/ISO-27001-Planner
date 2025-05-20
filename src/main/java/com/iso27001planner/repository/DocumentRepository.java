package com.iso27001planner.repository;

import com.iso27001planner.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByCompany_Id(Long companyId);
}
