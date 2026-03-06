package com.academic.platform.controller;

import com.academic.platform.dto.DepartmentDashboardDTO;
import com.academic.platform.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private com.academic.platform.service.SystemSettingService systemSettingService;

    @GetMapping("/dashboard/{department}")
    public ResponseEntity<DepartmentDashboardDTO> getDashboardStats(@PathVariable String department) {
        return ResponseEntity.ok(departmentService.getDashboardStats(department));
    }

    @GetMapping("/analytics/{department}")
    public ResponseEntity<?> getAnalytics(
            @PathVariable String department) {
        if ("false".equalsIgnoreCase(systemSettingService.getSetting("feature.analytics.enabled"))) {
            return ResponseEntity.status(403).body("Analytics module disabled.");
        }
        return ResponseEntity.ok(departmentService.getAnalytics(department));
    }
}
