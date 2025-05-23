package com.iso27001planner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iso27001planner.entity.Control;
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
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void seedControls() throws IOException {
        if (controlRepository.findByCompanyIsNull().isEmpty()) {
            InputStream inputStream = getClass().getResourceAsStream("/iso/annex-a-controls.json");

            List<Map<String, String>> rawControls = objectMapper.readValue(inputStream, new TypeReference<>() {});
            List<Control> controls = rawControls.stream()
                    .map(c -> Control.builder()
                            .title(c.get("title"))
                            .description(c.get("description"))
                            .status("not_implemented")
                            .lastReview(LocalDate.now())
                            .build())
                    .toList();

            controlRepository.saveAll(controls);
            System.out.println("✅ Seeded " + controls.size() + " Annex A controls");
        }
    }
}
