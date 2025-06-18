package com.iso27001planner.service;

import com.iso27001planner.dto.EmployeeDTO;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Employee;
import com.iso27001planner.entity.Training;
import com.iso27001planner.entity.TrainingRecord;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.EmployeeRepository;
import com.iso27001planner.repository.TrainingRecordRepository;
import com.iso27001planner.repository.TrainingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingRecordRepository trainingRecordRepository;

    public EmployeeDTO register(EmployeeDTO dto, Long companyId) {
        Employee employee = Employee.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .department(dto.getDepartment())
                .company(Company.builder().id(companyId).build())
                .build();

        return toDTO(employeeRepository.save(employee));
    }

    public void markTrainingCompleted(UUID employeeId, UUID trainingId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee not found", HttpStatus.NOT_FOUND));

        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new BusinessException("Training not found", HttpStatus.NOT_FOUND));

        TrainingRecord record = TrainingRecord.builder()
                .employee(emp)
                .training(training)
                .completionDate(LocalDate.now())
                .build();

        trainingRecordRepository.save(record);
    }

    public List<EmployeeDTO> listByCompany(Long companyId) {
        return employeeRepository.findByCompany_Id(companyId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void updateEmployee(UUID id, EmployeeDTO dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Employee not found", HttpStatus.NOT_FOUND));

        employee.setName(dto.getName());
        employee.setDepartment(dto.getDepartment());
        employee.setEmail(dto.getEmail());

        employeeRepository.save(employee);
    }

    @Transactional
    public void deleteEmployee(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new BusinessException("Employee not found", HttpStatus.NOT_FOUND);
        }
        employeeRepository.deleteById(id);
    }

    private EmployeeDTO toDTO(Employee emp) {
        List<String> completed = emp.getCompletedTrainings() != null
                ? emp.getCompletedTrainings().stream()
                .map(r -> r.getTraining().getTitle())
                .toList()
                : new ArrayList<>();

        return new EmployeeDTO(
                emp.getId().toString(),
                emp.getName(),
                emp.getDepartment(),
                emp.getEmail(),
                completed
        );
    }
}
