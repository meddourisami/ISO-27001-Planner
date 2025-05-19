package com.iso27001planner.repository;

import com.iso27001planner.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    List<Employee> findByCompany_Id(Long companyId);
}
