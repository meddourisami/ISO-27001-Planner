package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name= "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

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
