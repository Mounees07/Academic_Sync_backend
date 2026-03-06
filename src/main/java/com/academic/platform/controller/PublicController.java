package com.academic.platform.controller;

import com.academic.platform.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private SystemSettingService settingService;

    @GetMapping("/settings")
    public ResponseEntity<?> getPublicSettings() {
        return ResponseEntity.ok(settingService.getPublicSettings());
    }
}
