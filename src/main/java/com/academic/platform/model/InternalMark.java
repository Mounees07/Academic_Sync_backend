package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "internal_marks", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "student_id", "section_id" })
}, indexes = {
        @Index(name = "idx_imark_student", columnList = "student_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalMark {

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

    // Assignment marks
    @Builder.Default
    private Double assignmentMarks = 0.0;
    @Builder.Default
    private Double maxAssignment = 20.0;

    // Unit Test 1
    @Builder.Default
    private Double ut1Marks = 0.0;
    @Builder.Default
    private Double maxUt = 50.0;

    // Unit Test 2
    @Builder.Default
    private Double ut2Marks = 0.0;

    // Model exam
    @Builder.Default
    private Double modelMarks = 0.0;
    @Builder.Default
    private Double maxModel = 100.0;

    private Integer semester;

    @Column(length = 10)
    private String academicYear;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        setDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private void setDefaults() {
        if (assignmentMarks == null) assignmentMarks = 0.0;
        if (ut1Marks == null) ut1Marks = 0.0;
        if (ut2Marks == null) ut2Marks = 0.0;
        if (modelMarks == null) modelMarks = 0.0;
        if (maxAssignment == null) maxAssignment = 20.0;
        if (maxUt == null) maxUt = 50.0;
        if (maxModel == null) maxModel = 100.0;
    }

    @Transient
    public double getTotalInternalScore() {
        return (assignmentMarks != null ? assignmentMarks : 0)
                + (ut1Marks != null ? ut1Marks : 0)
                + (ut2Marks != null ? ut2Marks : 0)
                + (modelMarks != null ? modelMarks : 0);
    }

    @Transient
    public double getMaxTotalScore() {
        return (maxAssignment != null ? maxAssignment : 0)
                + (maxUt != null ? maxUt : 0)
                + (maxUt != null ? maxUt : 0)  // UT2 same max
                + (maxModel != null ? maxModel : 0);
    }

    @Transient
    public double getPercentageScore() {
        if (getMaxTotalScore() == 0) return 0;
        return Math.round((getTotalInternalScore() / getMaxTotalScore()) * 10000.0) / 100.0;
    }
}
