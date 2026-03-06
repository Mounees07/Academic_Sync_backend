package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "academic_calendar")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicCalendar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "Fall 2025"

    @Column(nullable = false)
    private LocalDate startDate; // Semester start

    @Column(nullable = false)
    private LocalDate endDate; // Semester end

    private LocalDate examStartDate;
    private LocalDate examEndDate;

    private boolean isCurrent; // Is this the active semester?

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String status; // PLANNED, ACTIVE, COMPLETED, ARCHIVED
}
