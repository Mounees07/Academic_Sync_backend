package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "subject_attendance", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "student_id", "section_id", "month", "year" })
}, indexes = {
        @Index(name = "idx_subatt_student", columnList = "student_id"),
        @Index(name = "idx_subatt_section", columnList = "section_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false, length = 100)
    private String subjectName;

    @Column(length = 20)
    private String subjectCode;

    @Column(nullable = false)
    private Integer month; // 1-12

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer totalClasses = 0;

    @Column(nullable = false)
    private Integer attendedClasses = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalClasses == null) totalClasses = 0;
        if (attendedClasses == null) attendedClasses = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Computed — not stored, calculated in Java
    @Transient
    public double getPercentage() {
        if (totalClasses == null || totalClasses == 0) return 0.0;
        return Math.round((attendedClasses.doubleValue() / totalClasses) * 10000.0) / 100.0;
    }

    // Color indicator: GREEN > 80%, YELLOW 75-80%, RED < 75%
    @Transient
    public String getColorStatus() {
        double pct = getPercentage();
        if (pct >= 80) return "GREEN";
        if (pct >= 75) return "YELLOW";
        return "RED";
    }
}
