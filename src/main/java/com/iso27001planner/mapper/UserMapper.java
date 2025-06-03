package com.iso27001planner.mapper;

import com.iso27001planner.dto.UserDTO;
import com.iso27001planner.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public static UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole().name());
        dto.setCompanyId(user.getCompany() != null ? user.getCompany().getId() : null);
        dto.setMfaEnabled(user.isMfaEnabled());
        return dto;
    }
}
