package com.academic.platform.controller;

import com.academic.platform.model.CourseAttendance;
import com.academic.platform.model.CourseAttendanceSession;
import com.academic.platform.service.CourseAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/course-attendance")
@CrossOrigin(origins = "http://localhost:3000")
public class CourseAttendanceController {

    @Autowired
    private CourseAttendanceService attendanceService;

    @PostMapping("/sessions/generate/{sectionId}")
    public ResponseEntity<CourseAttendanceSession> generateOtp(
            @PathVariable Long sectionId,
            @RequestParam String facultyUid) {
        try {
            return ResponseEntity.ok(attendanceService.generateOtp(sectionId, facultyUid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/sessions/{sessionId}/deactivate")
    public ResponseEntity<CourseAttendanceSession> deactivateSession(
            @PathVariable Long sessionId,
            @RequestParam String facultyUid) {
        try {
            return ResponseEntity.ok(attendanceService.deactivateSession(sessionId, facultyUid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/sessions/section/{sectionId}/active")
    public ResponseEntity<CourseAttendanceSession> getActiveSession(@PathVariable Long sectionId) {
        return ResponseEntity.ok(attendanceService.getActiveSession(sectionId));
    }

    @GetMapping("/sessions/section/{sectionId}")
    public ResponseEntity<List<CourseAttendanceSession>> getSectionSessions(@PathVariable Long sectionId) {
        return ResponseEntity.ok(attendanceService.getSectionSessions(sectionId));
    }

    @GetMapping("/sessions/section/{sectionId}/by-date")
    public ResponseEntity<List<CourseAttendanceSession>> getSessionsByDate(
            @PathVariable Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getSessionsByDate(sectionId, date));
    }

    @GetMapping("/sessions/{sessionId}/attendances")
    public ResponseEntity<List<CourseAttendance>> getSessionAttendances(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.getSessionAttendances(sessionId));
    }

    @GetMapping("/public/debug-attendance/{sessionId}")
    public ResponseEntity<String> debugAttendance(@PathVariable Long sessionId) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            List<CourseAttendance> list = attendanceService.getSessionAttendances(sessionId);
            return ResponseEntity.ok(mapper.writeValueAsString(list));
        } catch (Exception e) {
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }

    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(
            @RequestParam String otp,
            @RequestParam String studentUid) {
        try {
            CourseAttendance attendance = attendanceService.markAttendance(otp, studentUid);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Return error message string
        }
    }

    @GetMapping("/section/{sectionId}/student/{studentId}")
    public ResponseEntity<List<CourseAttendance>> getStudentAttendance(
            @PathVariable Long sectionId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(attendanceService.getStudentAttendanceForSection(sectionId, studentId));
    }

    @PostMapping("/sessions/bulk/{sectionId}")
    public ResponseEntity<CourseAttendanceSession> saveBulkAttendance(
            @PathVariable Long sectionId,
            @RequestParam String facultyUid,
            @RequestBody List<java.util.Map<String, String>> requestData) {
        try {
            return ResponseEntity.ok(attendanceService.saveBulkAttendance(sectionId, facultyUid, requestData));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
