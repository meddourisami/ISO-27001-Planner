package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.w3c.dom.Text;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Control {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    @Column(length = 5000)
    private String description;
    private String status;       // e.g. implemented, not_implemented, in_progress
    private String evidence;

    private LocalDate lastReview;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}
