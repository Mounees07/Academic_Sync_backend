package com.academic.platform.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private String leaveType; // Medical, Personal, etc.
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalTime fromTime; // planned gate-out time
    private LocalTime toTime; // planned gate-in time

    private String reason;
    private String parentEmail;

    private String parentStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    private String mentorStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    private String parentActionToken; // Token for email link

    private String approvalOtp;
    private LocalDateTime approvalOtpExpiry;

    private LocalDateTime actualExitTime;
    private LocalDateTime actualReturnTime;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public LeaveRequest() {
    }

    public LeaveRequest(Long id, User student, String leaveType, LocalDate fromDate, LocalDate toDate, String reason,
            String parentEmail, String parentStatus, String mentorStatus, String parentActionToken, String approvalOtp,
            LocalDateTime approvalOtpExpiry, LocalDateTime actualExitTime, LocalDateTime actualReturnTime,
            LocalDateTime createdAt) {
        this.id = id;
        this.student = student;
        this.leaveType = leaveType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.reason = reason;
        this.parentEmail = parentEmail;
        this.parentStatus = parentStatus;
        this.mentorStatus = mentorStatus;
        this.parentActionToken = parentActionToken;
        this.approvalOtp = approvalOtp;
        this.approvalOtpExpiry = approvalOtpExpiry;
        this.actualExitTime = actualExitTime;
        this.actualReturnTime = actualReturnTime;
        this.createdAt = createdAt;
    }

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

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public LocalTime getFromTime() {
        return fromTime;
    }

    public void setFromTime(LocalTime fromTime) {
        this.fromTime = fromTime;
    }

    public LocalTime getToTime() {
        return toTime;
    }

    public void setToTime(LocalTime toTime) {
        this.toTime = toTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getParentEmail() {
        return parentEmail;
    }

    public void setParentEmail(String parentEmail) {
        this.parentEmail = parentEmail;
    }

    public String getParentStatus() {
        return parentStatus;
    }

    public void setParentStatus(String parentStatus) {
        this.parentStatus = parentStatus;
    }

    public String getMentorStatus() {
        return mentorStatus;
    }

    public void setMentorStatus(String mentorStatus) {
        this.mentorStatus = mentorStatus;
    }

    public String getParentActionToken() {
        return parentActionToken;
    }

    public void setParentActionToken(String parentActionToken) {
        this.parentActionToken = parentActionToken;
    }

    public String getApprovalOtp() {
        return approvalOtp;
    }

    public void setApprovalOtp(String approvalOtp) {
        this.approvalOtp = approvalOtp;
    }

    public LocalDateTime getApprovalOtpExpiry() {
        return approvalOtpExpiry;
    }

    public void setApprovalOtpExpiry(LocalDateTime approvalOtpExpiry) {
        this.approvalOtpExpiry = approvalOtpExpiry;
    }

    public LocalDateTime getActualExitTime() {
        return actualExitTime;
    }

    public void setActualExitTime(LocalDateTime actualExitTime) {
        this.actualExitTime = actualExitTime;
    }

    public LocalDateTime getActualReturnTime() {
        return actualReturnTime;
    }

    public void setActualReturnTime(LocalDateTime actualReturnTime) {
        this.actualReturnTime = actualReturnTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Builder Pattern
    public static LeaveRequestBuilder builder() {
        return new LeaveRequestBuilder();
    }

    public static class LeaveRequestBuilder {
        private Long id;
        private User student;
        private String leaveType;
        private LocalDate fromDate;
        private LocalDate toDate;
        private String reason;
        private String parentEmail;
        private String parentStatus = "PENDING";
        private String mentorStatus = "PENDING";
        private String parentActionToken;
        private String approvalOtp;
        private LocalDateTime approvalOtpExpiry;
        private LocalDateTime actualExitTime;
        private LocalDateTime actualReturnTime;
        private LocalDateTime createdAt;

        public LeaveRequestBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LeaveRequestBuilder student(User student) {
            this.student = student;
            return this;
        }

        public LeaveRequestBuilder leaveType(String leaveType) {
            this.leaveType = leaveType;
            return this;
        }

        public LeaveRequestBuilder fromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public LeaveRequestBuilder toDate(LocalDate toDate) {
            this.toDate = toDate;
            return this;
        }

        public LeaveRequestBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public LeaveRequestBuilder parentEmail(String parentEmail) {
            this.parentEmail = parentEmail;
            return this;
        }

        public LeaveRequestBuilder parentStatus(String parentStatus) {
            this.parentStatus = parentStatus;
            return this;
        }

        public LeaveRequestBuilder mentorStatus(String mentorStatus) {
            this.mentorStatus = mentorStatus;
            return this;
        }

        public LeaveRequestBuilder parentActionToken(String parentActionToken) {
            this.parentActionToken = parentActionToken;
            return this;
        }

        public LeaveRequestBuilder approvalOtp(String approvalOtp) {
            this.approvalOtp = approvalOtp;
            return this;
        }

        public LeaveRequestBuilder approvalOtpExpiry(LocalDateTime approvalOtpExpiry) {
            this.approvalOtpExpiry = approvalOtpExpiry;
            return this;
        }

        public LeaveRequestBuilder actualExitTime(LocalDateTime actualExitTime) {
            this.actualExitTime = actualExitTime;
            return this;
        }

        public LeaveRequestBuilder actualReturnTime(LocalDateTime actualReturnTime) {
            this.actualReturnTime = actualReturnTime;
            return this;
        }

        public LeaveRequestBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public LeaveRequest build() {
            return new LeaveRequest(id, student, leaveType, fromDate, toDate, reason, parentEmail, parentStatus,
                    mentorStatus, parentActionToken, approvalOtp, approvalOtpExpiry, actualExitTime, actualReturnTime,
                    createdAt);
        }
    }
}
