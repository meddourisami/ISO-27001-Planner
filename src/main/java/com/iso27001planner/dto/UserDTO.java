package com.iso27001planner.dto;

import com.iso27001planner.entity.User;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private Long companyId;

}
