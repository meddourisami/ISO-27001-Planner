package com.iso27001planner.controller;

import com.iso27001planner.dto.TaskDTO;
import com.iso27001planner.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'ISMS_USER')")
    public ResponseEntity<TaskDTO> create(@RequestBody TaskDTO dto) {
        return ResponseEntity.ok(taskService.createTask(dto));
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskDTO>> list(@PathVariable Long companyId) {
        return ResponseEntity.ok(taskService.listCompanyTasks(companyId));
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<Void> updateProgress(@PathVariable UUID id, @RequestParam int progress) {
        taskService.updateProgress(id, progress);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        taskService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

//    @PostMapping("/{taskId}/comments")
//    public ResponseEntity<Void> comment(@PathVariable UUID taskId, @RequestBody TaskCommentDTO dto) {
//        taskService.addComment(taskId, dto);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/{taskId}/comments")
//    public ResponseEntity<List<TaskCommentDTO>> listComments(@PathVariable UUID taskId) {
//        return ResponseEntity.ok(taskService.getComments(taskId));
//    }
}