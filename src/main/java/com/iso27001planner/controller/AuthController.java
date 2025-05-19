package com.iso27001planner.controller;

import com.iso27001planner.dto.*;
import com.iso27001planner.entity.User;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.UserRepository;
import com.iso27001planner.service.AuthService;
import com.iso27001planner.service.MFAService;
import com.iso27001planner.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidKeyException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MFAService mfaService;
    private final UserRepository userRepository;
    private final UserManagementService userManagementService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public TokenResponse refreshToken(@RequestBody RefreshTokenRequest request) {
        return authService.refreshAccessToken(request.getRefreshToken());
    }


    @PostMapping("/logout-user")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ISMS_ADMIN')")
    public ResponseEntity<String> logoutUserByAdmin(@RequestParam String email) {
        userManagementService.logoutUserByEmail(email);
        return ResponseEntity.ok("User " + email + " has been logged out.");
    }

    @PostMapping("/logout-self")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> logoutSelf() {
        userManagementService.logoutSelf();
        return ResponseEntity.ok("You have been logged out successfully.");
    }

}
