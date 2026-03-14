package com.academic.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Faculty Leave Request – supports a two-stage approval workflow:
 *   1. HOD approves (or Admin directly, if applicant IS the HOD)
 *   2. Admin gives final approval → notifies the faculty
 */
@Entity
@Table(name = "faculty_leave_requests")
public class FacultyLeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The faculty/mentor/HOD who applied
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "applicant_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User applicant;

    private String leaveType;        // e.g. Medical, Personal, Casual, Earned
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;

    /**
     * WHO is handling this leave.
     *   FACULTY  – applicant is MENTOR/TEACHER → HOD then ADMIN
     *   HOD      – applicant is HOD            → ADMIN only
     */
    private String applicantRole;   // "FACULTY" | "HOD"

    // Stage 1: HOD approval (skipped when applicant is HOD)
    private String hodStatus = "PENDING";   // PENDING, APPROVED, REJECTED, SKIPPED
    private String hodRemarks;
    private LocalDateTime hodAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hod_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "studentDetails"})
    private User hod;   // which HOD processed this

    // Stage 2: Admin approval (final)
    private String adminStatus = "PENDING";  // PENDING, APPROVED, REJECTED
    private String adminRemarks;
    private LocalDateTime adminAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }


    // ── Constructors ─────────────────────────────────────────────────────────
    public FacultyLeaveRequest() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getApplicant() { return applicant; }
    public void setApplicant(User applicant) { this.applicant = applicant; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getApplicantRole() { return applicantRole; }
    public void setApplicantRole(String applicantRole) { this.applicantRole = applicantRole; }

    public String getHodStatus() { return hodStatus; }
    public void setHodStatus(String hodStatus) { this.hodStatus = hodStatus; }

    public String getHodRemarks() { return hodRemarks; }
    public void setHodRemarks(String hodRemarks) { this.hodRemarks = hodRemarks; }

    public LocalDateTime getHodAt() { return hodAt; }
    public void setHodAt(LocalDateTime hodAt) { this.hodAt = hodAt; }

    public User getHod() { return hod; }
    public void setHod(User hod) { this.hod = hod; }

    public String getAdminStatus() { return adminStatus; }
    public void setAdminStatus(String adminStatus) { this.adminStatus = adminStatus; }

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }

    public LocalDateTime getAdminAt() { return adminAt; }
    public void setAdminAt(LocalDateTime adminAt) { this.adminAt = adminAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * Convenience: overall status for display.
     *   PENDING  – awaiting any action
     *   HOD_APPROVED – HOD approved, waiting for admin
     *   APPROVED – admin gave final approval
     *   REJECTED – rejected at any stage
     */
    public String getOverallStatus() {
        if ("REJECTED".equals(hodStatus) || "REJECTED".equals(adminStatus)) return "REJECTED";
        if ("APPROVED".equals(adminStatus)) return "APPROVED";
        if ("APPROVED".equals(hodStatus) || "SKIPPED".equals(hodStatus)) return "HOD_APPROVED";
        return "PENDING";
    }
}
