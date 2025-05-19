package com.iso27001planner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainingDTO {
    private String id;
    private String title;
    private String description;
    private String type;
    private String status;
    private String startDate;
    private String endDate;
    private int duration;
    private String instructor;
    private String materials;
    private List<String> requiredFor;

    //public TrainingDTO(String string, String title, String description, String type, String status, String string1, String string2, int duration, String instructor, String materials, List<String> requiredFor) {
    //}
}
