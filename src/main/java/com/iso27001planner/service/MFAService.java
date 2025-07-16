package com.iso27001planner.service;

import com.iso27001planner.dto.AuthenticationResponse;
import com.iso27001planner.dto.MFARequest;
import com.iso27001planner.entity.RefreshToken;
import com.iso27001planner.entity.User;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.UserRepository;
import com.iso27001planner.util.JwtService;
import com.iso27001planner.util.MFAUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MFAService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EmailSenderService emailSenderService;
    private final ApplicationEventPublisher eventPublisher;
    private final RefreshTokenService refreshTokenService;

    /**
     * Verifies the user's submitted OTP and returns a token if correct.
     */
    @Transactional
    public AuthenticationResponse verifyEmailOtp(MFARequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        Instant now = Instant.now();

        if (!user.isMfaEnabled()) {
            throw new BusinessException("MFA is not enabled for this account", HttpStatus.BAD_REQUEST);
        }

        // Check if user is currently locked
        if (user.getOtpLockUntil() != null && now.isBefore(user.getOtpLockUntil())) {
            throw new BusinessException("Too many failed OTP attempts. Try again at " + user.getOtpLockUntil(), HttpStatus.FORBIDDEN);
        }

        // Expired or missing OTP
        if (user.getMfaSecret() == null || user.getMfaExpiryTime() == null || now.isAfter(user.getMfaExpiryTime())) {
            throw new BusinessException("MFA code expired or not generated", HttpStatus.UNAUTHORIZED);
        }

        // Incorrect OTP — track brute-force attempts
        if (!user.getMfaSecret().equals(request.getCode())) {
            int attempts = user.getFailedOtpAttempts() + 1;
            user.setFailedOtpAttempts(attempts);

            if (attempts >= 5) {
                user.setOtpLockUntil(now.plus(Duration.ofMinutes(5))); // Lock for 5 mins
                user.setFailedOtpAttempts(0); // reset count after lock
                userRepository.save(user);
                throw new BusinessException("Too many incorrect attempts. Account locked for 5 minutes.", HttpStatus.FORBIDDEN);
            }

            userRepository.save(user);
            throw new BusinessException("Invalid OTP code", HttpStatus.UNAUTHORIZED);
        }

        // OTP is correct — clean up and login
        user.setMfaSecret(null);
        user.setMfaExpiryTime(null);
        user.setOtpLockUntil(null);
        user.setFailedOtpAttempts(0);
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user); // new refresh token

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "VERIFY_MFA",
                request.getEmail(),
                "User",
                user.getEmail(),
                "MFA verified and proceed to login"
        ));

        return new AuthenticationResponse(accessToken, refreshToken.getToken(), true);
    }

    /**
     * Enables MFA for the user by generating a one-time OTP and sending it by email.
     */
    @Transactional
    public String enableEmailMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        user.setMfaEnabled(true);
        user.setMfaSecret(null);         // clean any old secrets
        user.setMfaExpiryTime(null);
        userRepository.save(user);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "ENABLE_MFA",
                 email,
                "User",
                 user.getId().toString(),
                "MFA enabled by user"
        ));
        return "Email MFA enabled successfully.";
    }

    @Transactional
    public String generateEmailOtp(User user) {
        int otp = (int)(Math.random() * 900000) + 100000;
        user.setMfaSecret(String.valueOf(otp));
        userRepository.save(user);
        emailSenderService.sendOtp(user.getEmail(), String.valueOf(otp));
        return "OTP sent via email";
    }

    @Transactional
    public void triggerLoginOtp(User user) {
        Instant now = Instant.now();

        if (user.getLastOtpSentAt() != null) {
            long secondsSinceLastSend = Duration.between(user.getLastOtpSentAt(), now).getSeconds();

            if (secondsSinceLastSend < 60) {
                long waitTime = 60 - secondsSinceLastSend;
                throw new BusinessException("Please wait " + waitTime + "s before requesting a new OTP", HttpStatus.TOO_MANY_REQUESTS);
            }
        }

        String otp = MFAUtil.generateOtp();
        Instant expiry = now.plus(Duration.ofMinutes(5));

        user.setMfaSecret(otp);
        user.setMfaExpiryTime(expiry);
        user.setLastOtpSentAt(now); // Track send time
        userRepository.save(user);

        emailSenderService.sendOtp(user.getEmail(), otp);
        eventPublisher.publishEvent(new AuditEvent(
                this,
                "Sending MFA code",
                "system",
                "User",
                user.getId().toString(),
                "MFA code sent by email to user"
        ));
    }

    @Transactional
    public String disableMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        if (!user.isMfaEnabled()) {
            throw new BusinessException("MFA is already disabled", HttpStatus.BAD_REQUEST);
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaExpiryTime(null);
        userRepository.save(user);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "DISABLE_MFA",
                 email,
                "User",
                 user.getId().toString(),
                "MFA disabled by user"
        ));
        return "Email MFA disabled successfully.";
    }

    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        if (!user.isMfaEnabled()) {
            throw new BusinessException("MFA is not enabled for this user", HttpStatus.BAD_REQUEST);
        }

        // Optional: prevent abuse (e.g. within 1 minute)
        if (user.getMfaExpiryTime() != null && Instant.now().isBefore(user.getMfaExpiryTime().minusSeconds(240))) {
            throw new BusinessException("Please wait before requesting another code", HttpStatus.TOO_MANY_REQUESTS);
        }

        String otp = MFAUtil.generateOtp();
        Instant expiry = Instant.now().plus(Duration.ofMinutes(5));

        user.setMfaSecret(otp);
        user.setMfaExpiryTime(expiry);
        userRepository.save(user);

        emailSenderService.sendOtp(user.getEmail(), otp);

        eventPublisher.publishEvent(new AuditEvent(
                this, "RESEND_MFA_OTP", user.getEmail(), "MFA", null, "Resent OTP code"
        ));
    }

    @Transactional
    public void verifyMfaSetupCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        Instant now = Instant.now();

        if (user.getMfaSecret() == null || user.getMfaExpiryTime() == null || now.isAfter(user.getMfaExpiryTime())) {
            throw new BusinessException("MFA code expired or not generated", HttpStatus.UNAUTHORIZED);
        }

        if (!user.getMfaSecret().equals(code)) {
            throw new BusinessException("Invalid OTP code", HttpStatus.UNAUTHORIZED);
        }

        // OTP is correct — enable MFA
        user.setMfaEnabled(true);
        user.setMfaSecret(null);
        user.setMfaExpiryTime(null);
        user.setFailedOtpAttempts(0);
        user.setOtpLockUntil(null);
        userRepository.save(user);
    }
}

