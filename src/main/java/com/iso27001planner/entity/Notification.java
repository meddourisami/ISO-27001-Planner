package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;       // "risk", "system", etc.

    private String title;      // "High Risk Identified"

    private String description;// Full message

    private String category;   // "reminder", "alert", "update"

    @Column(name = "`read`") // backticks escape the column name
    private boolean read;

    private LocalDateTime sentAt;

    private String targetEmail;

    @ManyToOne
    @JoinColumn(name = "risk_id")
    private Risk risk;
}
