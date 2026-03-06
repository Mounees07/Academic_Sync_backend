package com.academic.platform.controller;

import com.academic.platform.model.Attendance;
import com.academic.platform.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = { "http://localhost:5173", "http://10.10.188.128:5173" }, allowCredentials = "true")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@RequestParam String studentUid) {
        try {
            return ResponseEntity.ok(attendanceService.markAttendance(studentUid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/check-today/{studentUid}")
    public ResponseEntity<Boolean> checkToday(@PathVariable String studentUid) {
        return ResponseEntity.ok(attendanceService.isAttendanceMarkedToday(studentUid));
    }

    @GetMapping("/student/{studentUid}")
    public ResponseEntity<List<Attendance>> getStudentHistory(@PathVariable String studentUid) {
        return ResponseEntity.ok(attendanceService.getStudentAttendance(studentUid));
    }

    @GetMapping("/mentor/{mentorUid}")
    public ResponseEntity<List<Attendance>> getMenteesAttendance(
            @PathVariable String mentorUid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getMenteesAttendance(mentorUid, date));
    }

    @GetMapping("/stats/{studentUid}")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String studentUid) {
        return ResponseEntity.ok(attendanceService.getStudentStats(studentUid));
    }
}
