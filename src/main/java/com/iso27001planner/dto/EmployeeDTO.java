package com.iso27001planner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDTO {
    private String id;
    private String name;
    private String department;
    private String email;
    private List<String> completedTrainings;

    //public EmployeeDTO(String string, String name, String department, String email, List<String> completed) {
    //}
}
