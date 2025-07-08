package com.iso27001planner.service;

import com.iso27001planner.dto.TaskDTO;
import com.iso27001planner.entity.Task;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.RiskRepository;
import com.iso27001planner.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final RiskRepository riskRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TaskDTO createTask(TaskDTO dto) {
        Task task = Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .assignee(dto.getAssignee())
                .status(dto.getStatus())
                .priority(dto.getPriority())
                .dueDate(LocalDate.parse(dto.getDueDate()))
                .category(dto.getCategory())
                .progress(dto.getProgress())
                .relatedControl(dto.getRelatedControl())
                .company(companyRepository.findById(dto.getCompanyId()).orElse(null))
                .risk(dto.getRiskId() != null ? riskRepository.findById(UUID.fromString(String.valueOf(dto.getRiskId()))).orElse(null) : null)
                .build();

        return toDTO(taskRepository.save(task));
    }

    public List<TaskDTO> listCompanyTasks(Long companyId) {
        return taskRepository.findByCompany_Id(companyId).stream().map(this::toDTO).toList();
    }

    public void updateProgress(UUID id, int progress) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Task not found", HttpStatus.NOT_FOUND));
        task.setProgress(progress);
        if (progress == 100) task.setStatus("completed");
        taskRepository.save(task);
    }

    public void updateStatus(UUID id, String status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Task not found", HttpStatus.NOT_FOUND));
        task.setStatus(status);
        taskRepository.save(task);
    }

    private TaskDTO toDTO(Task task) {
        return new TaskDTO(
                task.getId().toString(),
                task.getTitle(),
                task.getDescription(),
                task.getAssignee(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate() != null ? task.getDueDate().toString() : null,
                task.getCategory(),
                task.getProgress(),
                task.getRelatedControl(),
                task.getRisk() != null ? Long.valueOf(task.getRisk().getId()) : null,
                task.getCompany() != null ? task.getCompany().getId() : null
        );
    }

    @Transactional
    public void updateTask(UUID id, TaskDTO dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Task not found", HttpStatus.NOT_FOUND));

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setAssignee(dto.getAssignee());
        task.setStatus(dto.getStatus());
        task.setPriority(dto.getPriority());
        task.setDueDate(LocalDate.parse(dto.getDueDate()));
        task.setCategory(dto.getCategory());
        task.setProgress(dto.getProgress());
        task.setRelatedControl(dto.getRelatedControl());

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "UPDATE_TASK",
                getCurrentUserEmail(),
                "Task",
                id.toString(),
                "Task updated"
        ));

        taskRepository.save(task);
    }

    public List<TaskDTO> findHighRiskTasksNearingDeadline(int withinDays) {
        LocalDate threshold = LocalDate.now().plusDays(withinDays);
        return taskRepository.findUrgentHighRiskTasks(threshold).stream()
                .map(this::toDTO)
                .toList();
    }

    public int calculateRiskMitigationProgress(Long riskId) {
        int total = taskRepository.countByRiskId(riskId);
        if (total == 0) return 0;
        int done = taskRepository.countCompletedByRiskId(riskId);
        return (int) ((done * 100.0) / total);
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}
