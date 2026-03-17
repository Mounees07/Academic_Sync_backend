package com.academic.platform.controller;

import com.academic.platform.model.ExamSeating;
import com.academic.platform.service.ExamSeatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam-seating")
public class ExamSeatingController {

    @Autowired
    private ExamSeatingService examSeatingService;

    // ── CREATE via CSV upload ─────────────────────────────────────────────────
    @PostMapping("/allocate")
    public ResponseEntity<?> allocateSeating(
            @RequestParam("examId") Long examId,
            @RequestParam("file") MultipartFile file) {
        try {
            List<ExamSeating> seatings = examSeatingService.processSeatingUpload(examId, file);
            return ResponseEntity.ok(seatings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // ── CREATE via auto-allocation ────────────────────────────────────────────
    @PostMapping("/auto-allocate")
    public ResponseEntity<?> autoAllocate(@RequestParam("examId") Long examId) {
        try {
            List<ExamSeating> seatings = examSeatingService.autoAllocate(examId);
            return ResponseEntity.ok(seatings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────
    @GetMapping("/exam/{examId}")
    public ResponseEntity<List<ExamSeating>> getSeatingByExam(@PathVariable Long examId) {
        return ResponseEntity.ok(examSeatingService.getSeatingByExam(examId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ExamSeating>> getSeatingByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(examSeatingService.getSeatingByStudent(studentId));
    }

    @GetMapping("/student/uid/{uid}")
    public ResponseEntity<List<ExamSeating>> getSeatingByStudentUid(@PathVariable String uid) {
        return ResponseEntity.ok(examSeatingService.getSeatingByStudentUid(uid));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ExamSeating>> getAllAllocations() {
        return ResponseEntity.ok(examSeatingService.getAllAllocations());
    }

    // ── UPDATE single allocation (seat number and/or venue) ───────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSeating(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            String seatNumber = body.containsKey("seatNumber") ? String.valueOf(body.get("seatNumber")) : null;
            Long venueId = body.containsKey("venueId") && body.get("venueId") != null
                    ? Long.parseLong(String.valueOf(body.get("venueId")))
                    : null;
            ExamSeating updated = examSeatingService.updateSeating(id, seatNumber, venueId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // ── DELETE single allocation ───────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSingleSeating(@PathVariable Long id) {
        try {
            examSeatingService.deleteSingleSeating(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Allocation deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // ── DELETE all allocations for a specific exam ─────────────────────────────
    @DeleteMapping("/exam/{examId}")
    public ResponseEntity<?> deleteAllByExam(@PathVariable Long examId) {
        try {
            examSeatingService.deleteAllByExam(examId);
            return ResponseEntity.ok(Collections.singletonMap("message", "All allocations cleared for exam " + examId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // Error wrapper
    public static class ErrorResponse {
        private final String message;
        public ErrorResponse(String message) { this.message = message; }
        public String getMessage() { return message; }
    }
}
