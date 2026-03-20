package com.academic.platform.controller;

import com.academic.platform.model.PlacementCompany;
import com.academic.platform.model.PlacementDrive;
import com.academic.platform.model.PlacementProfile;
import com.academic.platform.model.Role;
import com.academic.platform.model.User;
import com.academic.platform.repository.PlacementCompanyRepository;
import com.academic.platform.repository.PlacementDriveRepository;
import com.academic.platform.repository.PlacementProfileRepository;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/placement/coordinator")
public class PlacementCoordinatorController {

    @Autowired
    private PlacementProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlacementCompanyRepository companyRepository;

    @Autowired
    private PlacementDriveRepository driveRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        List<Map<String, Object>> students = buildStudentRows();
        long totalStudents = students.size();
        long eligibleStudents = students.stream()
                .filter(student -> ((Number) student.get("readinessScore")).doubleValue() >= 60.0)
                .count();
        long placedStudents = students.stream()
                .filter(student -> "PLACED".equals(student.get("placementStatus")))
                .count();
        double avgReadiness = students.stream()
                .mapToDouble(student -> ((Number) student.get("readinessScore")).doubleValue())
                .average()
                .orElse(0.0);

        Map<String, Long> skillsDistribution = new LinkedHashMap<>();
        skillsDistribution.put("0-25", students.stream().filter(s -> ((Number) s.get("readinessScore")).doubleValue() < 25).count());
        skillsDistribution.put("25-50", students.stream().filter(s -> {
            double score = ((Number) s.get("readinessScore")).doubleValue();
            return score >= 25 && score < 50;
        }).count());
        skillsDistribution.put("50-75", students.stream().filter(s -> {
            double score = ((Number) s.get("readinessScore")).doubleValue();
            return score >= 50 && score < 75;
        }).count());
        skillsDistribution.put("75-100", students.stream().filter(s -> ((Number) s.get("readinessScore")).doubleValue() >= 75).count());

        Map<String, Object> placementStats = new LinkedHashMap<>();
        placementStats.put("plannedDrives", driveRepository.findAll().stream().filter(d -> "PLANNED".equalsIgnoreCase(d.getStatus())).count());
        placementStats.put("activeDrives", driveRepository.findAll().stream().filter(d -> "ACTIVE".equalsIgnoreCase(d.getStatus())).count());
        placementStats.put("completedDrives", driveRepository.findAll().stream().filter(d -> "COMPLETED".equalsIgnoreCase(d.getStatus())).count());
        placementStats.put("companies", companyRepository.count());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalStudents", totalStudents);
        response.put("eligibleStudents", eligibleStudents);
        response.put("placedStudents", placedStudents);
        response.put("averageReadinessScore", Math.round(avgReadiness * 100.0) / 100.0);
        response.put("skillsDistribution", skillsDistribution);
        response.put("placementStats", placementStats);
        response.put("students", students.stream().limit(8).toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/students")
    public ResponseEntity<?> getStudents() {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        return ResponseEntity.ok(buildStudentRows());
    }

    @PutMapping("/students/{uid}")
    public ResponseEntity<?> updateStudent(@PathVariable String uid, @RequestBody Map<String, Object> body) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        User student = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new RuntimeException("Student not found."));
        PlacementProfile profile = profileRepository.findByStudentFirebaseUid(uid)
                .orElse(PlacementProfile.builder().student(student).build());

        if (body.containsKey("skillsCompleted")) {
            profile.setSkillsCompleted(Integer.parseInt(String.valueOf(body.get("skillsCompleted"))));
        }
        if (body.containsKey("totalSkills")) {
            profile.setTotalSkills(Integer.parseInt(String.valueOf(body.get("totalSkills"))));
        }
        if (body.containsKey("aptitudeScore")) {
            profile.setAptitudeScore(Double.parseDouble(String.valueOf(body.get("aptitudeScore"))));
        }
        if (body.containsKey("mockInterviewScore")) {
            profile.setMockInterviewScore(Double.parseDouble(String.valueOf(body.get("mockInterviewScore"))));
        }
        if (body.containsKey("completedSkillsList")) {
            profile.setCompletedSkillsList(body.get("completedSkillsList") == null ? "" : String.valueOf(body.get("completedSkillsList")));
        }
        if (body.containsKey("resumeReviewStatus")) {
            profile.setResumeReviewStatus(String.valueOf(body.get("resumeReviewStatus")).toUpperCase(Locale.ROOT));
        }
        if (body.containsKey("resumeRemarks")) {
            profile.setResumeRemarks(body.get("resumeRemarks") == null ? null : String.valueOf(body.get("resumeRemarks")));
        }
        if (body.containsKey("placementStatus")) {
            profile.setPlacementStatus(String.valueOf(body.get("placementStatus")).toUpperCase(Locale.ROOT));
        }
        if (body.containsKey("preferredRole")) {
            profile.setPreferredRole(body.get("preferredRole") == null ? null : String.valueOf(body.get("preferredRole")));
        }
        if (body.containsKey("preferredCompanies")) {
            profile.setPreferredCompanies(body.get("preferredCompanies") == null ? null : String.valueOf(body.get("preferredCompanies")));
        }
        if (student.getStudentDetails() != null && student.getStudentDetails().getCgpa() != null) {
            profile.setCgpaScore(student.getStudentDetails().getCgpa());
        }

        PlacementProfile saved = profileRepository.save(profile);
        return ResponseEntity.ok(buildStudentRow(student, saved));
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        List<Map<String, Object>> students = buildStudentRows();
        Map<String, Long> departmentWise = students.stream()
                .collect(Collectors.groupingBy(
                        student -> String.valueOf(student.get("department")),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> companySelections = new LinkedHashMap<>();
        for (PlacementDrive drive : driveRepository.findAllByOrderByDriveDateDesc()) {
            String companyName = drive.getCompany() != null ? drive.getCompany().getCompanyName() : "Unknown";
            companySelections.merge(companyName, (long) drive.getSelectedStudents().size(), Long::sum);
        }

        double placementPercentage = students.isEmpty() ? 0.0 :
                (students.stream().filter(student -> "PLACED".equals(student.get("placementStatus"))).count() * 100.0) / students.size();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("placementPercentage", Math.round(placementPercentage * 100.0) / 100.0);
        response.put("departmentWisePerformance", departmentWise);
        response.put("companyWiseSelections", companySelections);
        response.put("statusBreakdown", Map.of(
                "NOT_READY", students.stream().filter(student -> "NOT_READY".equals(student.get("placementStatus"))).count(),
                "ELIGIBLE", students.stream().filter(student -> "ELIGIBLE".equals(student.get("placementStatus"))).count(),
                "PLACED", students.stream().filter(student -> "PLACED".equals(student.get("placementStatus"))).count()
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/companies")
    public ResponseEntity<?> getCompanies() {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        return ResponseEntity.ok(companyRepository.findAllByOrderByCompanyNameAsc());
    }

    @PostMapping("/companies")
    public ResponseEntity<?> createCompany(@RequestBody PlacementCompany company) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        return ResponseEntity.ok(companyRepository.save(company));
    }

    @PutMapping("/companies/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Long id, @RequestBody PlacementCompany payload) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        PlacementCompany company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found."));
        company.setCompanyName(payload.getCompanyName());
        company.setIndustry(payload.getIndustry());
        company.setLocation(payload.getLocation());
        company.setWebsite(payload.getWebsite());
        company.setPackageOffered(payload.getPackageOffered());
        company.setStatus(payload.getStatus());
        company.setNotes(payload.getNotes());
        return ResponseEntity.ok(companyRepository.save(company));
    }

    @DeleteMapping("/companies/{id}")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        companyRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/drives")
    public ResponseEntity<?> getDrives() {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        return ResponseEntity.ok(driveRepository.findAllByOrderByDriveDateDesc().stream().map(this::buildDriveResponse).toList());
    }

    @PostMapping("/drives")
    public ResponseEntity<?> createDrive(@RequestBody Map<String, Object> body) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        PlacementDrive drive = new PlacementDrive();
        applyDrivePayload(drive, body);
        return ResponseEntity.ok(buildDriveResponse(driveRepository.save(drive)));
    }

    @PutMapping("/drives/{id}")
    public ResponseEntity<?> updateDrive(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        PlacementDrive drive = driveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drive not found."));
        applyDrivePayload(drive, body);
        return ResponseEntity.ok(buildDriveResponse(driveRepository.save(drive)));
    }

    @DeleteMapping("/drives/{id}")
    public ResponseEntity<?> deleteDrive(@PathVariable Long id) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        driveRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private void applyDrivePayload(PlacementDrive drive, Map<String, Object> body) {
        Long companyId = Long.parseLong(String.valueOf(body.get("companyId")));
        PlacementCompany company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found."));
        drive.setCompany(company);
        drive.setRoleTitle(String.valueOf(body.getOrDefault("roleTitle", "")));
        String driveDate = String.valueOf(body.getOrDefault("driveDate", ""));
        drive.setDriveDate(driveDate.isBlank() ? null : LocalDate.parse(driveDate));
        drive.setLocation(String.valueOf(body.getOrDefault("location", "")));
        drive.setEligibilityCriteria(String.valueOf(body.getOrDefault("eligibilityCriteria", "")));
        drive.setDescription(String.valueOf(body.getOrDefault("description", "")));
        drive.setStatus(String.valueOf(body.getOrDefault("status", "PLANNED")).toUpperCase(Locale.ROOT));
        drive.setEligibleStudents(resolveUsers(body.get("eligibleStudentUids")));
        drive.setAppliedStudents(resolveUsers(body.get("appliedStudentUids")));
        drive.setSelectedStudents(resolveUsers(body.get("selectedStudentUids")));
    }

    private Set<User> resolveUsers(Object rawValue) {
        Set<User> users = new LinkedHashSet<>();
        if (!(rawValue instanceof List<?> items)) {
            return users;
        }
        for (Object item : items) {
            if (item == null) {
                continue;
            }
            userRepository.findByFirebaseUid(String.valueOf(item)).ifPresent(users::add);
        }
        return users;
    }

    private Map<String, Object> buildDriveResponse(PlacementDrive drive) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", drive.getId());
        response.put("companyId", drive.getCompany() != null ? drive.getCompany().getId() : null);
        response.put("companyName", drive.getCompany() != null ? drive.getCompany().getCompanyName() : "Unknown");
        response.put("roleTitle", drive.getRoleTitle());
        response.put("driveDate", drive.getDriveDate());
        response.put("location", drive.getLocation());
        response.put("eligibilityCriteria", drive.getEligibilityCriteria());
        response.put("description", drive.getDescription());
        response.put("status", drive.getStatus());
        response.put("eligibleStudents", drive.getEligibleStudents().stream().map(this::buildStudentMini).toList());
        response.put("appliedStudents", drive.getAppliedStudents().stream().map(this::buildStudentMini).toList());
        response.put("selectedStudents", drive.getSelectedStudents().stream().map(this::buildStudentMini).toList());
        response.put("eligibleCount", drive.getEligibleStudents().size());
        response.put("appliedCount", drive.getAppliedStudents().size());
        response.put("selectedCount", drive.getSelectedStudents().size());
        return response;
    }

    private Map<String, Object> buildStudentMini(User student) {
        Map<String, Object> mini = new LinkedHashMap<>();
        mini.put("uid", student.getFirebaseUid());
        mini.put("fullName", student.getFullName());
        mini.put("department", student.getStudentDetails() != null ? student.getStudentDetails().getDepartment() : "");
        return mini;
    }

    private List<Map<String, Object>> buildStudentRows() {
        return userRepository.findByRole(Role.STUDENT).stream()
                .map(student -> {
                    PlacementProfile profile = profileRepository.findByStudentFirebaseUid(student.getFirebaseUid())
                            .orElse(PlacementProfile.builder().student(student).build());
                    if (student.getStudentDetails() != null) {
                        Double cgpa = student.getStudentDetails().getCgpa();
                        if (cgpa == null) {
                            cgpa = student.getStudentDetails().getGpa();
                        }
                        profile.setCgpaScore(cgpa == null ? 0.0 : cgpa);
                    }
                    return buildStudentRow(student, profile);
                })
                .sorted((left, right) -> Double.compare(
                        ((Number) right.get("readinessScore")).doubleValue(),
                        ((Number) left.get("readinessScore")).doubleValue()))
                .toList();
    }

    private Map<String, Object> buildStudentRow(User student, PlacementProfile profile) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("uid", student.getFirebaseUid());
        row.put("name", student.getFullName());
        row.put("email", student.getEmail());
        row.put("department", student.getStudentDetails() != null ? defaultString(student.getStudentDetails().getDepartment()) : "");
        row.put("rollNumber", student.getStudentDetails() != null ? defaultString(student.getStudentDetails().getRollNumber()) : "");
        row.put("year", student.getStudentDetails() != null && student.getStudentDetails().getSemester() != null
                ? Math.max(1, (student.getStudentDetails().getSemester() + 1) / 2)
                : 1);
        row.put("readinessScore", profile.getReadinessScore());
        row.put("skillsCompleted", profile.getSkillsCompleted());
        row.put("totalSkills", profile.getTotalSkills());
        row.put("aptitudeScore", profile.getAptitudeScore());
        row.put("mockInterviewScore", profile.getMockInterviewScore());
        row.put("resumeUrl", profile.getResumeUrl());
        row.put("resumeUploaded", profile.getResumeUploaded());
        row.put("resumeReviewStatus", defaultString(profile.getResumeReviewStatus(), "PENDING"));
        row.put("resumeRemarks", profile.getResumeRemarks());
        row.put("placementStatus", defaultString(profile.getPlacementStatus(), derivePlacementStatus(profile)));
        row.put("completedSkillsList", defaultString(profile.getCompletedSkillsList()));
        row.put("preferredRole", defaultString(profile.getPreferredRole()));
        row.put("preferredCompanies", defaultString(profile.getPreferredCompanies()));
        row.put("cgpaScore", profile.getCgpaScore() == null ? 0.0 : profile.getCgpaScore());
        return row;
    }

    private String derivePlacementStatus(PlacementProfile profile) {
        if (profile.getReadinessScore() >= 80) return "ELIGIBLE";
        return "NOT_READY";
    }

    private String defaultString(String value) {
        return defaultString(value, "");
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isCoordinatorOrAdmin() {
        String uid = securityUtils.getCurrentUserUid();
        if (uid == null) return false;
        User actor = userRepository.findByFirebaseUid(uid).orElse(null);
        return actor != null &&
                (actor.getRole() == Role.PLACEMENT_COORDINATOR || actor.getRole() == Role.ADMIN);
    }
}
