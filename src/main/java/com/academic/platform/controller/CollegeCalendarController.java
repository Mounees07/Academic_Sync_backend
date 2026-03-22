package com.academic.platform.controller;

import com.academic.platform.model.CollegeCalendarEvent;
import com.academic.platform.service.CollegeCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/college-calendar")
public class CollegeCalendarController {

    @Autowired
    private CollegeCalendarService calendarService;

    @GetMapping("/events")
    public ResponseEntity<List<CollegeCalendarEvent>> getEvents(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(calendarService.getEvents(month, startDate, endDate));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<CollegeCalendarEvent>> getUpcomingEvents(
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ResponseEntity.ok(calendarService.getUpcomingEvents(limit));
    }

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestBody CollegeCalendarEvent event) {
        try {
            return ResponseEntity.ok(calendarService.createEvent(event));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody CollegeCalendarEvent event) {
        try {
            return ResponseEntity.ok(calendarService.updateEvent(id, event));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        try {
            calendarService.deleteEvent(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
