package com.iso27001planner.service;

import com.iso27001planner.dto.TrainingDTO;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Training;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingRepository trainingRepository;
    private final CompanyRepository companyRepository;

    public TrainingDTO createTraining(TrainingDTO dto, Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        Training training = Training.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType())
                .status(dto.getStatus())
                .startDate(LocalDate.parse(dto.getStartDate()))
                .endDate(LocalDate.parse(dto.getEndDate()))
                .duration(dto.getDuration())
                .instructor(dto.getInstructor())
                .materials(dto.getMaterials())
                .requiredFor(dto.getRequiredFor())
                .company(company)
                .build();

        return toDTO(trainingRepository.save(training));
    }

    public List<TrainingDTO> listTrainings(Long companyId) {
        return trainingRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    private TrainingDTO toDTO(Training training) {
        return new TrainingDTO(
                training.getId().toString(),
                training.getTitle(),
                training.getDescription(),
                training.getType(),
                training.getStatus(),
                training.getStartDate().toString(),
                training.getEndDate().toString(),
                training.getDuration(),
                training.getInstructor(),
                training.getMaterials(),
                training.getRequiredFor()
        );
    }
}
