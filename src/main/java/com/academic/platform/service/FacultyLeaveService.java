package com.academic.platform.service;

import com.academic.platform.model.FacultyLeaveRequest;
import com.academic.platform.model.Role;
import com.academic.platform.model.User;
import com.academic.platform.repository.FacultyLeaveRequestRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class FacultyLeaveService {

    @Autowired
    private FacultyLeaveRequestRepository repo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    // ─────────────────────────────────────────────────────────────
    // APPLY LEAVE (Faculty / Mentor / HOD)
    // ─────────────────────────────────────────────────────────────
    public FacultyLeaveRequest applyLeave(String applicantUid, FacultyLeaveRequest request) {
        User applicant = userRepository.findByFirebaseUid(applicantUid)
                .orElseThrow(() -> new RuntimeException("User not found: " + applicantUid));

        request.setApplicant(applicant);

        Role role = applicant.getRole();
        if (role == Role.HOD) {
            request.setApplicantRole("HOD");
            request.setHodStatus("SKIPPED");
            request.setAdminStatus("PENDING");
        } else {
            request.setApplicantRole("FACULTY");
            request.setHodStatus("PENDING");
            request.setAdminStatus("PENDING");
        }

        FacultyLeaveRequest saved = repo.save(request);

        if ("HOD".equals(request.getApplicantRole())) {
            notifyAdmins("HOD Leave Application",
                    applicant.getFullName() + " (HOD) has applied for leave from "
                            + request.getFromDate() + " to " + request.getToDate() + ".",
                    "/admin/faculty-leaves");
        } else {
            notifyHodOfDepartment(applicant,
                    "Faculty Leave Application",
                    applicant.getFullName() + " has applied for leave from "
                            + request.getFromDate() + " to " + request.getToDate() + ".",
                    "/hod/faculty-leaves");
        }

        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    // MY LEAVES
    // ─────────────────────────────────────────────────────────────
    public List<FacultyLeaveRequest> getMyLeaves(String applicantUid) {
        return repo.findByApplicantFirebaseUidOrderByCreatedAtDesc(applicantUid);
    }

    // ─────────────────────────────────────────────────────────────
    // HOD: GET PENDING LEAVES
    // ─────────────────────────────────────────────────────────────
    public List<FacultyLeaveRequest> getPendingForHod(String hodUid) {
        User hod = userRepository.findByFirebaseUid(hodUid)
                .orElseThrow(() -> new RuntimeException("HOD not found: " + hodUid));
        String department = hod.getStudentDetails() != null ? hod.getStudentDetails().getDepartment() : null;
        if (department == null || department.isBlank()) {
            return java.util.Collections.emptyList();
        }
        return repo.findPendingForHod(department);
    }

    // ─────────────────────────────────────────────────────────────
    // HOD: APPROVE / REJECT
    // ─────────────────────────────────────────────────────────────
    public FacultyLeaveRequest hodAction(Long leaveId, String hodUid, String action, String remarks) {
        FacultyLeaveRequest leave = repo.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found: " + leaveId));

        if (!"PENDING".equals(leave.getHodStatus())) {
            throw new RuntimeException("This leave has already been acted on by HOD.");
        }
        if (!"FACULTY".equals(leave.getApplicantRole())) {
            throw new RuntimeException("HOD leaves are handled by Admin.");
        }

        User hod = userRepository.findByFirebaseUid(hodUid)
                .orElseThrow(() -> new RuntimeException("HOD not found: " + hodUid));

        leave.setHod(hod);
        leave.setHodStatus(action);
        leave.setHodRemarks(remarks);
        leave.setHodAt(LocalDateTime.now());

        if ("APPROVED".equals(action)) {
            notifyAdmins("Faculty Leave – HOD Approved",
                    leave.getApplicant().getFullName() + "'s leave approved by HOD. Awaiting final admin approval.",
                    "/admin/faculty-leaves");
        } else {
            notifyApplicant(leave, "REJECTED",
                    "HOD has rejected your leave application. Remarks: " + (remarks != null ? remarks : "—"));
        }

        return repo.save(leave);
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN: GET PENDING LEAVES
    // ─────────────────────────────────────────────────────────────
    public List<FacultyLeaveRequest> getPendingForAdmin() {
        return repo.findPendingForAdmin();
    }

    public List<FacultyLeaveRequest> getAllLeaves() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN: FINAL APPROVE / REJECT
    // ─────────────────────────────────────────────────────────────
    public FacultyLeaveRequest adminAction(Long leaveId, String adminUid, String action, String remarks) {
        FacultyLeaveRequest leave = repo.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found: " + leaveId));

        if (!"PENDING".equals(leave.getAdminStatus())) {
            throw new RuntimeException("Admin has already acted on this leave.");
        }
        if (!"APPROVED".equals(leave.getHodStatus()) && !"SKIPPED".equals(leave.getHodStatus())) {
            throw new RuntimeException("HOD has not yet approved this leave.");
        }

        leave.setAdminStatus(action);
        leave.setAdminRemarks(remarks);
        leave.setAdminAt(LocalDateTime.now());

        FacultyLeaveRequest saved = repo.save(leave);

        if ("APPROVED".equals(action)) {
            notifyApplicant(leave, "APPROVED",
                    "Your leave from " + leave.getFromDate() + " to " + leave.getToDate() + " has been FINALLY APPROVED by Admin.");
            emailService.sendFacultyLeaveApproved(
                    leave.getApplicant().getEmail(),
                    leave.getApplicant().getFullName(),
                    leave.getFromDate().toString(),
                    leave.getToDate().toString(),
                    remarks);
        } else {
            notifyApplicant(leave, "REJECTED",
                    "Your leave was rejected by Admin. Remarks: " + (remarks != null ? remarks : "—"));
            emailService.sendFacultyLeaveRejected(
                    leave.getApplicant().getEmail(),
                    leave.getApplicant().getFullName(),
                    leave.getFromDate().toString(),
                    leave.getToDate().toString(),
                    remarks);
        }

        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    // CANCEL
    // ─────────────────────────────────────────────────────────────
    public void cancelLeave(Long leaveId, String applicantUid) {
        FacultyLeaveRequest leave = repo.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found: " + leaveId));
        if (!leave.getApplicant().getFirebaseUid().equals(applicantUid)) {
            throw new RuntimeException("Unauthorized");
        }
        if ("APPROVED".equals(leave.getAdminStatus())) {
            throw new RuntimeException("Cannot cancel a fully approved leave.");
        }
        repo.delete(leave);
    }

    public long pendingCountForAdmin() {
        return repo.countPendingForAdmin();
    }

    public long pendingCountForHod(String department) {
        return repo.countPendingForHod(department);
    }

    private void notifyApplicant(FacultyLeaveRequest leave, String status, String message) {
        notificationService.createNotification(
                leave.getApplicant().getFirebaseUid(),
                "LEAVE_" + status,
                "APPROVED".equals(status) ? "✅ Leave Approved" : "❌ Leave Rejected",
                message,
                "/faculty-leaves");
    }

    private void notifyAdmins(String title, String message, String url) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            notificationService.createNotification(admin.getFirebaseUid(), "LEAVE_PENDING", title, message, url);
        }
    }

    private void notifyHodOfDepartment(User applicant, String title, String message, String url) {
        String dept = applicant.getStudentDetails() != null ? applicant.getStudentDetails().getDepartment() : null;
        if (dept == null) return;
        List<User> hods = userRepository.findByRole(Role.HOD);
        for (User hod : hods) {
            String hodDept = hod.getStudentDetails() != null ? hod.getStudentDetails().getDepartment() : null;
            if (dept.equalsIgnoreCase(hodDept)) {
                notificationService.createNotification(hod.getFirebaseUid(), "LEAVE_PENDING", title, message, url);
            }
        }
    }
}
