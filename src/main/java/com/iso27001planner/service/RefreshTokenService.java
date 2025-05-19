package com.iso27001planner.service;

import com.iso27001planner.entity.RefreshToken;
import com.iso27001planner.entity.User;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Deletes any existing refresh token and creates a new one for the user.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        System.out.println("[DEBUG] Deleting old token for: " + user.getEmail());
        refreshTokenRepository.deleteByUser(user);
        System.out.println("[DEBUG] Creating new token...");

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60)); // 7 days

        return refreshTokenRepository.save(token);
    }

    /**
     * Checks if a token is expired.
     */
    public boolean isTokenExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }

    /**
     * Loads a token by string value.
     */
    public RefreshToken getToken(String tokenStr) {
        return refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED));
    }

    /**
     * Deletes a refresh token for logout or admin revocation.
     */
    public void revokeToken(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Deletes a token explicitly by token value.
     */
    public void revokeByToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        refreshToken.ifPresent(refreshTokenRepository::delete);
    }
}
