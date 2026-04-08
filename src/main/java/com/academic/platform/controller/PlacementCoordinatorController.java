package com.academic.platform.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.academic.platform.model.PlacementCompany;
import com.academic.platform.model.PlacementDrive;
import com.academic.platform.model.PlacementDriveApplication;
import com.academic.platform.model.PlacementProfile;
import com.academic.platform.model.Role;
import com.academic.platform.model.User;
import com.academic.platform.repository.PlacementCompanyRepository;
import com.academic.platform.repository.PlacementDriveRepository;
import com.academic.platform.repository.PlacementProfileRepository;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.service.PlacementDriveWorkflowService;
import com.academic.platform.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/placement/coordinator")
public class PlacementCoordinatorController {

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {};

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

    @Autowired
    private PlacementDriveWorkflowService driveWorkflowService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public ResponseEntity<?> getStudents() {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        return ResponseEntity.ok(buildStudentRows());
    }

    @GetMapping("/assessments")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAssessments() {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        List<Map<String, Object>> students = buildStudentRows();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("studentCount", students.size());
        summary.put("averageAssessmentScore", roundTwoDecimals(students.stream()
                .mapToDouble(student -> toDouble(student.get("averageAssessmentScore")))
                .average()
                .orElse(0.0)));
        summary.put("averageTrainingAttendance", roundTwoDecimals(students.stream()
                .mapToDouble(student -> toDouble(student.get("averageAttendancePercent")))
                .average()
                .orElse(0.0)));
        summary.put("averagePlacementRoundScore", roundTwoDecimals(students.stream()
                .mapToDouble(student -> toDouble(student.get("averagePlacementRoundScore")))
                .average()
                .orElse(0.0)));
        summary.put("studentsWithAssessments", students.stream()
                .filter(student -> toInt(student.get("attendanceCount")) > 0
                        || toInt(student.get("assessmentScoreCount")) > 0
                        || toInt(student.get("placementRoundCount")) > 0)
                .count());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("students", students);
        return ResponseEntity.ok(response);
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
        if (body.containsKey("trainingAttendance") || body.containsKey("assessmentScores") || body.containsKey("activityScores")) {
            profile.setActivityScoresJson(writeJson(mergeActivityEntries(body)));
        }
        if (body.containsKey("placementRounds")) {
            profile.setPlacementRoundsJson(writeJson(sanitizePlacementRoundEntries(body.get("placementRounds"))));
        }
        if (student.getStudentDetails() != null && student.getStudentDetails().getCgpa() != null) {
            profile.setCgpaScore(student.getStudentDetails().getCgpa());
        }

        PlacementProfile saved = profileRepository.save(profile);
        return ResponseEntity.ok(buildStudentRow(student, saved));
    }

    @GetMapping("/analytics")
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
        PlacementDrive saved = driveRepository.save(drive);
        driveWorkflowService.syncDriveApplications(saved, Collections.emptySet());
        return ResponseEntity.ok(buildDriveResponse(saved));
    }

    @PutMapping("/drives/{id}")
    public ResponseEntity<?> updateDrive(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        PlacementDrive drive = driveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drive not found."));
        Set<String> previousEligibleStudents = drive.getEligibleStudents().stream()
                .map(User::getFirebaseUid)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        applyDrivePayload(drive, body);
        PlacementDrive saved = driveRepository.save(drive);
        driveWorkflowService.syncDriveApplications(saved, previousEligibleStudents);
        return ResponseEntity.ok(buildDriveResponse(saved));
    }

    @PutMapping("/drives/{id}/applications/{uid}")
    public ResponseEntity<?> reviewApplication(@PathVariable Long id,
                                               @PathVariable String uid,
                                               @RequestBody Map<String, Object> body) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        String status = String.valueOf(body.getOrDefault("status", "ELIGIBLE"));
        String remarks = body.get("coordinatorRemarks") == null ? null : String.valueOf(body.get("coordinatorRemarks"));
        driveWorkflowService.reviewApplication(id, uid, status, remarks);
        PlacementDrive drive = driveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drive not found."));
        return ResponseEntity.ok(buildDriveResponse(drive));
    }

    @PutMapping("/drives/{id}/applications/{uid}/attendance")
    public ResponseEntity<?> markAttendance(@PathVariable Long id,
                                            @PathVariable String uid,
                                            @RequestBody Map<String, Object> body) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        boolean attended = Boolean.parseBoolean(String.valueOf(body.getOrDefault("attended", false)));
        driveWorkflowService.markAttendance(id, uid, attended);
        PlacementDrive drive = driveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drive not found."));
        return ResponseEntity.ok(buildDriveResponse(drive));
    }

    @DeleteMapping("/drives/{id}")
    public ResponseEntity<?> deleteDrive(@PathVariable Long id) {
        if (!isCoordinatorOrAdmin()) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        driveWorkflowService.deleteApplicationsForDrive(id);
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
        String status = String.valueOf(body.getOrDefault("status", "PLANNED")).toUpperCase(Locale.ROOT);
        Set<User> eligibleStudents = resolveUsers(body.get("eligibleStudentUids"));
        Set<User> appliedStudents = resolveUsers(body.get("appliedStudentUids"));
        Set<User> selectedStudents = resolveUsers(body.get("selectedStudentUids"));

        if ("COMPLETED".equals(status) && selectedStudents.isEmpty()) {
            throw new RuntimeException("Select the placed students before marking the drive as completed.");
        }

        if (!selectedStudents.isEmpty()) {
            appliedStudents.addAll(selectedStudents);
            eligibleStudents.addAll(selectedStudents);
        }

        drive.setStatus(status);
        drive.setEligibleStudents(eligibleStudents);
        drive.setAppliedStudents(appliedStudents);
        drive.setSelectedStudents(selectedStudents);
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
        List<PlacementDriveApplication> applications = driveWorkflowService.getApplicationsForDrive(drive.getId());

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
        response.put("applications", applications.stream().map(this::buildApplicationResponse).toList());
        response.put("eligibleCount", applications.stream().filter(app -> "ELIGIBLE".equals(app.getStatus())).count());
        response.put("appliedCount", applications.stream().filter(app -> Set.of("APPLIED", "SHORTLISTED", "REJECTED").contains(app.getStatus())).count());
        response.put("selectedCount", applications.stream().filter(app -> "SHORTLISTED".equals(app.getStatus())).count());
        response.put("reminderPendingCount", applications.stream().filter(app -> "ELIGIBLE".equals(app.getStatus())).count());
        return response;
    }

    private Map<String, Object> buildApplicationResponse(PlacementDriveApplication application) {
        Map<String, Object> row = buildStudentRow(
                application.getStudent(),
                profileRepository.findByStudentFirebaseUid(application.getStudent().getFirebaseUid())
                        .orElse(PlacementProfile.builder().student(application.getStudent()).build())
        );
        row.put("applicationStatus", application.getStatus());
        row.put("appliedAt", application.getAppliedAt());
        row.put("reviewedAt", application.getReviewedAt());
        row.put("attended", Boolean.TRUE.equals(application.getAttended()));
        row.put("attendanceMarkedAt", application.getAttendanceMarkedAt());
        row.put("coordinatorRemarks", defaultString(application.getCoordinatorRemarks()));
        row.put("reminderCount", application.getReminderCount() == null ? 0 : application.getReminderCount());
        row.put("mentorName", application.getStudent().getStudentDetails() != null && application.getStudent().getStudentDetails().getMentor() != null
                ? defaultString(application.getStudent().getStudentDetails().getMentor().getFullName())
                : "");
        return row;
    }

    private Map<String, Object> buildStudentMini(User student) {
        Map<String, Object> mini = new LinkedHashMap<>();
        mini.put("uid", student.getFirebaseUid());
        mini.put("fullName", student.getFullName());
        mini.put("department", student.getStudentDetails() != null ? student.getStudentDetails().getDepartment() : "");
        return mini;
    }

    private List<Map<String, Object>> buildStudentRows() {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Object[] snapshot : userRepository.findPlacementStudentSnapshotsByRole(Role.STUDENT)) {
            try {
                String firebaseUid = asString(snapshot[0]);
                String fullName = asString(snapshot[1]);
                String email = asString(snapshot[2]);
                String department = asString(snapshot[3]);
                String rollNumber = asString(snapshot[4]);
                Integer semester = snapshot[5] == null ? null : toInt(snapshot[5]);
                Double cgpaScore = snapshot[6] == null ? 0.0 : toDouble(snapshot[6]);

                PlacementProfile profile = profileRepository.findByStudentFirebaseUid(firebaseUid)
                        .orElse(null);

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("uid", firebaseUid);
                row.put("name", defaultString(fullName, "Student"));
                row.put("email", defaultString(email));
                row.put("department", defaultString(department));
                row.put("rollNumber", defaultString(rollNumber));
                row.put("year", semester != null ? Math.max(1, (semester + 1) / 2) : 1);

                double readinessScore = profile != null ? profile.getReadinessScore() : 0.0;
                row.put("readinessScore", readinessScore);
                row.put("skillsCompleted", profile != null && profile.getSkillsCompleted() != null ? profile.getSkillsCompleted() : 0);
                row.put("totalSkills", profile != null && profile.getTotalSkills() != null ? profile.getTotalSkills() : 10);
                row.put("aptitudeScore", profile != null && profile.getAptitudeScore() != null ? profile.getAptitudeScore() : 0.0);
                row.put("mockInterviewScore", profile != null && profile.getMockInterviewScore() != null ? profile.getMockInterviewScore() : 0.0);
                row.put("resumeUrl", profile != null ? profile.getResumeUrl() : null);
                row.put("resumeUploaded", profile != null && Boolean.TRUE.equals(profile.getResumeUploaded()));
                row.put("resumeReviewStatus", profile != null ? defaultString(profile.getResumeReviewStatus(), "PENDING") : "PENDING");
                row.put("resumeRemarks", profile != null ? profile.getResumeRemarks() : null);
                row.put("placementStatus", profile != null
                        ? defaultString(profile.getPlacementStatus(), derivePlacementStatus(profile))
                        : "NOT_READY");
                row.put("completedSkillsList", profile != null ? defaultString(profile.getCompletedSkillsList()) : "");
                row.put("preferredRole", profile != null ? defaultString(profile.getPreferredRole()) : "");
                row.put("preferredCompanies", profile != null ? defaultString(profile.getPreferredCompanies()) : "");
                row.put("cgpaScore", cgpaScore);

                List<Map<String, Object>> activityScores = profile != null
                        ? sanitizeActivityEntries(readJsonList(profile.getActivityScoresJson()))
                        : new ArrayList<>();
                List<Map<String, Object>> placementRounds = profile != null
                        ? sanitizePlacementRoundEntries(readJsonList(profile.getPlacementRoundsJson()))
                        : new ArrayList<>();
                List<Map<String, Object>> trainingAttendance = filterActivityEntriesByCategory(activityScores, "TRAINING_ATTENDANCE");
                List<Map<String, Object>> assessmentScores = filterAssessmentEntries(activityScores);

                row.put("activityScores", activityScores);
                row.put("trainingAttendance", trainingAttendance);
                row.put("assessmentScores", assessmentScores);
                row.put("placementRounds", placementRounds);
                row.put("activityCount", activityScores.size());
                row.put("attendanceCount", trainingAttendance.size());
                row.put("assessmentScoreCount", assessmentScores.size());
                row.put("placementRoundCount", placementRounds.size());
                row.put("averageActivityScore", calculateAverageScore(assessmentScores));
                row.put("averageAssessmentScore", calculateAverageScore(assessmentScores));
                row.put("averageAttendancePercent", calculateAverageAttendance(trainingAttendance));
                row.put("averagePlacementRoundScore", calculateAverageScore(placementRounds));

                rows.add(row);
            } catch (Exception exception) {
                System.err.println("Skipping placement row for student "
                        + defaultString(snapshot != null && snapshot.length > 0 ? asString(snapshot[0]) : null, "unknown")
                        + ": " + exception.getMessage());
            }
        }

        rows.sort((left, right) -> Double.compare(
                ((Number) right.get("readinessScore")).doubleValue(),
                ((Number) left.get("readinessScore")).doubleValue()));

        return rows;
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
        row.put("skillsCompleted", profile.getSkillsCompleted() == null ? 0 : profile.getSkillsCompleted());
        row.put("totalSkills", profile.getTotalSkills() == null ? 10 : profile.getTotalSkills());
        row.put("aptitudeScore", profile.getAptitudeScore() == null ? 0.0 : profile.getAptitudeScore());
        row.put("mockInterviewScore", profile.getMockInterviewScore() == null ? 0.0 : profile.getMockInterviewScore());
        row.put("resumeUrl", profile.getResumeUrl());
        row.put("resumeUploaded", profile.getResumeUploaded());
        row.put("resumeReviewStatus", defaultString(profile.getResumeReviewStatus(), "PENDING"));
        row.put("resumeRemarks", profile.getResumeRemarks());
        row.put("placementStatus", defaultString(profile.getPlacementStatus(), derivePlacementStatus(profile)));
        row.put("completedSkillsList", defaultString(profile.getCompletedSkillsList()));
        row.put("preferredRole", defaultString(profile.getPreferredRole()));
        row.put("preferredCompanies", defaultString(profile.getPreferredCompanies()));
        row.put("cgpaScore", profile.getCgpaScore() == null ? 0.0 : profile.getCgpaScore());

        List<Map<String, Object>> activityScores = sanitizeActivityEntries(readJsonList(profile.getActivityScoresJson()));
        List<Map<String, Object>> placementRounds = sanitizePlacementRoundEntries(readJsonList(profile.getPlacementRoundsJson()));
        List<Map<String, Object>> trainingAttendance = filterActivityEntriesByCategory(activityScores, "TRAINING_ATTENDANCE");
        List<Map<String, Object>> assessmentScores = filterAssessmentEntries(activityScores);

        row.put("activityScores", activityScores);
        row.put("trainingAttendance", trainingAttendance);
        row.put("assessmentScores", assessmentScores);
        row.put("placementRounds", placementRounds);
        row.put("activityCount", activityScores.size());
        row.put("attendanceCount", trainingAttendance.size());
        row.put("assessmentScoreCount", assessmentScores.size());
        row.put("placementRoundCount", placementRounds.size());
        row.put("averageActivityScore", calculateAverageScore(assessmentScores));
        row.put("averageAssessmentScore", calculateAverageScore(assessmentScores));
        row.put("averageAttendancePercent", calculateAverageAttendance(trainingAttendance));
        row.put("averagePlacementRoundScore", calculateAverageScore(placementRounds));
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

    private List<Map<String, Object>> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, LIST_OF_MAPS);
        } catch (Exception exception) {
            return new ArrayList<>();
        }
    }

    private String writeJson(List<Map<String, Object>> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to save placement assessment details.", exception);
        }
    }

    private List<Map<String, Object>> mergeActivityEntries(Map<String, Object> body) {
        if (body.containsKey("trainingAttendance") || body.containsKey("assessmentScores")) {
            List<Map<String, Object>> merged = new ArrayList<>();
            merged.addAll(sanitizeActivityEntries(body.get("trainingAttendance"), "TRAINING_ATTENDANCE"));
            merged.addAll(sanitizeActivityEntries(body.get("assessmentScores"), "TRAINING_ASSESSMENT"));
            return merged;
        }
        return sanitizeActivityEntries(body.get("activityScores"));
    }

    private List<Map<String, Object>> sanitizeActivityEntries(Object rawValue) {
        return sanitizeActivityEntries(rawValue, null);
    }

    private List<Map<String, Object>> sanitizeActivityEntries(Object rawValue, String forcedCategory) {
        if (!(rawValue instanceof List<?> items)) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> sanitized = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            String name = defaultString(asString(rawMap.get("name")));
            if (name.isBlank()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", nonBlankOrGenerated(rawMap.get("id")));
            row.put("name", name);
            row.put("category", defaultString(forcedCategory, defaultString(asString(rawMap.get("category")), "TRAINING_ASSESSMENT")));
            row.put("semester", sanitizeSemester(rawMap.get("semester")));
            row.put("conductorType", defaultString(asString(rawMap.get("conductorType")), "INTERNAL"));
            row.put("conductorName", defaultString(asString(rawMap.get("conductorName"))));
            row.put("score", clampScore(rawMap.get("score")));
            row.put("maxScore", sanitizeMaxScore(rawMap.get("maxScore")));
            row.put("attendancePercent", clampPercent(rawMap.get("attendancePercent")));
            row.put("remarks", defaultString(asString(rawMap.get("remarks"))));
            sanitized.add(row);
        }
        return sanitized;
    }

    private List<Map<String, Object>> filterActivityEntriesByCategory(List<Map<String, Object>> entries, String category) {
        return entries.stream()
                .filter(entry -> category.equalsIgnoreCase(defaultString(asString(entry.get("category")))))
                .toList();
    }

    private List<Map<String, Object>> filterAssessmentEntries(List<Map<String, Object>> entries) {
        return entries.stream()
                .filter(entry -> !"TRAINING_ATTENDANCE".equalsIgnoreCase(defaultString(asString(entry.get("category")))))
                .toList();
    }

    private List<Map<String, Object>> sanitizePlacementRoundEntries(Object rawValue) {
        if (!(rawValue instanceof List<?> items)) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> sanitized = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            String name = defaultString(asString(rawMap.get("name")));
            if (name.isBlank()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", nonBlankOrGenerated(rawMap.get("id")));
            row.put("name", name);
            row.put("semester", sanitizeSemester(rawMap.get("semester")));
            row.put("conductedBy", defaultString(asString(rawMap.get("conductedBy")), "PLACEMENT_TEAM"));
            row.put("score", clampScore(rawMap.get("score")));
            row.put("maxScore", sanitizeMaxScore(rawMap.get("maxScore")));
            row.put("remarks", defaultString(asString(rawMap.get("remarks"))));
            sanitized.add(row);
        }
        return sanitized;
    }

    private String nonBlankOrGenerated(Object value) {
        String raw = asString(value);
        return raw == null || raw.isBlank() ? UUID.randomUUID().toString() : raw;
    }

    private Integer sanitizeSemester(Object value) {
        int semester = toInt(value);
        if (semester < 1) return 1;
        return Math.min(semester, 8);
    }

    private double sanitizeMaxScore(Object value) {
        double maxScore = toDouble(value);
        return maxScore <= 0 ? 100.0 : roundTwoDecimals(maxScore);
    }

    private double clampScore(Object value) {
        double score = toDouble(value);
        if (score < 0) return 0.0;
        return roundTwoDecimals(score);
    }

    private double clampPercent(Object value) {
        double percent = toDouble(value);
        if (percent < 0) return 0.0;
        return roundTwoDecimals(Math.min(percent, 100.0));
    }

    private double calculateAverageScore(List<Map<String, Object>> entries) {
        return roundTwoDecimals(entries.stream()
                .mapToDouble(entry -> normalizeScore(entry.get("score"), entry.get("maxScore")))
                .average()
                .orElse(0.0));
    }

    private double calculateAverageAttendance(List<Map<String, Object>> entries) {
        return roundTwoDecimals(entries.stream()
                .mapToDouble(entry -> toDouble(entry.get("attendancePercent")))
                .average()
                .orElse(0.0));
    }

    private double normalizeScore(Object scoreValue, Object maxScoreValue) {
        double score = toDouble(scoreValue);
        double maxScore = toDouble(maxScoreValue);
        if (maxScore <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, (score / maxScore) * 100.0));
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private boolean isCoordinatorOrAdmin() {
        String uid = securityUtils.getCurrentUserUid();
        if (uid == null) return false;
        User actor = userRepository.findByFirebaseUid(uid).orElse(null);
        return actor != null &&
                (actor.getRole() == Role.PLACEMENT_COORDINATOR || actor.getRole() == Role.ADMIN);
    }
}
