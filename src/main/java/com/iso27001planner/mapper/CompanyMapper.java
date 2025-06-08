package com.iso27001planner.mapper;

import com.iso27001planner.dto.CompanyDTO;
import com.iso27001planner.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyDTO toDto(Company company) {
        return new CompanyDTO(
                company.getId(),
                company.getName(),
                company.getIsmsScope()
        );
    }
}
