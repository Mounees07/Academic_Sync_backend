package com.academic.platform.controller;

import com.academic.platform.model.Visitor;
import com.academic.platform.service.VisitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/gate/visitors")
public class VisitorController {

    @Autowired
    private VisitorService visitorService;

    @PostMapping("/check-in")
    public ResponseEntity<Visitor> checkIn(@RequestBody Visitor visitor) {
        return ResponseEntity.ok(visitorService.checkIn(visitor));
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<Visitor> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(visitorService.checkOut(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Visitor> updateVisitor(@PathVariable Long id, @RequestBody Visitor visitor) {
        return ResponseEntity.ok(visitorService.updateVisitor(id, visitor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVisitor(@PathVariable Long id) {
        visitorService.deleteVisitor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Visitor>> getAllVisitors() {
        return ResponseEntity.ok(visitorService.getAllVisitors());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Visitor>> getActiveVisitors() {
        return ResponseEntity.ok(visitorService.getActiveVisitors());
    }
}
