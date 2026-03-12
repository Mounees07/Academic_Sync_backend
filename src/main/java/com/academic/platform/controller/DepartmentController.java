package com.academic.platform.controller;

import com.academic.platform.dto.DepartmentDashboardDTO;
import com.academic.platform.model.User;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/department")
public class DepartmentController {

        @Autowired
        private DepartmentService departmentService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private com.academic.platform.service.SystemSettingService systemSettingService;

        /**
         * Resolve the HOD's department from their Firebase UID and return dashboard
         * stats.
         * This is the primary endpoint used by the frontend since userData.department
         * may be null for faculty/HOD users whose studentDetails is not fully
         * populated.
         */
        @GetMapping("/by-hod/{uid}")
        public ResponseEntity<?> getDashboardByHodUid(@PathVariable String uid) {
                Optional<User> hodOpt = userRepository.findByFirebaseUid(uid);
                if (hodOpt.isEmpty()) {
                        return ResponseEntity.status(404).body("HOD user not found");
                }
                User hod = hodOpt.get();
                String department = null;
                if (hod.getStudentDetails() != null) {
                        department = hod.getStudentDetails().getDepartment();
                }
                if (department == null || department.isBlank()) {
                        return ResponseEntity.status(422).body(
                                        "HOD's department is not configured. Please set the 'department' field in this user's profile.");
                }
                DepartmentDashboardDTO dashboard = departmentService.getDashboardStats(department);
                // Embed the resolved department name so the frontend can cache it
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("department", department);
                result.put("totalFaculty", dashboard.getTotalFaculty());
                result.put("totalStudents", dashboard.getTotalStudents());
                result.put("totalCourses", dashboard.getTotalCourses());
                result.put("pendingLeaves", dashboard.getPendingLeaves());
                result.put("recentActivities", dashboard.getRecentActivities());
                return ResponseEntity.ok(result);
        }

        @GetMapping("/dashboard/{department}")
        public ResponseEntity<DepartmentDashboardDTO> getDashboardStats(@PathVariable String department) {
                return ResponseEntity.ok(departmentService.getDashboardStats(department));
        }

        @GetMapping("/analytics/{department}")
        public ResponseEntity<?> getAnalytics(@PathVariable String department) {
                if ("false".equalsIgnoreCase(systemSettingService.getSetting("feature.analytics.enabled"))) {
                        return ResponseEntity.status(403).body("Analytics module disabled.");
                }
                return ResponseEntity.ok(departmentService.getAnalytics(department));
        }

        @GetMapping("/students-directory/{department}")
        public ResponseEntity<com.academic.platform.dto.DepartmentStudentsDirectoryDTO> getStudentsDirectory(
                        @PathVariable String department) {
                return ResponseEntity.ok(departmentService.getStudentsDirectory(department));
        }

        @GetMapping("/student-attendance/{department}")
        public ResponseEntity<?> getStudentAttendance(@PathVariable String department) {
                return ResponseEntity.ok(departmentService.getStudentAttendance(department));
        }

        @GetMapping("/faculty-workload/{department}")
        public ResponseEntity<?> getFacultyWorkload(@PathVariable String department) {
                return ResponseEntity.ok(departmentService.getFacultyWorkload(department));
        }

        @GetMapping("/resource-utilization/{department}")
        public ResponseEntity<?> getResourceUtilization(@PathVariable String department) {
                return ResponseEntity.ok(departmentService.getResourceUtilization(department));
        }

        @PutMapping("/faculty-workload/{facultyId}")
        public ResponseEntity<?> updateFacultyWorkload(@PathVariable Long facultyId,
                        @RequestBody com.academic.platform.dto.WorkloadUpdateDTO payload) {
                departmentService.updateFacultyWorkload(facultyId, payload.getTeaching(), payload.getResearch(),
                                payload.getAdmin());
                return ResponseEntity.ok().build();
        }
}

