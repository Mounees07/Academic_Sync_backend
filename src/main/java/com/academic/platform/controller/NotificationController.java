package com.academic.platform.controller;

import com.academic.platform.model.Notification;
import com.academic.platform.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @GetMapping("/user/{uid}")
    public ResponseEntity<List<Notification>> getAll(@PathVariable String uid) {
        return ResponseEntity.ok(service.getAll(uid));
    }

    @GetMapping("/user/{uid}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String uid) {
        return ResponseEntity.ok(Map.of("count", service.getUnreadCount(uid)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(service.markRead(id));
    }

    @PutMapping("/user/{uid}/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(@PathVariable String uid) {
        int updated = service.markAllRead(uid);
        return ResponseEntity.ok(Map.of("marked", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable Long id,
            @RequestParam String uid) {
        service.deleteNotification(id, uid);
        return ResponseEntity.ok(Map.of("deleted", true, "id", id));
    }

    @PostMapping("/user/{uid}/create")
    public ResponseEntity<Notification> create(
            @PathVariable String uid,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.createNotification(
                uid,
                body.get("type"),
                body.get("title"),
                body.get("message"),
                body.getOrDefault("actionUrl", null)));
    }
}

