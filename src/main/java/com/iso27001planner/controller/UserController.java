package com.iso27001planner.controller;

import com.iso27001planner.dto.*;
import com.iso27001planner.entity.Role;
import com.iso27001planner.entity.User;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.mapper.UserMapper;
import com.iso27001planner.repository.UserRepository;
import com.iso27001planner.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("/update-role")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public String updateRole(@RequestBody RoleUpdateRequest request) {
        return userManagementService.updateUserRole(request);
    }

    @PostMapping("/register-member")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<String> registerMember(@RequestBody RegisterMemberRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Admin not found", HttpStatus.UNAUTHORIZED));

        if (admin.getCompany() == null) {
            throw new BusinessException("Admin does not belong to a company", HttpStatus.FORBIDDEN);
        }

        if (!request.getRole().equals("ISMS_USER") && !request.getRole().equals("AUDITOR")) {
            throw new BusinessException("You can only register ISMS_USER or AUDITOR roles", HttpStatus.FORBIDDEN);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(Role.valueOf(request.getRole()));
        newUser.setCompany(admin.getCompany());
        newUser.setMfaEnabled(false);

        userRepository.save(newUser);
        return ResponseEntity.ok("User registered under your company.");
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        if (request.getNewPassword().length() < 8) {
            throw new BusinessException("Password must be at least 8 characters long", HttpStatus.BAD_REQUEST);
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userManagementService.changePassword(email, request);

        return ResponseEntity.ok("Password updated successfully.");
    }

    @PutMapping("/edit-member")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<String> editUser(@RequestBody EditUserRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userManagementService.editUser(email, request);
        return ResponseEntity.ok("User updated successfully.");
    }

    @DeleteMapping("/delete-member")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<String> deleteUser(@RequestParam String email) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        userManagementService.deleteUser(adminEmail, email);
        return ResponseEntity.ok("User deleted successfully.");
    }

    @GetMapping("/list-members")
    @PreAuthorize("hasAnyAuthority('ISMS_USER', 'ISMS_ADMIN')")
    public ResponseEntity<PaginatedResponse<UserDTO>> listMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDTO> resultPage = userManagementService.listCompanyUsers(email, pageable, role);

        PaginatedResponse<UserDTO> response = new PaginatedResponse<>(
                resultPage.getContent(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.UNAUTHORIZED));

        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @PutMapping("/update-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> updateProfile(@RequestBody UpdateProfileRequest request) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        userManagementService.updateOwnProfile(currentEmail, request);
        return ResponseEntity.ok("Profile updated successfully.");
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ISMS_ADMIN','ISMS_USER')") // Optional: secure if needed
    public ResponseEntity<List<String>> getRoles() {
        List<String> roles = Arrays.stream(Role.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/admins")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<UserDTO>> listIsmsAdmins() {
        return ResponseEntity.ok(userManagementService.listIsmsAdmins());
    }
}
