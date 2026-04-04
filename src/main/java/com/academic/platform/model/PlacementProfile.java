package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "placement_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", unique = true, nullable = false)
    private User student;

    @Builder.Default
    private Boolean resumeUploaded = false;

    @Column(columnDefinition = "TEXT")
    private String resumeUrl;

    @Builder.Default
    private Integer skillsCompleted = 0;
    @Builder.Default
    private Integer totalSkills = 10;

    // Skills list (comma-separated or JSON string)
    @Column(columnDefinition = "TEXT")
    private String completedSkillsList;

    @Builder.Default
    private Double aptitudeScore = 0.0;      // 0-100
    @Builder.Default
    private Double mockInterviewScore = 0.0;  // 0-100
    @Builder.Default
    private Double cgpaScore = 0.0;           // stored from student profile

    @Column(columnDefinition = "TEXT")
    private String activityScoresJson;

    @Column(columnDefinition = "TEXT")
    private String placementRoundsJson;

    @Builder.Default
    private String placementStatus = "NOT_READY";

    @Builder.Default
    private String resumeReviewStatus = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String resumeRemarks;

    // Career preferences
    @Column(length = 200)
    private String preferredRole;

    @Column(length = 200)
    private String preferredCompanies;

    private LocalDateTime updatedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (resumeUploaded == null) resumeUploaded = false;
        if (skillsCompleted == null) skillsCompleted = 0;
        if (totalSkills == null) totalSkills = 10;
        if (aptitudeScore == null) aptitudeScore = 0.0;
        if (mockInterviewScore == null) mockInterviewScore = 0.0;
        if (placementStatus == null || placementStatus.isBlank()) placementStatus = "NOT_READY";
        if (resumeReviewStatus == null || resumeReviewStatus.isBlank()) resumeReviewStatus = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Weighted readiness formula:
     * Resume: 20%, Skills: 30%, Aptitude: 25%, Mock Interview: 25%
     */
    @Transient
    public double getReadinessScore() {
        double resumePoints = Boolean.TRUE.equals(resumeUploaded) ? 20.0 : 0.0;
        double skillPoints = totalSkills != null && totalSkills > 0
                ? (skillsCompleted.doubleValue() / totalSkills) * 30.0 : 0.0;
        double aptitudePoints = aptitudeScore != null ? (aptitudeScore / 100.0) * 25.0 : 0.0;
        double interviewPoints = mockInterviewScore != null ? (mockInterviewScore / 100.0) * 25.0 : 0.0;
        return Math.round((resumePoints + skillPoints + aptitudePoints + interviewPoints) * 100.0) / 100.0;
    }
}
