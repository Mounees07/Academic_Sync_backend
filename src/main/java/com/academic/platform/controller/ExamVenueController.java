package com.academic.platform.controller;

import com.academic.platform.model.ExamVenue;
import com.academic.platform.service.ExamVenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-venues")
public class ExamVenueController {

    @Autowired
    private ExamVenueService examVenueService;

    @GetMapping
    public List<ExamVenue> getAllVenues() {
        return examVenueService.getAllVenues();
    }

    @PostMapping
    public ExamVenue createVenue(@RequestBody ExamVenue venue) {
        return examVenueService.createVenue(venue);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExamVenue> updateVenue(@PathVariable Long id, @RequestBody ExamVenue venueDetails) {
        return ResponseEntity.ok(examVenueService.updateVenue(id, venueDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenue(@PathVariable Long id) {
        examVenueService.deleteVenue(id);
        return ResponseEntity.ok().build();
    }
}
