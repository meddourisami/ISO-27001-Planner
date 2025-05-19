package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String author; // email or name
    private String content;
    private LocalDateTime postedAt;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;
}
