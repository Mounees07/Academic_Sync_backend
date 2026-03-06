package com.academic.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_workload", uniqueConstraints = @UniqueConstraint(columnNames = "faculty_id"))
public class FacultyWorkload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    @Column(nullable = false)
    private Integer teachingHours = 0;

    @Column(nullable = false)
    private Integer researchHours = 0;

    @Column(nullable = false)
    private Integer adminHours = 0;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getFaculty() {
        return faculty;
    }

    public void setFaculty(User faculty) {
        this.faculty = faculty;
    }

    public Integer getTeachingHours() {
        return teachingHours;
    }

    public void setTeachingHours(Integer teachingHours) {
        this.teachingHours = teachingHours;
    }

    public Integer getResearchHours() {
        return researchHours;
    }

    public void setResearchHours(Integer researchHours) {
        this.researchHours = researchHours;
    }

    public Integer getAdminHours() {
        return adminHours;
    }

    public void setAdminHours(Integer adminHours) {
        this.adminHours = adminHours;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
