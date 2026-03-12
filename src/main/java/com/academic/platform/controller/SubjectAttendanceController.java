package com.academic.platform.controller;

import com.academic.platform.model.SubjectAttendance;
import com.academic.platform.service.SubjectAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subject-attendance")
public class SubjectAttendanceController {

    @Autowired
    private SubjectAttendanceService service;

    /** GET all subject attendance for a student */
    @GetMapping("/student/{uid}")
    public ResponseEntity<List<Map<String, Object>>> getStudentAttendance(@PathVariable String uid) {
        return ResponseEntity.ok(service.getStudentSubjectAttendance(uid));
    }

    /** GET monthly breakdown */
    @GetMapping("/student/{uid}/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthly(
            @PathVariable String uid,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(service.getMonthlyAttendance(uid, month, year));
    }

    /** GET analytics (trend, monthly chart data) */
    @GetMapping("/analytics/{uid}")
    public ResponseEntity<Map<String, Object>> getAnalytics(@PathVariable String uid) {
        return ResponseEntity.ok(service.getAttendanceAnalytics(uid));
    }

    /** GET attendance prediction */
    @GetMapping("/prediction/{uid}")
    public ResponseEntity<Map<String, Object>> getPrediction(@PathVariable String uid) {
        return ResponseEntity.ok(service.getAttendancePrediction(uid));
    }

    /** POST/PUT upsert a subject attendance record (teacher/admin only) */
    @PostMapping("/upsert")
    public ResponseEntity<SubjectAttendance> upsert(@RequestBody Map<String, Object> body) {
        String studentUid = (String) body.get("studentUid");
        Long sectionId = Long.parseLong(body.get("sectionId").toString());
        String subjectName = (String) body.get("subjectName");
        String subjectCode = (String) body.getOrDefault("subjectCode", null);
        int month = Integer.parseInt(body.get("month").toString());
        int year = Integer.parseInt(body.get("year").toString());
        int totalClasses = Integer.parseInt(body.get("totalClasses").toString());
        int attendedClasses = Integer.parseInt(body.get("attendedClasses").toString());

        return ResponseEntity.ok(service.upsertAttendance(
                studentUid, sectionId, subjectName, subjectCode, month, year, totalClasses, attendedClasses));
    }
}

