package com.academic.platform.service;

import com.academic.platform.model.LeaveRequest;
import com.academic.platform.model.User;
import com.academic.platform.repository.LeaveRequestRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    // Hardcoded for now, should be from properties or env
    private static final String FRONTEND_URL = "http://10.10.188.128:5173";

    public LeaveRequest applyLeave(String studentUid, LeaveRequest request) {
        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        request.setStudent(student);
        request.setParentStatus("PENDING");
        request.setMentorStatus("PENDING");
        request.setParentActionToken(UUID.randomUUID().toString());

        // Generate OTP immediately so it can be sent in the initial email
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        request.setApprovalOtp(otp);
        request.setApprovalOtpExpiry(java.time.LocalDateTime.now().plusDays(7));

        LeaveRequest saved = leaveRepository.save(request);

        // Send Email to Parent with OTP
        String approvalLink = FRONTEND_URL + "/parent-response/" + saved.getParentActionToken();
        emailService.sendParentApprovalRequest(
                request.getParentEmail(),
                student.getFullName(),
                request.getReason(),
                request.getFromDate().toString(),
                request.getToDate().toString(),
                approvalLink,
                otp);

        return saved;
    }

    public List<LeaveRequest> getStudentLeaves(String studentUid) {
        return leaveRepository.findByStudentFirebaseUid(studentUid);
    }

    public LeaveRequest getLeaveByToken(String token) {
        return leaveRepository.findByParentActionToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));
    }

    public LeaveRequest parentAction(String token, String status) {
        LeaveRequest leave = getLeaveByToken(token);
        if (!leave.getParentStatus().equals("PENDING")) {
            throw new RuntimeException("Request already processed by parent");
        }

        leave.setParentStatus(status); // APPROVED or REJECTED

        if ("REJECTED".equals(status)) {
            leave.setMentorStatus("REJECTED_BY_PARENT");
            emailService.sendStudentLeaveStatus(leave.getStudent().getEmail(), "REJECTED (By Parent)",
                    "Your parent has declined this request.");
        }
        // If Approved, we don't need to generate OTP again, it was already sent.
        // We just save the status.

        return leaveRepository.save(leave);
    }

    public List<LeaveRequest> getPendingLeavesForMentor(String mentorUid) {
        return leaveRepository.findByStudentMentorFirebaseUidAndParentStatus(mentorUid);
    }

    public LeaveRequest mentorAction(Long leaveId, String status, String remarks) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        leave.setMentorStatus(status);
        LeaveRequest saved = leaveRepository.save(leave);

        // Notify Student
        emailService.sendStudentLeaveStatus(leave.getStudent().getEmail(), status, remarks);

        return saved;
    }

    public void deleteLeave(Long leaveId, String studentUid) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (!leave.getStudent().getFirebaseUid().equals(studentUid)) {
            throw new RuntimeException("Unauthorized to delete this leave");
        }

        if ("APPROVED".equals(leave.getMentorStatus())) {
            throw new RuntimeException("Cannot cancel leave after Mentor approval.");
        }

        leaveRepository.delete(leave);
    }

    public LeaveRequest updateLeave(Long leaveId, String studentUid, LeaveRequest updatedData) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (!leave.getStudent().getFirebaseUid().equals(studentUid)) {
            throw new RuntimeException("Unauthorized to update this leave");
        }

        if (!"PENDING".equals(leave.getParentStatus())) {
            throw new RuntimeException("Cannot edit leave: Parent has already processed it.");
        }

        leave.setLeaveType(updatedData.getLeaveType());
        leave.setFromDate(updatedData.getFromDate());
        leave.setToDate(updatedData.getToDate());
        leave.setReason(updatedData.getReason());
        // If parent email changed, maybe resend email? keeping simple for now

        return leaveRepository.save(leave);
    }

    public void generateOtpForApproval(Long leaveId, String mentorUid) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        // This method now resends the OTP to the PARENT, not the mentor.
        // But performs validation that the request is from the mentor.

        User mentor = userRepository.findByFirebaseUid(mentorUid)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));

        User student = leave.getStudent();
        if (student.getStudentDetails().getMentor() == null
                || !student.getStudentDetails().getMentor().getFirebaseUid().equals(mentorUid)) {
            throw new RuntimeException("Unauthorized: You are not the mentor of this student.");
        }

        // Check if OTP already exists and is valid, just resend it? Or generate new?
        // Let's generate new to be safe/fresh.
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        leave.setApprovalOtp(otp);
        leave.setApprovalOtpExpiry(java.time.LocalDateTime.now().plusDays(7));
        leaveRepository.save(leave);

        emailService.sendParentOtpCode(leave.getParentEmail(), leave.getStudent().getFullName(), otp);
    }

    public LeaveRequest verifyOtpAndApprove(Long leaveId, String otp, String mentorUid, String remarks) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getApprovalOtp() == null || leave.getApprovalOtpExpiry() == null) {
            throw new RuntimeException("No OTP generated. Please request OTC first.");
        }

        if (java.time.LocalDateTime.now().isAfter(leave.getApprovalOtpExpiry())) {
            throw new RuntimeException("OTP Expired. Please request a new one.");
        }

        if (!leave.getApprovalOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP.");
        }

        // OTP Verified, Clear it and Approve
        leave.setApprovalOtp(null);
        leave.setApprovalOtpExpiry(null);

        // Implicitly approve parent status as they provided the OTP
        leave.setParentStatus("APPROVED");

        return mentorAction(leaveId, "APPROVED", remarks);
    }

    public void testEmail(String toEmail) {
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        emailService.sendActionOtp(toEmail, otp, "Test Email Verification");
    }

    public LeaveRequest getActiveLeaveForStudent(String rollNumber) {
        List<LeaveRequest> leaves = leaveRepository.findByStudentStudentDetails_RollNumber(rollNumber);
        java.time.LocalDate today = java.time.LocalDate.now();

        // Priority 1: Student has exited but NOT yet returned (most urgent — overdue /
        // in transit)
        java.util.Optional<LeaveRequest> notReturned = leaves.stream()
                .filter(l -> "APPROVED".equals(l.getMentorStatus()))
                .filter(l -> l.getActualExitTime() != null && l.getActualReturnTime() == null)
                .findFirst();
        if (notReturned.isPresent())
            return notReturned.get();

        // Priority 2: Leave is active today and student hasn't exited yet
        java.util.Optional<LeaveRequest> activeToday = leaves.stream()
                .filter(l -> "APPROVED".equals(l.getMentorStatus()))
                .filter(l -> l.getActualReturnTime() == null)
                .filter(l -> !today.isBefore(l.getFromDate()) && !today.isAfter(l.getToDate()))
                .findFirst();
        if (activeToday.isPresent())
            return activeToday.get();

        // Priority 3: Upcoming approved leave (from date in the future, not yet
        // completed)
        java.util.Optional<LeaveRequest> upcoming = leaves.stream()
                .filter(l -> "APPROVED".equals(l.getMentorStatus()))
                .filter(l -> l.getActualReturnTime() == null)
                .filter(l -> today.isBefore(l.getFromDate()))
                .min(java.util.Comparator.comparing(LeaveRequest::getFromDate)); // nearest upcoming
        return upcoming.orElse(null);
    }

    public List<LeaveRequest> getLeavesByDate(java.time.LocalDate date) {
        return leaveRepository.findApprovedLeavesByDate(date);
    }

    public List<LeaveRequest> searchLeavesByRollOrName(String query) {
        if (query == null || query.trim().isEmpty())
            return java.util.Collections.emptyList();
        String q = query.trim();

        // Run both queries separately to avoid complex JPQL join issues
        List<LeaveRequest> byRoll = leaveRepository.findApprovedLeavesByRoll(q);
        List<LeaveRequest> byName = leaveRepository.findApprovedLeavesByName(q);

        // Merge + deduplicate by leave ID, preserve order (most recent first)
        java.util.LinkedHashMap<Long, LeaveRequest> merged = new java.util.LinkedHashMap<>();
        for (LeaveRequest l : byRoll)
            merged.put(l.getId(), l);
        for (LeaveRequest l : byName)
            merged.putIfAbsent(l.getId(), l);

        LocalDate today = LocalDate.now();
        LocalDateTime cutoff24h = LocalDateTime.now().minusHours(24);

        // Gate-security visibility rules:
        // 1. PENDING (never exited): hide if toDate has already passed (leave expired)
        // 2. OUT (exited, not returned): always visible — student is outside
        // 3. RETURNED: hide after 24 hours of actual return time
        return merged.values().stream().filter(l -> {
            boolean hasExited = l.getActualExitTime() != null;
            boolean hasReturned = l.getActualReturnTime() != null;

            if (!hasExited && !hasReturned) {
                // PENDING: only show if leave period hasn't ended yet
                return !l.getToDate().isBefore(today);
            }
            if (hasExited && !hasReturned) {
                // OUT: always show
                return true;
            }
            // RETURNED: show only within 24 hours of return
            return l.getActualReturnTime().isAfter(cutoff24h);
        }).collect(java.util.stream.Collectors.toList());
    }

    public LeaveRequest updateSecurityExitEntry(Long leaveId, String action) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (!"APPROVED".equals(leave.getMentorStatus())) {
            throw new RuntimeException("Leave is not approved by mentor.");
        }

        if ("EXIT".equalsIgnoreCase(action)) {
            if (leave.getActualExitTime() != null) {
                throw new RuntimeException("Student has already exited.");
            }
            leave.setActualExitTime(java.time.LocalDateTime.now());
        } else if ("RETURN".equalsIgnoreCase(action)) {
            if (leave.getActualExitTime() == null) {
                throw new RuntimeException("Cannot return without exiting first.");
            }
            if (leave.getActualReturnTime() != null) {
                throw new RuntimeException("Student has already returned.");
            }
            leave.setActualReturnTime(java.time.LocalDateTime.now());
        } else {
            throw new RuntimeException("Invalid security action: " + action);
        }

        return leaveRepository.save(leave);
    }
}
