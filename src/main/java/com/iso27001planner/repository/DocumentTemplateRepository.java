package com.iso27001planner.repository;

import com.iso27001planner.entity.DocumentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {
    List<DocumentTemplate> findByCompanyIsNull();

    @Query("SELECT d FROM DocumentTemplate d WHERE lower(d.content) LIKE lower(concat('%', :keyword, '%'))")
    List<DocumentTemplate> searchByContent(@Param("keyword") String keyword);

    boolean existsByFileHash(String fileHash);
}
