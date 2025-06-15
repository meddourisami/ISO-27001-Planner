package com.iso27001planner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Control;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.ControlRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ControlSeeder {

    private final ControlRepository controlRepository;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void seedControlsForAllCompanies() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/iso/annex-a-controls.json");

        List<Map<String, String>> rawControls = objectMapper.readValue(inputStream, new TypeReference<>() {});

        List<Company> companies = companyRepository.findAll();

        for (Company company : companies) {
            boolean alreadySeeded = controlRepository.existsByCompanyId(company.getId());

            if (!alreadySeeded) {
                List<Control> controls = rawControls.stream()
                        .map(c -> Control.builder()
                                .title(c.get("title"))
                                .description(c.get("description"))
                                .status("not_implemented")
                                .lastReview(LocalDate.now())
                                .company(company)
                                .build())
                        .toList();

                controlRepository.saveAll(controls);
                System.out.println("✅ Seeded " + controls.size() + " controls for company ID " + company.getId());
            } else {
                System.out.println("⚠️ Controls already seeded for company ID " + company.getId());
            }
        }
    }
}
