package com.academic.platform.controller;

// Controller for seeding data

import com.academic.platform.model.Assignment;
import com.academic.platform.model.Course;
import com.academic.platform.model.Enrollment;
import com.academic.platform.model.Role;
import com.academic.platform.model.Section;
import com.academic.platform.model.User;
import com.academic.platform.model.StudentDetails;
import com.academic.platform.repository.AssignmentRepository;
import com.academic.platform.repository.CourseRepository;
import com.academic.platform.repository.EnrollmentRepository;
import com.academic.platform.repository.LeaveRequestRepository;
import com.academic.platform.repository.SectionRepository;
import com.academic.platform.repository.SubmissionRepository;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.model.LeaveRequest; // Import LeaveRequest

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/seed")
@CrossOrigin(origins = { "http://localhost:5173", "http://10.10.188.128:5173" }, allowCredentials = "true")
public class SeedController {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SubmissionRepository subRepo;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    @Autowired
    private com.academic.platform.repository.ExamVenueRepository examVenueRepository; // Added for seeding venues

    // A simple endpoint to populate sample LMS data
    @PostMapping("/lms")
    public ResponseEntity<String> seedLmsData(@RequestParam(required = false) String studentUid) {
        StringBuilder logs = new StringBuilder();

        try {
            // 1. Ensure a few courses exist
            createCourseIfNotExists("CS101", "Introduction to Computer Science",
                    "Basics of programming and algorithms.", 4);
            createCourseIfNotExists("CS202", "Data Structures", "Advanced data handling and organization.", 4);
            createCourseIfNotExists("CS303", "Database Systems", "SQL, NoSQL, and normalization techniques.", 3);
            createCourseIfNotExists("CS404", "Artificial Intelligence", "Neural networks and ML basics.", 4);

            logs.append("Courses verified/created.\n");

            // 2. Ensure a Faculty exists
            Optional<User> facultyOpt = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.TEACHER)
                    .findFirst();

            User faculty;
            if (facultyOpt.isPresent()) {
                faculty = facultyOpt.get();
                logs.append("Found existing faculty: ").append(faculty.getFullName()).append("\n");
            } else {
                List<User> allUsers = userRepository.findAll();
                if (allUsers.isEmpty())
                    return ResponseEntity.badRequest().body("No users found in DB. Please register a user first.");
                faculty = allUsers.get(0); // Fallback
                logs.append("Using fallback user as faculty: ").append(faculty.getFullName()).append("\n");
            }

            // 3. Create Sections for these courses
            List<Course> courses = courseRepository.findAll();

            for (Course c : courses) {
                boolean exists = sectionRepository.existsByCourseAndFacultyAndSemesterAndYear(
                        c, faculty, "Fall", 2026);

                if (!exists) {
                    Section s = Section.builder()
                            .course(c)
                            .faculty(faculty)
                            .semester("Fall")
                            .year(2026)
                            .build();
                    sectionRepository.save(s);
                }
            }
            logs.append("Sections verified/created.\n");

            // 4. Create CSE Faculty if low count
            long cseFacultyCount = userRepository.findByStudentDetails_DepartmentAndRoleIn("CSE", List.of(Role.TEACHER))
                    .size();
            if (cseFacultyCount < 3) {
                for (int i = 1; i <= 3 - cseFacultyCount; i++) {
                    User t = new User();
                    t.setFirebaseUid("seed_fac_uid_" + System.currentTimeMillis() + i);
                    t.setEmail("faculty" + System.currentTimeMillis() + i + "@cse.edu");
                    t.setFullName("CSE Faculty " + i);
                    t.setRole(Role.TEACHER);

                    t.getStudentDetails().setDepartment("CSE");

                    userRepository.save(t);
                }
                logs.append("Created CSE Faculty.\n");
            }

            // 5b. Ensure Students in CSE
            long cseStudentCount = userRepository.findByStudentDetails_DepartmentAndRoleIn("CSE", List.of(Role.STUDENT))
                    .size();
            if (cseStudentCount < 5) {
                for (int i = 1; i <= 5 - cseStudentCount; i++) {
                    User s = new User();
                    s.setFirebaseUid("seed_stu_uid_" + System.currentTimeMillis() + i);
                    s.setEmail("student" + System.currentTimeMillis() + i + "@cse.edu");
                    s.setFullName("CSE Student " + i);
                    s.setRole(Role.STUDENT);

                    StudentDetails details = s.getStudentDetails();
                    details.setDepartment("CSE");
                    details.setSemester(8);
                    details.setSection("A");
                    details.setRollNumber("22CSE" + String.format("%03d", i));

                    userRepository.save(s);
                }
                logs.append("Created CSE Students.\n");
            }

            // 5c. Create Pending Leave Requests for CSE
            List<User> cseStudents = userRepository.findByStudentDetails_DepartmentAndRoleIn("CSE",
                    List.of(Role.STUDENT));
            if (!cseStudents.isEmpty() && leaveRequestRepository.count() == 0) {
                User s = cseStudents.get(0);
                LeaveRequest lr = LeaveRequest.builder()
                        .student(s)
                        .leaveType("Medical")
                        .fromDate(java.time.LocalDate.now().plusDays(1))
                        .toDate(java.time.LocalDate.now().plusDays(3))
                        .reason("Health issues requiring rest.")
                        .parentEmail("parent@test.com")
                        .parentStatus("APPROVED") // Parent approved, waiting for Mentor/HOD
                        .mentorStatus("PENDING")
                        .build();
                leaveRequestRepository.save(lr);
                logs.append("Created sample Leave Request.\n");
            }

            // 5d. Ensure HOD user has CSE department (if HOD exists)
            Optional<User> hodOpt = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.HOD)
                    .findFirst();
            if (hodOpt.isPresent()) {
                User hod = hodOpt.get();
                if (!"CSE".equals(hod.getStudentDetails().getDepartment())) {
                    hod.getStudentDetails().setDepartment("CSE");
                    userRepository.save(hod);
                    logs.append("Updated HOD department to CSE.\n");
                }
            }

            // 6. Ensure Exam Venues exist (for Seating Allocation Demo)
            if (examVenueRepository.count() == 0) {
                com.academic.platform.model.ExamVenue v1 = new com.academic.platform.model.ExamVenue();
                v1.setName("ME201");
                v1.setBlock("Mechanical Block");
                v1.setCapacity(60);
                v1.setExamType("All");
                v1.setAvailable(true);
                examVenueRepository.save(v1);

                com.academic.platform.model.ExamVenue v2 = new com.academic.platform.model.ExamVenue();
                v2.setName("ME202");
                v2.setBlock("Mechanical Block");
                v2.setCapacity(60);
                v2.setExamType("All");
                v2.setAvailable(true);
                examVenueRepository.save(v2);

                logs.append("Created sample Exam Venues (ME201, ME202).\n");
            }

            logs.append("Departments and Users verified/created.\n");

            return ResponseEntity.ok(logs.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Seeding failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/enrollments")
    public ResponseEntity<String> clearEnrollments(@RequestParam String studentUid) {
        try {
            User student = userRepository.findByFirebaseUid(studentUid)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
            enrollmentRepository.deleteAll(enrollments);
            return ResponseEntity.ok("Cleared " + enrollments.size() + " enrollments for student.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to clear enrollments: " + e.getMessage());
        }
    }

    @DeleteMapping("/duplicates")
    public ResponseEntity<String> removeDuplicateSections() {
        try {
            List<Section> allSections = sectionRepository.findAll();
            int removedCount = 0;
            Map<String, List<Section>> groups = new HashMap<>();

            for (Section s : allSections) {
                String key = s.getCourse().getId() + "-" + s.getFaculty().getId() + "-" + s.getSemester() + "-"
                        + s.getYear();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }

            for (List<Section> group : groups.values()) {
                if (group.size() > 1) {
                    for (int i = 1; i < group.size(); i++) {
                        Section toDelete = group.get(i);
                        removeSectionCascade(toDelete);
                        removedCount++;
                    }
                }
            }

            return ResponseEntity.ok("Removed " + removedCount + " duplicate sections.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to remove duplicates: " + e.getMessage());
        }
    }

    private void removeSectionCascade(Section section) {
        List<Enrollment> enrollments = enrollmentRepository.findBySection(section);
        enrollmentRepository.deleteAll(enrollments);

        List<Assignment> assignments = assignmentRepository.findBySection(section);
        for (Assignment a : assignments) {
            subRepo.deleteAll(subRepo.findByAssignment(a));
        }
        assignmentRepository.deleteAll(assignments);

        sectionRepository.delete(section);
    }

    private void createCourseIfNotExists(String code, String name, String desc, Integer credits) {
        if (courseRepository.findByCode(code).isEmpty()) {
            Course c = Course.builder()
                    .code(code)
                    .name(name)
                    .description(desc)
                    .credits(credits)
                    .department("CSE") // Default to CSE for seeding
                    .build();
            courseRepository.save(c);
        } else {
            Course c = courseRepository.findByCode(code).get();
            if (c.getDepartment() == null) {
                c.setDepartment("CSE");
                courseRepository.save(c);
            }
        }
    }

    @DeleteMapping("/demo-students")
    public ResponseEntity<String> deleteDemoStudents() {
        try {
            List<User> demoStudents = userRepository.findAll().stream()
                    .filter(u -> u.getFullName() != null && u.getFullName().startsWith("CSE Student"))
                    .toList();

            for (User s : demoStudents) {
                List<Enrollment> enrollments = enrollmentRepository.findByStudent(s);
                enrollmentRepository.deleteAll(enrollments);

                List<LeaveRequest> leaves = leaveRequestRepository.findByStudentFirebaseUid(s.getFirebaseUid());
                leaveRequestRepository.deleteAll(leaves);
            }

            userRepository.deleteAll(demoStudents);

            return ResponseEntity.ok("Deleted " + demoStudents.size() + " demo students.");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete demo students: " + e.getMessage());
        }
    }

    @DeleteMapping("/demo-assignments")
    public ResponseEntity<String> deleteDemoAssignments() {
        try {
            List<Assignment> allAssignments = assignmentRepository.findAll();
            List<Assignment> demoAssignments = allAssignments.stream()
                    .filter(a -> a.getTitle().startsWith("Assignment 1:") || a.getTitle().equals("Midterm Project"))
                    .toList();

            for (Assignment a : demoAssignments) {
                subRepo.deleteAll(subRepo.findByAssignment(a));
                assignmentRepository.delete(a);
            }

            return ResponseEntity.ok("Deleted " + demoAssignments.size() + " demo assignments.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to deleting demo assignments: " + e.getMessage());
        }
    }

    @PostMapping("/init-admin")
    public ResponseEntity<String> initAdmin() {
        String email = "sankavi8881@gmail.com";
        String password = "Admin@123"; // Default password
        StringBuilder logs = new StringBuilder();

        try {
            Optional<User> existingDbUser = userRepository.findByEmail(email);
            String firebaseUid;

            // 1. Ensure User exists in Firebase
            try {
                com.google.firebase.auth.UserRecord userRecord = com.google.firebase.auth.FirebaseAuth.getInstance()
                        .getUserByEmail(email);
                firebaseUid = userRecord.getUid();
                logs.append("ℹ️ User exists in Firebase (UID: ").append(firebaseUid).append(")\n");
            } catch (com.google.firebase.auth.FirebaseAuthException e) {
                if ("user-not-found".equals(e.getErrorCode())) {
                    // Create in Firebase
                    com.google.firebase.auth.UserRecord.CreateRequest request = new com.google.firebase.auth.UserRecord.CreateRequest()
                            .setEmail(email)
                            .setDisplayName("Admin Sankavi")
                            .setPassword(password)
                            .setEmailVerified(true);
                    com.google.firebase.auth.UserRecord created = com.google.firebase.auth.FirebaseAuth.getInstance()
                            .createUser(request);
                    firebaseUid = created.getUid();
                    logs.append("✅ Created user in Firebase (UID: ").append(firebaseUid).append(")\n");
                } else {
                    throw e;
                }
            }

            // 2. Ensure User exists in Database and is ADMIN
            User user;
            if (existingDbUser.isPresent()) {
                user = existingDbUser.get();
                if (user.getRole() != Role.ADMIN) {
                    user.setRole(Role.ADMIN);
                    userRepository.save(user);
                    logs.append("✅ Updated existing user role to ADMIN.\n");
                } else {
                    logs.append("ℹ️ User is already ADMIN in Database.\n");
                }
            } else {
                user = new User();
                user.setEmail(email);
                user.setFirebaseUid(firebaseUid);
                user.setFullName("Admin Sankavi");
                user.setRole(Role.ADMIN);
                userRepository.save(user);
                logs.append("✅ Created new ADMIN user in Database.\n");
            }

            return ResponseEntity.ok(logs.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to initialize admin: " + e.getMessage());
        }
    }

    @GetMapping("/force-admin-role")
    public ResponseEntity<String> forceAdminRole() {
        String email = "sankavi8881@gmail.com";
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setRole(Role.ADMIN);
                userRepository.save(user);
                return ResponseEntity
                        .ok("✅ Successfully set " + email + " to ADMIN role. Current role: " + user.getRole());
            } else {
                return ResponseEntity.status(404).body("❌ User not found: " + email);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage());
        }
    }
}
