package com.academic.platform.controller;

import com.academic.platform.model.MentorshipMeeting;
import com.academic.platform.service.MeetingService;
import com.academic.platform.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private SystemSettingService systemSettingService;

    // Helper to check if messaging/meeting feature is enabled
    private boolean isMeetingFeatureEnabled() {
        return Boolean.parseBoolean(systemSettingService.getSetting("feature.messaging.enabled"));
    }

    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleMeeting(
            @RequestParam String mentorUid,
            @RequestParam String menteeUid,
            @RequestBody MentorshipMeeting meeting) {
        if (!isMeetingFeatureEnabled()) {
            return ResponseEntity.status(403).body("Meeting/Messaging module is disabled by administrator.");
        }
        return ResponseEntity.ok(meetingService.scheduleMeeting(mentorUid, menteeUid, meeting));
    }

    @PostMapping("/schedule-bulk")
    public ResponseEntity<?> scheduleGroupMeeting(
            @RequestParam String mentorUid,
            @RequestBody MentorshipMeeting meeting) {
        if (!isMeetingFeatureEnabled()) {
            return ResponseEntity.status(403).body("Meeting/Messaging module is disabled by administrator.");
        }
        return ResponseEntity.ok(meetingService.scheduleGroupMeeting(mentorUid, meeting));
    }

    @GetMapping("/mentor/{mentorUid}")
    public ResponseEntity<?> getMentorMeetings(@PathVariable String mentorUid) {
        if (!isMeetingFeatureEnabled()) {
            return ResponseEntity.status(403).body("Meeting/Messaging module is disabled by administrator.");
        }
        return ResponseEntity.ok(meetingService.getMeetingsForMentor(mentorUid));
    }

    @GetMapping("/mentee/{menteeUid}")
    public ResponseEntity<?> getMenteeMeetings(@PathVariable String menteeUid) {
        if (!isMeetingFeatureEnabled()) {
            return ResponseEntity.status(403).body("Meeting/Messaging module is disabled by administrator.");
        }
        return ResponseEntity.ok(meetingService.getMeetingsForMentee(menteeUid));
    }

    @DeleteMapping("/{meetingId}")
    public ResponseEntity<?> deleteMeeting(@PathVariable Long meetingId) {
        if (!isMeetingFeatureEnabled()) {
            return ResponseEntity.status(403).body("Meeting/Messaging module is disabled by administrator.");
        }
        meetingService.deleteMeeting(meetingId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{meetingId}")
    public ResponseEntity<?> updateMeeting(@PathVariable Long meetingId,
            @RequestBody MentorshipMeeting meeting) {
        if (!isMeetingFeatureEnabled()) {
            return ResponseEntity.status(403).body("Meeting/Messaging module is disabled by administrator.");
        }
        return ResponseEntity.ok(meetingService.updateMeeting(meetingId, meeting));
    }
}
