package com.iso27001planner.service;

import com.iso27001planner.entity.AuditLog;
import com.iso27001planner.entity.User;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.AuditEventRepository;
import com.iso27001planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditEventRepository auditLogRepo;
    private final UserRepository userRepo;

    public List<AuditLog> getRecentCompanyLogs(int limit) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // Try to load user from DB
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Authenticated user not found", HttpStatus.UNAUTHORIZED));

        Long companyId = user.getCompany().getId();

        return auditLogRepo.findTop50ByOrderByTimestampDesc().stream()
                .filter(log -> isLogRelatedToCompany(log, companyId))
                .limit(limit)
                .toList();
    }

    private boolean isLogRelatedToCompany(AuditLog log, Long companyId) {
        String actor = log.getActorEmail();

        if (actor == null || actor.equalsIgnoreCase("system")) {
            System.out.println("🛑 Skipping log: actor is null or 'system'");
            return false;
        }

        Optional<User> userOpt = userRepo.findByEmail(actor);
        if (userOpt.isEmpty()) {
            System.out.println("⚠️ No user found for actor: " + actor);
            return false;
        }

        User user = userOpt.get();

        if (user.getCompany() == null) {
            System.out.println("⚠️ User has no company: " + user.getEmail());
            return false;
        }

        Long userCompanyId = user.getCompany().getId();
        if (userCompanyId == null) {
            System.out.println("🚨 User's company has null ID: " + user.getEmail());
            return false;
        }

        boolean match = userCompanyId.equals(companyId);
        System.out.println("🔎 Comparing companyId for " + user.getEmail() + ": " + userCompanyId + " == " + companyId + " → " + match);
        return match;
    }
}
