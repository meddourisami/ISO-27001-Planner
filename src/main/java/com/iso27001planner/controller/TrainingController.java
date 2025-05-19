package com.iso27001planner.controller;

import com.iso27001planner.dto.TrainingDTO;
import com.iso27001planner.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
