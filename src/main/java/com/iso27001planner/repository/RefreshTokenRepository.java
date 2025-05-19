package com.iso27001planner.repository;

import com.iso27001planner.entity.RefreshToken;
import com.iso27001planner.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    @Modifying
    @Transactional
    @Query("delete from RefreshToken rt where rt.user = :user")
    void deleteByUser(@Param("user") User user);
}
