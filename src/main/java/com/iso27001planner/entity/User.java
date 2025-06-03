package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name= "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", updatable = true)
    private String fullName;

    @Column(name = "email", unique = true, updatable = true) // ✅ must not be false
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean mfaEnabled;

    private String mfaSecret;

    private Instant mfaExpiryTime;

    private Instant lastOtpSentAt;

    private int failedOtpAttempts = 0;

    private Instant otpLockUntil;

    @ManyToOne
    private Company company;

    // getters and setters
}
