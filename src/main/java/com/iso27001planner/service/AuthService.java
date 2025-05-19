package com.iso27001planner.service;

import com.iso27001planner.dto.AuthenticationRequest;
import com.iso27001planner.dto.AuthenticationResponse;
import com.iso27001planner.dto.TokenResponse;
import com.iso27001planner.entity.RefreshToken;
import com.iso27001planner.entity.User;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.RefreshTokenRepository;
import com.iso27001planner.repository.UserRepository;
import com.iso27001planner.util.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.SQLOutput;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MFAService mfaService;
    //private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    private RefreshTokenService refreshTokenService;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    eventPublisher.publishEvent(new AuditEvent(
                            this, "LOGIN_FAILED", request.getEmail(), "User", "-", "Login failed: user not found"
                    ));
                    throw new BusinessException("User not found", HttpStatus.NOT_FOUND);
                });


        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            eventPublisher.publishEvent(new AuditEvent(
                    this, "LOGIN_FAILED", request.getEmail(), "User", user.getId().toString(), "Login failed: incorrect password"
            ));
            throw new BusinessException("Authentication failed: password incorrect", HttpStatus.BAD_REQUEST);
        }

        if (user.isMfaEnabled()) {
            mfaService.triggerLoginOtp(user); // Generates + emails OTP
            eventPublisher.publishEvent(new AuditEvent(
                    this, "LOGIN_MFA_TRIGGERED", request.getEmail(), "User", user.getId().toString(),
                    "Login attempt requires MFA verification"
            ));
            return new AuthenticationResponse(null, null, true); // No token yet
        }

        String token = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        eventPublisher.publishEvent(new AuditEvent(
                this, "LOGIN_SUCCESS", request.getEmail(), "User", user.getId().toString(), "Login successful"
        ));

        return new AuthenticationResponse(token, refreshToken.getToken(), false);
    }

    public TokenResponse refreshAccessToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    eventPublisher.publishEvent(new AuditEvent(
                            this, "REFRESH_TOKEN_INVALID", getCurrentUserEmail(), "User", "-", "Refresh token invalid"
                    ));
                    throw new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
                });

        if (refreshTokenService.isTokenExpired(token)) {
            eventPublisher.publishEvent(new AuditEvent(
                    this, "REFRESH_TOKEN_EXPIRED", token.getUser().getEmail(), "User", token.getUser().getId().toString(),
                    "Attempted to use expired refresh token"
            ));
            throw new BusinessException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

        String newAccessToken = jwtService.generateToken(token.getUser());

        eventPublisher.publishEvent(new AuditEvent(
                this, "REFRESH_TOKEN_USED", token.getUser().getEmail(), "User", token.getUser().getId().toString(),
                "Refresh token used to generate new access token"
        ));
        return new TokenResponse(newAccessToken, refreshToken);
    }

    public void logoutCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.UNAUTHORIZED));

        refreshTokenService.revokeToken(user); // ✅ Remove associated refresh token

        // Audit
        eventPublisher.publishEvent(new AuditEvent(
                this,
                "LOGOUT",
                email,
                "User",
                user.getId().toString(),
                "User logged out"
        ));
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}
