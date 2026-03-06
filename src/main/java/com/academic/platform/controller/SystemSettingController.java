package com.academic.platform.controller;

import com.academic.platform.service.AuditLogService;
import com.academic.platform.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class SystemSettingController {

    @Autowired
    private SystemSettingService settingService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(settingService.getAllSettings());
    }

    @Autowired
    private com.academic.platform.service.UserService userService;

    @Autowired
    private com.academic.platform.utils.SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSettings(
            @RequestBody Map<String, String> updates,
            HttpServletRequest request) {

        String uid = securityUtils.getCurrentUserUid();
        String email = "unknown";

        // Fetch full user details for logging
        if (uid != null) {
            var userOpt = userService.getUserByFirebaseUid(uid);
            if (userOpt.isPresent()) {
                email = userOpt.get().getEmail();
            }
        }

        // Log the change
        String ip = request.getRemoteAddr();
        settingService.updateSettings(updates, uid, email, ip);

        return ResponseEntity.ok(Map.of("message", "System settings updated successfully"));
    }

    @GetMapping("/public/features")
    public ResponseEntity<?> getPublicFeatures() {
        return ResponseEntity.ok(settingService.getPublicSettings());
    }
}
