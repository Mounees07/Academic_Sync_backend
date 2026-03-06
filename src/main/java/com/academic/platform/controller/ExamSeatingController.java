package com.academic.platform.controller;

import com.academic.platform.model.ExamSeating;
import com.academic.platform.service.ExamSeatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/exam-seating")
public class ExamSeatingController {

    @Autowired
    private ExamSeatingService examSeatingService;

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

    /**
     * Smart auto-allocation: alternates students by dept + section.
     * No CSV needed — the system picks students from the database automatically.
     */
    @PostMapping("/auto-allocate")
    public ResponseEntity<?> autoAllocate(@RequestParam("examId") Long examId) {
        try {
            List<ExamSeating> seatings = examSeatingService.autoAllocate(examId);
            return ResponseEntity.ok(seatings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // Simple wrapper for error message
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

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
}
