package com.iso27001planner.controller;

import com.iso27001planner.dto.AuthenticationResponse;
import com.iso27001planner.dto.MFARequest;
import com.iso27001planner.entity.User;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.UserRepository;
import com.iso27001planner.service.MFAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
public class MFAController {

    private final MFAService mfaService;
    private final UserRepository userRepository;

    /**
     * Enable email MFA for the current user. Sends OTP via email.
     */
    @PostMapping("/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> enableMfa() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println(email + "This is the mail ");
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

    @PostMapping("/resend")
    public ResponseEntity<String> resendOtp(@RequestParam String email) {
        mfaService.resendOtp(email);
        return ResponseEntity.ok("A new OTP has been sent to your email.");
    }

    @PostMapping("/verify-setup")
    public ResponseEntity<String> verifyMfaSetupCode(@RequestBody MFARequest request) {
        mfaService.verifyMfaSetupCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok("MFA has been successfully enabled.");
    }
}
