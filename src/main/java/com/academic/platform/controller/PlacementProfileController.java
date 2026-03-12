package com.academic.platform.controller;

import com.academic.platform.model.PlacementProfile;
import com.academic.platform.model.User;
import com.academic.platform.repository.PlacementProfileRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/placement")
public class PlacementProfileController {

    @Autowired
    private PlacementProfileRepository profileRepo;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/student/{uid}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String uid) {
        PlacementProfile profile = profileRepo.findByStudentFirebaseUid(uid).orElse(null);
        if (profile == null) {
            // Return empty profile
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("resumeUploaded", false);
            empty.put("skillsCompleted", 0);
            empty.put("totalSkills", 10);
            empty.put("aptitudeScore", 0);
            empty.put("mockInterviewScore", 0);
            empty.put("readinessScore", 0);
            empty.put("completedSkillsList", "");
            return ResponseEntity.ok(empty);
        }
        return ResponseEntity.ok(buildResponse(profile));
    }

    @PutMapping("/student/{uid}/update")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @PathVariable String uid,
            @RequestBody Map<String, Object> body) {
        User student = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new RuntimeException("Student not found: " + uid));


        PlacementProfile profile = profileRepo.findByStudentFirebaseUid(uid)
                .orElse(PlacementProfile.builder().student(student).build());

        if (body.containsKey("resumeUploaded"))
            profile.setResumeUploaded(Boolean.parseBoolean(body.get("resumeUploaded").toString()));
        if (body.containsKey("resumeUrl"))
            profile.setResumeUrl((String) body.get("resumeUrl"));
        if (body.containsKey("skillsCompleted"))
            profile.setSkillsCompleted(Integer.parseInt(body.get("skillsCompleted").toString()));
        if (body.containsKey("totalSkills"))
            profile.setTotalSkills(Integer.parseInt(body.get("totalSkills").toString()));
        if (body.containsKey("aptitudeScore"))
            profile.setAptitudeScore(Double.parseDouble(body.get("aptitudeScore").toString()));
        if (body.containsKey("mockInterviewScore"))
            profile.setMockInterviewScore(Double.parseDouble(body.get("mockInterviewScore").toString()));
        if (body.containsKey("completedSkillsList"))
            profile.setCompletedSkillsList((String) body.get("completedSkillsList"));
        if (body.containsKey("preferredRole"))
            profile.setPreferredRole((String) body.get("preferredRole"));
        if (body.containsKey("preferredCompanies"))
            profile.setPreferredCompanies((String) body.get("preferredCompanies"));

        PlacementProfile saved = profileRepo.save(profile);
        return ResponseEntity.ok(buildResponse(saved));
    }

    private Map<String, Object> buildResponse(PlacementProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("resumeUploaded", p.getResumeUploaded());
        m.put("resumeUrl", p.getResumeUrl());
        m.put("skillsCompleted", p.getSkillsCompleted());
        m.put("totalSkills", p.getTotalSkills());
        m.put("aptitudeScore", p.getAptitudeScore());
        m.put("mockInterviewScore", p.getMockInterviewScore());
        m.put("readinessScore", p.getReadinessScore());
        m.put("completedSkillsList", p.getCompletedSkillsList());
        m.put("preferredRole", p.getPreferredRole());
        m.put("preferredCompanies", p.getPreferredCompanies());
        m.put("updatedAt", p.getUpdatedAt());
        return m;
    }
}

