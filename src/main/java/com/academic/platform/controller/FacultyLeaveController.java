package com.academic.platform.controller;

import com.academic.platform.model.FacultyLeaveRequest;
import com.academic.platform.service.FacultyLeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the Faculty / HOD leave management feature.
 *
 * Workflow:
 *  1. Faculty/Mentor → POST /api/faculty-leaves/apply?uid=...  (creates leave, HOD notified)
 *  2. HOD           → POST /api/faculty-leaves/{id}/hod-action?hodUid=...  {action, remarks}
 *  3. Admin         → POST /api/faculty-leaves/{id}/admin-action?adminUid=...  {action, remarks}  (FINAL)
 *  4. Faculty       → GET  /api/faculty-leaves/my?uid=...  (view own leaves)
 *
 * HOD applying leave:
 *  1. HOD  → POST /api/faculty-leaves/apply?uid=...  (hodStatus set to SKIPPED, goes directly to admin)
 *  2. Admin → POST /api/faculty-leaves/{id}/admin-action  (FINAL)
 */
@RestController
@RequestMapping("/api/faculty-leaves")
public class FacultyLeaveController {

    @Autowired
    private FacultyLeaveService service;

    /** Faculty / Mentor / HOD apply for leave */
    @PostMapping("/apply")
    public ResponseEntity<?> apply(
            @RequestParam String uid,
            @RequestBody FacultyLeaveRequest request) {
        return ResponseEntity.ok(service.applyLeave(uid, request));
    }

    /** Applicant views their own leaves */
    @GetMapping("/my")
    public ResponseEntity<?> myLeaves(@RequestParam String uid) {
        return ResponseEntity.ok(service.getMyLeaves(uid));
    }

    /** HOD: view pending faculty leaves for their department */
    @GetMapping("/hod/pending")
    public ResponseEntity<?> pendingForHod(@RequestParam String hodUid) {
        return ResponseEntity.ok(service.getPendingForHod(hodUid));
    }

    /** HOD: approve or reject a faculty leave */
    @PostMapping("/{id}/hod-action")
    public ResponseEntity<?> hodAction(
            @PathVariable Long id,
            @RequestParam String hodUid,
            @RequestBody Map<String, String> payload) {
        String action = payload.get("action");   // APPROVED | REJECTED
        String remarks = payload.getOrDefault("remarks", "");
        return ResponseEntity.ok(service.hodAction(id, hodUid, action, remarks));
    }

    /** Admin: view all leaves pending final approval */
    @GetMapping("/admin/pending")
    public ResponseEntity<?> pendingForAdmin() {
        return ResponseEntity.ok(service.getPendingForAdmin());
    }

    /** Admin: view all faculty leaves (full history) */
    @GetMapping("/admin/all")
    public ResponseEntity<?> allLeaves() {
        return ResponseEntity.ok(service.getAllLeaves());
    }

    /** Admin: give final approval / rejection */
    @PostMapping("/{id}/admin-action")
    public ResponseEntity<?> adminAction(
            @PathVariable Long id,
            @RequestParam String adminUid,
            @RequestBody Map<String, String> payload) {
        String action = payload.get("action");   // APPROVED | REJECTED
        String remarks = payload.getOrDefault("remarks", "");
        return ResponseEntity.ok(service.adminAction(id, adminUid, action, remarks));
    }

    /** Cancel leave (applicant only, if not fully approved) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(
            @PathVariable Long id,
            @RequestParam String uid) {
        service.cancelLeave(id, uid);
        return ResponseEntity.noContent().build();
    }

    /** Dashboard count helpers */
    @GetMapping("/admin/pending-count")
    public ResponseEntity<?> adminPendingCount() {
        return ResponseEntity.ok(Map.of("count", service.pendingCountForAdmin()));
    }

    @GetMapping("/hod/pending-count")
    public ResponseEntity<?> hodPendingCount(@RequestParam String department) {
        return ResponseEntity.ok(Map.of("count", service.pendingCountForHod(department)));
    }
}
