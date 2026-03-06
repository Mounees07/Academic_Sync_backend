package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private String subjectCode;
    private String subjectName;
    private String grade; // e.g. "A+", "O", "9.5"
    private Integer credits;
    private Integer semester;

    @Column(name = "exam_type") // INTERNAL, SEMESTER
    private String examType; // Could be enum, simplified to String for CSV ease

    private LocalDate publishedDate;
}
