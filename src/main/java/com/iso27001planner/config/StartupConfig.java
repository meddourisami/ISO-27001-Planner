package com.iso27001planner.config;

import com.iso27001planner.entity.Role;
import com.iso27001planner.entity.User;
import com.iso27001planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupConfig implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail("superadmin@planner.com").isEmpty()) {
            User superAdmin = new User();
            superAdmin.setEmail("superadmin@planner.com");
            superAdmin.setPassword(passwordEncoder.encode("SuperSecurePassword123!"));
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setMfaEnabled(false);
            superAdmin.setCompany(null);

            userRepository.save(superAdmin);

            System.out.println("✅ Super Admin created: superadmin@planner.com / SuperSecurePassword123!");
        }
    }
}
