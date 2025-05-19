package com.iso27001planner.controller;

import com.iso27001planner.dto.AuthenticationResponse;
import com.iso27001planner.dto.MFARequest;
import com.iso27001planner.service.MFAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
public class MFAController {

    private final MFAService mfaService;

    /**
     * Enable email MFA for the current user. Sends OTP via email.
     */
    @PostMapping("/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> enableMfa() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = mfaService.enableEmailMfa(email);
        return ResponseEntity.ok(message);
    }

    /**
     * Verify the OTP submitted by the user.
     */
    @PostMapping("/verify")
    public ResponseEntity<AuthenticationResponse> verifyOtp(@RequestBody MFARequest request) {
        AuthenticationResponse response = mfaService.verifyEmailOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> disableMfa() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(mfaService.disableMfa(email));
    }
}
