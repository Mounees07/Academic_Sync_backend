package com.academic.platform.controller;

import com.academic.platform.model.AcademicSchedule;
import com.academic.platform.service.AcademicScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = { "http://localhost:5173", "http://10.10.188.128:5173" }, allowCredentials = "true")
public class AcademicScheduleController {

    @Autowired
    private AcademicScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<List<AcademicSchedule>> getSchedules() {
        return ResponseEntity.ok(scheduleService.getAllUpcomingSchedules());
    }

    @GetMapping("/search")
    public ResponseEntity<List<AcademicSchedule>> searchSchedules(
            @RequestParam("date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @RequestParam(value = "subjectName", required = false) String subjectName) {
        return ResponseEntity.ok(scheduleService.searchSchedules(date, subjectName));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSchedule(
            @RequestParam("file") MultipartFile file,
            @RequestParam("hodUid") String hodUid) {
        try {
            return ResponseEntity.ok(scheduleService.processBulkUpload(file, hodUid));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/recent-uploads")
    public ResponseEntity<List<AcademicSchedule>> getRecentUploads() {
        return ResponseEntity.ok(scheduleService.getRecentSchedules());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSchedule(
            @PathVariable Long id,
            @RequestBody AcademicSchedule schedule,
            @RequestParam String uid) {
        try {
            return ResponseEntity.ok(scheduleService.updateSchedule(id, schedule, uid));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSchedule(
            @PathVariable Long id,
            @RequestParam String uid) {
        try {
            scheduleService.deleteSchedule(id, uid);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
