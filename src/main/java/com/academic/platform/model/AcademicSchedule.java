package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "academic_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private ScheduleType type;

    private LocalDate date;

    private String session; // FN or AN
    private LocalTime startTime;
    private LocalTime endTime;

    private String subjectName;
    private String rollNoRange; // e.g. "101-160"

    private String location;
    private String description;

    // Who uploaded it (Department head)
    private String department;

    public enum ScheduleType {
        ACADEMIC,
        LAB_SLOT,
        SKILL_TRAINING,
        INTERNAL_EXAM,
        SEMESTER_EXAM,
        LAB_PRACTICAL,
        FACULTY_MEETING
    }
}
