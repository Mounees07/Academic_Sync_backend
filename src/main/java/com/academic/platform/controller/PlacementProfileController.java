package com.academic.platform.controller;

import com.academic.platform.model.Role;
import com.academic.platform.model.PlacementDriveApplication;
import com.academic.platform.model.PlacementDrive;
import com.academic.platform.model.PlacementProfile;
import com.academic.platform.model.User;
import com.academic.platform.repository.PlacementDriveRepository;
import com.academic.platform.repository.PlacementProfileRepository;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.service.PlacementDriveWorkflowService;
import com.academic.platform.utils.SecurityUtils;
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

    @Autowired
    private PlacementDriveRepository driveRepository;

    @Autowired
    private PlacementDriveWorkflowService driveWorkflowService;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping("/student/{uid}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String uid) {
        PlacementProfile profile = profileRepo.findByStudentFirebaseUid(uid).orElse(null);
        if (profile == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("resumeUploaded", false);
            empty.put("resumeUrl", null);
            empty.put("skillsCompleted", 0);
            empty.put("totalSkills", 10);
            empty.put("aptitudeScore", 0);
            empty.put("mockInterviewScore", 0);
            empty.put("readinessScore", 0);
            empty.put("completedSkillsList", "");
            empty.put("preferredRole", "");
            empty.put("preferredCompanies", "");
            empty.put("placementStatus", "NOT_READY");
            empty.put("resumeReviewStatus", "PENDING");
            empty.put("resumeRemarks", "");
            empty.put("availableDrives", buildStudentDrives(uid));
            return ResponseEntity.ok(empty);
        }
        Map<String, Object> response = buildResponse(profile);
        response.put("availableDrives", buildStudentDrives(uid));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/student/{uid}/update")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @PathVariable String uid,
            @RequestBody Map<String, Object> body) {
        String actorUid = securityUtils.getCurrentUserUid();
        User actor = actorUid == null ? null : userRepository.findByFirebaseUid(actorUid).orElse(null);
        boolean isSelfUpdate = actorUid != null && actorUid.equals(uid);
        boolean canManagePlacement = actor != null &&
                (actor.getRole() == Role.PLACEMENT_COORDINATOR || actor.getRole() == Role.ADMIN);

        if (!isSelfUpdate && !canManagePlacement) {
            return ResponseEntity.status(403).build();
        }

        User student = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new RuntimeException("Student not found: " + uid));


        PlacementProfile profile = profileRepo.findByStudentFirebaseUid(uid)
                .orElse(PlacementProfile.builder().student(student).build());

        if (body.containsKey("resumeUploaded")) {
            profile.setResumeUploaded(Boolean.parseBoolean(body.get("resumeUploaded").toString()));
        }
        if (body.containsKey("resumeUrl")) {
            String resumeUrl = body.get("resumeUrl") == null ? null : body.get("resumeUrl").toString().trim();
            profile.setResumeUrl(resumeUrl == null || resumeUrl.isBlank() ? null : resumeUrl);
            profile.setResumeUploaded(resumeUrl != null && !resumeUrl.isBlank());
        }

        if (canManagePlacement) {
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
        }

        PlacementProfile saved = profileRepo.save(profile);
        Map<String, Object> response = buildResponse(saved);
        response.put("availableDrives", buildStudentDrives(uid));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{uid}/drives")
    public ResponseEntity<?> getStudentDrives(@PathVariable String uid) {
        return ResponseEntity.ok(buildStudentDrives(uid));
    }

    @PutMapping("/student/{uid}/drives/{driveId}/apply")
    public ResponseEntity<?> applyForDrive(@PathVariable String uid, @PathVariable Long driveId) {
        String actorUid = securityUtils.getCurrentUserUid();
        if (actorUid == null || !actorUid.equals(uid)) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        driveWorkflowService.applyForDrive(driveId, uid);
        return ResponseEntity.ok(buildStudentDrives(uid));
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
        m.put("placementStatus", p.getPlacementStatus());
        m.put("resumeReviewStatus", p.getResumeReviewStatus());
        m.put("resumeRemarks", p.getResumeRemarks());
        m.put("updatedAt", p.getUpdatedAt());
        return m;
    }

    private List<Map<String, Object>> buildStudentDrives(String uid) {
        Map<Long, PlacementDriveApplication> applicationByDrive = driveWorkflowService.getApplicationsForStudent(uid)
                .stream()
                .collect(java.util.stream.Collectors.toMap(app -> app.getDrive().getId(), app -> app, (left, right) -> left, LinkedHashMap::new));

        return driveRepository.findAllByOrderByDriveDateDesc().stream()
                .filter(drive -> applicationByDrive.containsKey(drive.getId()))
                .map(drive -> {
                    PlacementDriveApplication application = applicationByDrive.get(drive.getId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", drive.getId());
                    row.put("companyName", drive.getCompany() != null ? drive.getCompany().getCompanyName() : "Unknown");
                    row.put("roleTitle", drive.getRoleTitle());
                    row.put("driveDate", drive.getDriveDate());
                    row.put("location", drive.getLocation());
                    row.put("eligibilityCriteria", drive.getEligibilityCriteria());
                    row.put("description", drive.getDescription());
                    row.put("status", drive.getStatus());
                    row.put("applicationStatus", application.getStatus());
                    row.put("appliedAt", application.getAppliedAt());
                    row.put("coordinatorRemarks", application.getCoordinatorRemarks());
                    row.put("reminderCount", application.getReminderCount());
                    row.put("canApply", "ELIGIBLE".equals(application.getStatus()));
                    return row;
                })
                .toList();
    }
}

