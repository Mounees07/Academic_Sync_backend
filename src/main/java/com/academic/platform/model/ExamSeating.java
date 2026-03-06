package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exam_seatings", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "exam_id", "student_id" }) // One seat per exam per student
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSeating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id", nullable = false)
    private AcademicSchedule exam;

    @ManyToOne
    @JoinColumn(name = "venue_id", nullable = false)
    private ExamVenue venue;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private String seatNumber; // Optional, can be auto-generated or left null
}
