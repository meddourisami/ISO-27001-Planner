package com.iso27001planner.controller;

import com.iso27001planner.dto.TrainingDTO;
import com.iso27001planner.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trainings")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @PostMapping("/create")
    public ResponseEntity<TrainingDTO> create(@RequestBody TrainingDTO dto,
                                              @RequestParam Long companyId) {
        return ResponseEntity.ok(trainingService.createTraining(dto, companyId));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<TrainingDTO>> list(@PathVariable Long companyId) {
        return ResponseEntity.ok(trainingService.listTrainings(companyId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<Void> updateTraining(@PathVariable UUID id, @RequestBody TrainingDTO dto) {
        trainingService.updateTraining(id, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<Void> deleteTraining(@PathVariable UUID id) {
        trainingService.deleteTraining(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{trainingId}/assign/{employeeId}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<String> assignEmployeeToTraining(
            @PathVariable UUID trainingId,
            @PathVariable UUID employeeId
    ) {
        trainingService.assignEmployeeToTraining(trainingId, employeeId);
        return ResponseEntity.ok("Employee assigned to training.");
    }
}
