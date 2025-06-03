package com.iso27001planner.repository;

import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Role;
import com.iso27001planner.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByCompany(Company company);
    Page<User> findByCompany(Company company, Pageable pageable);
    Page<User> findByCompanyAndRole(Company company, Role role, Pageable pageable);

    Optional<User> findByEmailIgnoreCase(String email);
}
