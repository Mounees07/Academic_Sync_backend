package com.academic.platform.controller;

import com.academic.platform.service.AcademicHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/academic-health")

public class AcademicHealthController {

    @Autowired
    private AcademicHealthService service;

    @GetMapping("/student/{uid}")
    public ResponseEntity<Map<String, Object>> getHealthCard(@PathVariable String uid) {
        return ResponseEntity.ok(service.getHealthCard(uid));
    }
}

