package com.academic.platform.controller;

import com.academic.platform.model.LeaveRequest;
import com.academic.platform.service.LeaveService;
import com.academic.platform.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private SystemSettingService systemSettingService;

    private boolean isLeaveFeatureEnabled() {
        return Boolean.parseBoolean(systemSettingService.getSetting("feature.leave.enabled"));
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(
            @RequestParam String studentUid,
            @RequestBody LeaveRequest request) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module is currently disabled by administrator.");
        }
        return ResponseEntity.ok(leaveService.applyLeave(studentUid, request));
    }

    @GetMapping("/pending/{mentorUid}")
    public ResponseEntity<?> getPendingLeaves(@PathVariable String mentorUid) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        return ResponseEntity.ok(leaveService.getPendingLeavesForMentor(mentorUid));
    }

    @GetMapping("/student/{studentUid}")
    public ResponseEntity<?> getStudentLeaves(@PathVariable String studentUid) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        return ResponseEntity.ok(leaveService.getStudentLeaves(studentUid));
    }

    // Public endpoint for parent to view details via token
    @GetMapping("/parent-view/{token}")
    public ResponseEntity<?> getLeaveByToken(@PathVariable String token) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        return ResponseEntity.ok(leaveService.getLeaveByToken(token));
    }

    // Public endpoint for parent action
    @PostMapping("/parent-action/{token}")
    public ResponseEntity<?> parentAction(
            @PathVariable String token,
            @RequestParam String status) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        leaveService.parentAction(token, status);
        return ResponseEntity.ok("Parent action recorded successfully");
    }

    @PostMapping("/mentor-action/{leaveId}")
    public ResponseEntity<?> mentorAction(
            @PathVariable Long leaveId,
            @RequestBody Map<String, String> payload) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        String status = payload.get("status");
        String remarks = payload.get("remarks");
        return ResponseEntity.ok(leaveService.mentorAction(leaveId, status, remarks));
    }

    @DeleteMapping("/{leaveId}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long leaveId, @RequestParam String studentUid) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        leaveService.deleteLeave(leaveId, studentUid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{leaveId}")
    public ResponseEntity<?> updateLeave(
            @PathVariable Long leaveId,
            @RequestParam String studentUid,
            @RequestBody LeaveRequest request) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        return ResponseEntity.ok(leaveService.updateLeave(leaveId, studentUid, request));
    }

    @PostMapping("/{leaveId}/generate-otp")
    public ResponseEntity<?> generateOtp(
            @PathVariable Long leaveId,
            @RequestParam String mentorUid) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        leaveService.generateOtpForApproval(leaveId, mentorUid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{leaveId}/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @PathVariable Long leaveId,
            @RequestParam String mentorUid,
            @RequestBody Map<String, String> payload) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        String otp = payload.get("otp");
        String remarks = payload.get("remarks");
        return ResponseEntity.ok(leaveService.verifyOtpAndApprove(leaveId, otp, mentorUid, remarks));
    }

    @PostMapping("/test-email")
    public ResponseEntity<String> testEmail(@RequestParam String email) {
        leaveService.testEmail(email);
        return ResponseEntity.ok("Test email sent to " + email);
    }

    @GetMapping("/security/by-date")
    public ResponseEntity<?> getLeavesByDate(@RequestParam String date) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        return ResponseEntity.ok(leaveService.getLeavesByDate(localDate));
    }

    @GetMapping("/security/search")
    public ResponseEntity<?> searchLeaves(@RequestParam String query) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        return ResponseEntity.ok(leaveService.searchLeavesByRollOrName(query));
    }

    @GetMapping("/security/active/{rollNumber}")
    public ResponseEntity<?> getActiveLeaveForStudent(@PathVariable String rollNumber) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        LeaveRequest leave = leaveService.getActiveLeaveForStudent(rollNumber);
        if (leave == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(leave);
    }

    @PostMapping("/security/{leaveId}/action")
    public ResponseEntity<?> securityAction(
            @PathVariable Long leaveId,
            @RequestParam String action) {
        if (!isLeaveFeatureEnabled()) {
            return ResponseEntity.status(403).body("Leave module disabled.");
        }
        return ResponseEntity.ok(leaveService.updateSecurityExitEntry(leaveId, action));
    }
}
