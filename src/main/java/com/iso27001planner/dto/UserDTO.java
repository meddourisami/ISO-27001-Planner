package com.iso27001planner.dto;

import com.iso27001planner.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private boolean mfaEnabled;
    private Long companyId;

}
