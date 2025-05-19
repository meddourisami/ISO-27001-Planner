package com.iso27001planner.controller;

import com.iso27001planner.dto.EmployeeDTO;
import com.iso27001planner.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeDTO> register(@RequestBody EmployeeDTO dto,
                                                @RequestParam Long companyId) {
        return ResponseEntity.ok(employeeService.register(dto, companyId));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<EmployeeDTO>> list(@PathVariable Long companyId) {
        return ResponseEntity.ok(employeeService.listByCompany(companyId));
    }

    @PostMapping("/{employeeId}/complete-training/{trainingId}")
    public ResponseEntity<Void> markTrainingCompleted(@PathVariable UUID employeeId,
                                                      @PathVariable UUID trainingId) {
        employeeService.markTrainingCompleted(employeeId, trainingId);
        return ResponseEntity.ok().build();
    }
}
