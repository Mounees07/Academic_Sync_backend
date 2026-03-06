package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_attendances", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "session_id", "student_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private CourseAttendanceSession session;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private LocalDateTime markedAt;

    private String status;

    @PrePersist
    protected void onCreate() {
        if (markedAt == null)
            markedAt = LocalDateTime.now();
        if (status == null)
            status = "PRESENT";
    }

    @com.fasterxml.jackson.annotation.JsonProperty("studentName")
    public String getStudentName() {
        if (student != null)
            return student.getFullName();
        return null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("studentRollNumber")
    public String getStudentRollNumber() {
        if (student != null && student.getStudentDetails() != null)
            return student.getStudentDetails().getRollNumber();
        return null;
    }
}
