package com.academic.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    private LocalDateTime enrollmentDate;

    @Column(name = "change_count")
    private Integer changeCount = 0;

    @Column(name = "last_updated_date")
    private LocalDateTime lastUpdatedDate;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public Integer getChangeCount() {
        return changeCount;
    }

    public void setChangeCount(Integer changeCount) {
        this.changeCount = changeCount;
    }

    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    // Manual Builder
    public static EnrollmentBuilder builder() {
        return new EnrollmentBuilder();
    }

    public static class EnrollmentBuilder {
        private User student;
        private Section section;
        private LocalDateTime enrollmentDate;
        private Integer changeCount;
        private LocalDateTime lastUpdatedDate;

        public EnrollmentBuilder student(User student) {
            this.student = student;
            return this;
        }

        public EnrollmentBuilder section(Section section) {
            this.section = section;
            return this;
        }

        public EnrollmentBuilder enrollmentDate(LocalDateTime enrollmentDate) {
            this.enrollmentDate = enrollmentDate;
            return this;
        }

        public EnrollmentBuilder changeCount(Integer changeCount) {
            this.changeCount = changeCount;
            return this;
        }

        public EnrollmentBuilder lastUpdatedDate(LocalDateTime lastUpdatedDate) {
            this.lastUpdatedDate = lastUpdatedDate;
            return this;
        }

        public Enrollment build() {
            Enrollment e = new Enrollment();
            e.setStudent(student);
            e.setSection(section);
            e.setEnrollmentDate(enrollmentDate);
            if (changeCount != null)
                e.setChangeCount(changeCount);
            e.setLastUpdatedDate(lastUpdatedDate);
            return e;
        }
    }
}
