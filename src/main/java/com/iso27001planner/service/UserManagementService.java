package com.iso27001planner.service;

import com.iso27001planner.dto.*;
import com.iso27001planner.entity.Role;
import com.iso27001planner.entity.User;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;


    @Transactional
    public String updateUserRole(RoleUpdateRequest request) {
        User actingUser = getAuthenticatedUser();

        User targetUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        Role requestedRole = Role.valueOf(request.getNewRole());

        if (actingUser.getRole() == Role.SUPER_ADMIN) {
            // Super admin can do anything
        } else if (actingUser.getRole() == Role.ISMS_ADMIN) {
            // Company admin can only update users in their own company
            if (targetUser.getCompany() == null || !targetUser.getCompany().getId().equals(actingUser.getCompany().getId())) {
                throw new BusinessException("You are not allowed to update this user", HttpStatus.FORBIDDEN);
            }

            if (requestedRole == Role.SUPER_ADMIN) {
                throw new BusinessException("Company Admin cannot assign SUPER_ADMIN role", HttpStatus.FORBIDDEN);
            }
        } else {
            throw new BusinessException("You are not allowed to assign roles", HttpStatus.FORBIDDEN);
        }

        targetUser.setRole(requestedRole);
        userRepository.save(targetUser);
        eventPublisher.publishEvent(new AuditEvent(
                this,
                "UPDATE_ROLE",
                 actingUser.getEmail(),
                "User",
                 targetUser.getEmail(),
                "Changed role to: " + requestedRole
        ));
        return "User role updated to " + requestedRole;
    }

    public User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Auth error", HttpStatus.UNAUTHORIZED));
    }

    @Transactional
    public void logoutSelf() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Authenticated user not found", HttpStatus.UNAUTHORIZED));

        refreshTokenService.revokeToken(user);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "LOGOUT_SELF",
                email,
                "User",
                email,
                "User logged out from their own session"
        ));
    }

    @Transactional
    public void logoutUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        refreshTokenService.revokeToken(user);

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "LOGOUT_USER",
                adminEmail,
                "User",
                user.getEmail(),
                "Admin " + adminEmail + " logged out user " + email
        ));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

        // 🔐 NEW: Prevent using the same password again
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("New password must be different from the current password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "CHANGE_PASSWORD",
                email,
                "User",
                email,
                "User changed password"
        ));
    }

    @Transactional
    public void editUser(String adminEmail, EditUserRequest request) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException("Admin not found", HttpStatus.UNAUTHORIZED));

        User target = userRepository.findByEmail(request.getTargetEmail())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        if (!target.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new BusinessException("Cannot modify users outside your company", HttpStatus.FORBIDDEN);
        }

        if (target.getRole() == Role.ISMS_ADMIN || target.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException("Cannot edit other admins", HttpStatus.FORBIDDEN);
        }

        target.setEmail(request.getEmail());
        target.setFullName(request.getFullName());
        target.setRole(Role.valueOf(request.getRole()));

        userRepository.save(target);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "EDIT_USER",
                adminEmail,
                "User",
                request.getEmail(),
                "Edited user profile: " + request.getEmail()
        ));
    }

    @Transactional
    public void deleteUser(String adminEmail, String userEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException("Admin not found", HttpStatus.UNAUTHORIZED));

        User target = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        if (!target.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new BusinessException("Cannot delete users outside your company", HttpStatus.FORBIDDEN);
        }

        if (target.getRole() == Role.ISMS_ADMIN || target.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException("Cannot delete admins", HttpStatus.FORBIDDEN);
        }

        refreshTokenService.revokeToken(target); // optional cleanup
        userRepository.delete(target);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "DELETE_USER",
                adminEmail,
                "User",
                userEmail,
                "Deleted user: " + userEmail
        ));
    }

    public Page<UserDTO> listCompanyUsers(String adminEmail, Pageable pageable, String roleFilter) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException("Admin not found", HttpStatus.UNAUTHORIZED));

        Page<User> users;
        if (roleFilter != null && !roleFilter.isBlank()) {
            users = userRepository.findByCompanyAndRole(admin.getCompany(), Role.valueOf(roleFilter), pageable);
        } else {
            users = userRepository.findByCompany(admin.getCompany(), pageable);
        }

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "LIST_USERS",
                adminEmail,
                "User",
                "-",
                "Listed company users (filter=" + (roleFilter != null ? roleFilter : "none") + ")"
        ));

        return users.map(this::mapToDto);
    }

    @Modifying
    @Transactional
    public void updateOwnProfile(String currentEmail, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        String existingEmail = Optional.ofNullable(user.getEmail()).orElse("");
        String existingName = Optional.ofNullable(user.getFullName()).orElse("");
        String newEmail = Optional.ofNullable(request.getEmail()).orElse("");
        String newName = Optional.ofNullable(request.getFullName()).orElse("");

        log.debug("Current email: {}", existingEmail);
        log.debug("New email: {}", newEmail);
        log.debug("Current name: {}", existingName);
        log.debug("New name: {}", newName);

        boolean emailChanged = !existingEmail.equals(newEmail);
        boolean nameChanged = !existingName.equals(newName);

        log.debug("Email changed? {}", emailChanged);
        log.debug("Name changed? {}", nameChanged);

        if (!emailChanged && !nameChanged) {
            log.info("No changes detected. Skipping save.");
            return;
        }

        if (emailChanged && userRepository.existsByEmail(newEmail)) {
            throw new BusinessException("Email already in use", HttpStatus.CONFLICT);
        }

        if (emailChanged) {
            user.setEmail(newEmail);
        }

        if (nameChanged) {
            user.setFullName(newName);
        }

        userRepository.save(user);

        log.info("User profile updated: {}", user.getId());

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "UPDATE_PROFILE",
                currentEmail,
                "User",
                user.getId().toString(),
                "User updated profile" + (emailChanged ? " (email changed)" : "") + (nameChanged ? " (name changed)" : "")
        ));
    }

    public UserDTO mapToDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(String.valueOf(user.getRole()));
        return dto;
    }
}
