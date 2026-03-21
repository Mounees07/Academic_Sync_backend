package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "course_attendance_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAttendanceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private AcademicSchedule schedule;

    private String otp;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private boolean active;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @JsonProperty("sessionLabel")
    public String getSessionLabel() {
        if (schedule == null) {
            return otp != null && "MANUAL".equalsIgnoreCase(otp) ? "Manual Session" : "OTP Session";
        }

        String slot = schedule.getSession() != null && !schedule.getSession().isBlank()
                ? schedule.getSession().trim()
                : "Scheduled";
        String start = schedule.getStartTime() != null ? schedule.getStartTime().toString() : "--:--";
        String end = schedule.getEndTime() != null ? schedule.getEndTime().toString() : "--:--";
        return slot + " (" + start + " - " + end + ")";
    }

    @JsonProperty("venue")
    public String getVenue() {
        return schedule != null ? schedule.getLocation() : null;
    }

    @JsonProperty("courseDate")
    public String getCourseDate() {
        return schedule != null && schedule.getDate() != null ? schedule.getDate().toString() : null;
    }
}
