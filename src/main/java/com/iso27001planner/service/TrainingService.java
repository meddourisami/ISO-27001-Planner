package com.iso27001planner.service;

import com.iso27001planner.dto.TrainingDTO;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Employee;
import com.iso27001planner.entity.Training;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.EmployeeRepository;
import com.iso27001planner.repository.TrainingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingRepository trainingRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "NEW TRAINING ADDED",
                getCurrentUserEmail(),
                "Training",
                 training.getId().toString(),
                "New training added" + training.getTitle()
        ));

        return toDTO(trainingRepository.save(training));
    }

    public List<TrainingDTO> listTrainings(Long companyId) {
        return trainingRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void updateTraining(UUID id, TrainingDTO dto) {
        Training training = trainingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Training not found", HttpStatus.NOT_FOUND));

        training.setTitle(dto.getTitle());
        training.setDescription(dto.getDescription());
        training.setType(dto.getType());
        training.setStatus(dto.getStatus());
        training.setStartDate(LocalDate.parse(dto.getStartDate()));
        training.setEndDate(LocalDate.parse(dto.getEndDate()));
        training.setDuration(dto.getDuration());
        training.setInstructor(dto.getInstructor());
        training.setMaterials(dto.getMaterials());

        trainingRepository.save(training);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "TRAINING UPDATED",
                getCurrentUserEmail(),
                "Training",
                training.getId().toString(),
                training.getTitle() +" Training Info updated "
        ));
    }

    @Transactional
    public void deleteTraining(UUID id) {
        if (!trainingRepository.existsById(id)) {
            throw new BusinessException("Training not found", HttpStatus.NOT_FOUND);
        }

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "TRAINING DELETED",
                 getCurrentUserEmail(),
                "Training",
                 id.toString(),
                "Training deleted" + id.toString()
        ));
        trainingRepository.deleteById(id);

    }

    @Transactional
    public void assignEmployeeToTraining(UUID trainingId, UUID employeeId) {
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new BusinessException("Training not found", HttpStatus.NOT_FOUND));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee not found", HttpStatus.NOT_FOUND));

        // 🔒 Optional: check both belong to the same company
        if (!training.getCompany().getId().equals(employee.getCompany().getId())) {
            throw new BusinessException("Employee and training belong to different companies", HttpStatus.FORBIDDEN);
        }

        // 🛡 Avoid duplicates
        String employeeIdStr = employee.getId().toString();
        if (training.getRequiredFor() != null && training.getRequiredFor().contains(employeeIdStr)) {
            throw new BusinessException("Employee is already assigned to this training", HttpStatus.CONFLICT);
        }

        // 📝 Add employee to training
        if (training.getRequiredFor() == null) {
            training.setRequiredFor(new ArrayList<>());
        }

        training.getRequiredFor().add(employeeIdStr);
        trainingRepository.save(training);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "EMPLOYEE ASSIGNED TO TRAINING",
                employee.getEmail(),
                "Training",
                training.getId().toString(),
                "Employee assigned to training: " + training.getTitle()
        ));
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

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
