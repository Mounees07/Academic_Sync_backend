package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ExamVenue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String block;
    private Integer capacity;

    // e.g., "Semester", "Internal", "Lab", "All"
    private String examType;

    private boolean isAvailable = true;
}
