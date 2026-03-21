package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "placement_drive_applications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_drive_student_application", columnNames = {"drive_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_drive_application_drive", columnList = "drive_id"),
                @Index(name = "idx_drive_application_student", columnList = "student_id"),
                @Index(name = "idx_drive_application_status", columnList = "status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementDriveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drive_id", nullable = false)
    private PlacementDrive drive;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ELIGIBLE";

    @Builder.Default
    @Column(nullable = false)
    private Integer reminderCount = 0;

    private LocalDateTime lastReminderSentAt;

    private LocalDateTime mentorNotifiedAt;

    private LocalDateTime appliedAt;

    private LocalDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String coordinatorRemarks;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null || status.isBlank()) {
            status = "ELIGIBLE";
        }
        if (reminderCount == null) {
            reminderCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
