package com.academic.platform.service;

import com.academic.platform.dto.DepartmentAnalyticsDTO;
import com.academic.platform.dto.DepartmentDashboardDTO;
import com.academic.platform.model.Role;
import com.academic.platform.model.User;
import com.academic.platform.model.LeaveRequest;
import com.academic.platform.repository.CourseRepository;
import com.academic.platform.repository.LeaveRequestRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CourseRepository courseRepository;

        @Autowired
        private LeaveRequestRepository leaveRepository;

        @Autowired
        private com.academic.platform.repository.FacultyWorkloadRepository facultyWorkloadRepository;

        @Autowired
        private com.academic.platform.repository.CourseAttendanceRepository courseAttendanceRepository;

        @Autowired
        private com.academic.platform.repository.CourseAttendanceSessionRepository sessionRepository;

        @Autowired
        private com.academic.platform.repository.EnrollmentRepository enrollmentRepository;

        @Autowired
        private com.academic.platform.repository.ExamVenueRepository examVenueRepository;

        public DepartmentDashboardDTO getDashboardStats(String department) {
                DepartmentDashboardDTO dto = new DepartmentDashboardDTO();

                java.util.List<com.academic.platform.model.Role> facultyRoles = java.util.Arrays.asList(
                                com.academic.platform.model.Role.TEACHER,
                                com.academic.platform.model.Role.MENTOR,
                                com.academic.platform.model.Role.HOD,
                                com.academic.platform.model.Role.PRINCIPAL);

                // 1. Faculty Count — try strict department match first, fallback if empty
                java.util.List<User> facultyList = userRepository
                                .findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(department, facultyRoles);
                if (facultyList.isEmpty()) {
                        facultyList = userRepository.findFacultyByDepartmentOrNoDepartment(department, facultyRoles);
                }
                dto.setTotalFaculty(facultyList.size());

                // 2. Student Count
                dto.setTotalStudents(
                                userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(department,
                                                Collections.singletonList(Role.STUDENT)).size());

                // 3. Course Count — use IgnoreCase to avoid case mismatch
                dto.setTotalCourses(courseRepository.findByDepartmentIgnoreCase(department).size());

                // 4. Recent Leaves
                List<LeaveRequest> allLeaves = leaveRepository
                                .findByStudentStudentDetails_DepartmentOrderByCreatedAtDesc(department);
                dto.setRecentActivities(allLeaves.stream().limit(5).toList());

                dto.setPendingLeaves(allLeaves.stream()
                                .filter(l -> "PENDING".equals(l.getMentorStatus())
                                                || "PENDING".equals(l.getParentStatus()))
                                .count());

                return dto;
        }

        public DepartmentAnalyticsDTO getAnalytics(String department) {
                DepartmentAnalyticsDTO dto = new DepartmentAnalyticsDTO();

                List<User> students = userRepository.findByStudentDetails_DepartmentAndRoleIn(department,
                                Collections.singletonList(Role.STUDENT));

                // 1. KPIs
                dto.setActiveStudents(students.size());
                dto.setActiveCourses(courseRepository.findByDepartment(department).size());

                double avgAttendance = students.stream()
                                .mapToDouble(u -> u.getStudentDetails().getAttendance() != null
                                                ? u.getStudentDetails().getAttendance()
                                                : 0.0)
                                .average().orElse(0.0);
                dto.setCurrentAvgAttendance(Math.round(avgAttendance * 10.0) / 10.0);

                double avgGPA = students.stream()
                                .mapToDouble(u -> u.getStudentDetails().getGpa() != null
                                                ? u.getStudentDetails().getGpa()
                                                : 0.0)
                                .average().orElse(0.0);
                dto.setDeptCGPA(Math.round(avgGPA * 100.0) / 100.0);

                // 2. Enrollment Trends (Group by Month - Last 6 months or all)
                // Taking simplistic approach: formatting createdAt to "MMM"
                // Note: Real world would need handling years, etc.
                Map<String, Long> enrollmentMap = students.stream()
                                .filter(u -> u.getCreatedAt() != null)
                                .collect(Collectors.groupingBy(
                                                u -> u.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM")),
                                                Collectors.counting()));

                // Hardcoding a mock list if empty or filling gaps would be better, but let's
                // just map present data
                // To make it look like a trend, we might need to sort by date.
                // For simplicity, let's just return what we have or some basic logic.
                // If data is scarce, this chart might look empty.
                // Let's rely on the fact that we might have seeded data or just show what's
                // there.
                List<DepartmentAnalyticsDTO.EnrollmentTrend> trends = new ArrayList<>();
                enrollmentMap.forEach(
                                (k, v) -> trends.add(new DepartmentAnalyticsDTO.EnrollmentTrend(k, v.intValue())));
                dto.setEnrollmentTrends(trends);

                // 3. Attendance by Year
                Map<Integer, Double> attendanceByYearMap = students.stream()
                                .filter(u -> u.getStudentDetails().getSemester() != null)
                                .collect(Collectors.groupingBy(
                                                u -> (u.getStudentDetails().getSemester() + 1) / 2, // Sem 1,2 -> Year 1
                                                Collectors.averagingDouble(
                                                                u -> u.getStudentDetails().getAttendance() != null
                                                                                ? u.getStudentDetails().getAttendance()
                                                                                : 0.0)));

                List<DepartmentAnalyticsDTO.AttendanceStats> attStats = new ArrayList<>();
                attendanceByYearMap.forEach(
                                (year, att) -> attStats.add(new DepartmentAnalyticsDTO.AttendanceStats("Year " + year,
                                                Math.round(att * 10.0) / 10.0)));
                dto.setAttendanceByYear(attStats);

                // 4. Performance Distribution
                int distinction = 0, first = 0, second = 0, fail = 0;
                for (User s : students) {
                        double g = s.getStudentDetails().getGpa() != null ? s.getStudentDetails().getGpa() : 0.0;
                        if (g >= 8.5)
                                distinction++;
                        else if (g >= 7.0)
                                first++;
                        else if (g >= 5.0)
                                second++;
                        else
                                fail++;
                }
                List<DepartmentAnalyticsDTO.PerformanceDistribution> perfDist = new ArrayList<>();
                perfDist.add(new DepartmentAnalyticsDTO.PerformanceDistribution("Distinction", distinction));
                perfDist.add(new DepartmentAnalyticsDTO.PerformanceDistribution("First Class", first));
                perfDist.add(new DepartmentAnalyticsDTO.PerformanceDistribution("Second Class", second));
                perfDist.add(new DepartmentAnalyticsDTO.PerformanceDistribution("Fail", fail));
                dto.setPerformanceDistribution(perfDist);

                // 5. Top Students
                List<DepartmentAnalyticsDTO.TopStudent> top = students.stream()
                                .sorted(Comparator
                                                .comparingDouble((User u) -> u.getStudentDetails().getGpa() != null
                                                                ? u.getStudentDetails().getGpa()
                                                                : 0.0)
                                                .reversed())
                                .limit(5)
                                .map(u -> new DepartmentAnalyticsDTO.TopStudent(
                                                u.getFullName(),
                                                u.getStudentDetails().getRollNumber(),
                                                u.getStudentDetails().getAttendance() != null
                                                                ? u.getStudentDetails().getAttendance()
                                                                : 0.0,
                                                u.getStudentDetails().getGpa() != null ? u.getStudentDetails().getGpa()
                                                                : 0.0))
                                .collect(Collectors.toList());
                dto.setTopStudents(top);

                return dto;
        }

        public com.academic.platform.dto.DepartmentStudentsDirectoryDTO getStudentsDirectory(String department) {
                List<User> students = userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(department,
                                Collections.singletonList(Role.STUDENT));

                int ug = 0;
                int pg = 0;
                int atRisk = 0;

                for (User s : students) {
                        String degree = s.getStudentDetails().getDegreeLevel();
                        String course = s.getStudentDetails().getCourseName() == null ? ""
                                        : s.getStudentDetails().getCourseName().toUpperCase();
                        if ("UG".equalsIgnoreCase(degree) || course.contains("B.SC") || course.contains("B.E")
                                        || course.contains("B.TECH") || course.startsWith("B")) {
                                ug++;
                        } else if ("PG".equalsIgnoreCase(degree) || course.contains("M.SC") || course.contains("M.E")
                                        || course.contains("M.TECH") || course.startsWith("M")) {
                                pg++;
                        }

                        Double gpa = s.getStudentDetails().getGpa();
                        if (gpa == null)
                                gpa = s.getStudentDetails().getCgpa();
                        if ((gpa != null && gpa < 6.0) || "Academic Warning"
                                        .equalsIgnoreCase(s.getStudentDetails().getStudentStatus())) {
                                atRisk++;
                        }
                }

                if (ug == 0 && pg == 0 && !students.isEmpty()) {
                        ug = students.size();
                }

                com.academic.platform.dto.DepartmentStudentsDirectoryDTO.Stats stats = new com.academic.platform.dto.DepartmentStudentsDirectoryDTO.Stats(
                                students.size(), ug, pg, atRisk);
                return new com.academic.platform.dto.DepartmentStudentsDirectoryDTO(stats, students);
        }

        public Map<String, Object> getFacultyWorkload(String department) {
                Map<String, Object> response = new HashMap<>();

                List<Role> facultyRoles = Arrays.asList(Role.TEACHER, Role.MENTOR, Role.HOD, Role.PRINCIPAL);
                List<User> facultyList = userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(department,
                                facultyRoles);
                if (facultyList.isEmpty()) {
                        facultyList = userRepository.findFacultyByDepartmentOrNoDepartment(department, facultyRoles);
                }

                int totalFaculty = facultyList.size();
                double totalTeaching = 0;
                double totalResearch = 0;
                double totalAdmin = 0;
                int overallocated = 0;
                int leadResearchers = 0;

                List<Map<String, Object>> workloadRecords = new ArrayList<>();

                for (User faculty : facultyList) {
                        Optional<com.academic.platform.model.FacultyWorkload> fwOpt = facultyWorkloadRepository
                                        .findByFaculty_Id(faculty.getId());
                        com.academic.platform.model.FacultyWorkload fw = fwOpt.orElse(null);

                        int teaching = fw != null ? fw.getTeachingHours() : 0;
                        int research = fw != null ? fw.getResearchHours() : 0;
                        int admin = fw != null ? fw.getAdminHours() : 0;

                        int total = teaching + research + admin;
                        totalTeaching += teaching;
                        totalResearch += research;
                        totalAdmin += admin;

                        if (total > 40)
                                overallocated++;
                        if (research > 10)
                                leadResearchers++;

                        String roleStr = faculty.getRole() != null ? faculty.getRole().name() : "Faculty";
                        String status = total > 40 ? "Overallocated" : (total < 10 ? "Underutilized" : "Optimal");
                        String statusClass = total > 40 ? "overallocated" : (total < 10 ? "underutilized" : "optimal");

                        Map<String, Object> rec = new HashMap<>();
                        rec.put("id", faculty.getId());
                        rec.put("name", faculty.getFullName());
                        rec.put("role", roleStr);
                        rec.put("teaching", teaching);
                        rec.put("research", research);
                        rec.put("admin", admin);
                        rec.put("total", total);
                        rec.put("status", status);
                        rec.put("statusClass", statusClass);
                        workloadRecords.add(rec);
                }

                double avgTeachingHours = totalFaculty > 0 ? totalTeaching / totalFaculty : 0.0;

                response.put("stats", Map.of(
                                "totalFaculty", totalFaculty,
                                "avgTeachingHours", Math.round(avgTeachingHours * 10) / 10.0,
                                "activeGrants", leadResearchers * 2,
                                "overallocatedFaculty", overallocated,
                                "leadResearchers", leadResearchers));

                response.put("distributionData", Arrays.asList(
                                Map.of("name", "Teaching", "value", totalTeaching > 0 ? totalTeaching : 1, "color",
                                                "#4F46E5"),
                                Map.of("name", "Research", "value", totalResearch > 0 ? totalResearch : 1, "color",
                                                "#10B981"),
                                Map.of("name", "Admin", "value", totalAdmin > 0 ? totalAdmin : 1, "color", "#F59E0B")));

                response.put("workloadRecords", workloadRecords);
                return response;
        }

        public void updateFacultyWorkload(Long facultyId, int teaching, int research, int admin) {
                User faculty = userRepository.findById(facultyId)
                                .orElseThrow(() -> new RuntimeException("Faculty not found"));

                com.academic.platform.model.FacultyWorkload fw = facultyWorkloadRepository.findByFaculty_Id(facultyId)
                                .orElse(new com.academic.platform.model.FacultyWorkload());
                fw.setFaculty(faculty);
                fw.setTeachingHours(teaching);
                fw.setResearchHours(research);
                fw.setAdminHours(admin);

                facultyWorkloadRepository.save(fw);
        }

        public Map<String, Object> getStudentAttendance(String department) {
                Map<String, Object> response = new HashMap<>();

                List<User> students = userRepository.findByStudentDetails_DepartmentAndRoleIn(department,
                                Collections.singletonList(Role.STUDENT));
                if (students.isEmpty()) {
                        students = userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(department,
                                        Collections.singletonList(Role.STUDENT));
                }

                int perfectAttendance = 0;
                int belowThreshold = 0;
                int unexcusedAbsences = 0;
                double totalPctSum = 0;
                int countWithData = 0;

                List<Map<String, Object>> attendanceRecords = new ArrayList<>();

                for (User student : students) {
                        List<com.academic.platform.model.CourseAttendance> atts = courseAttendanceRepository
                                        .findByStudentFirebaseUidOrderByMarkedAtDesc(student.getFirebaseUid());
                        int totalClasses = atts.size();
                        int classesAttended = (int) atts.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                                        .count();
                        double pct = totalClasses > 0 ? (classesAttended * 100.0) / totalClasses : 0.0;

                        if (totalClasses > 0) {
                                totalPctSum += pct;
                                countWithData++;
                                if (pct == 100.0)
                                        perfectAttendance++;
                                if (pct < 75.0)
                                        belowThreshold++;
                                unexcusedAbsences += (int) atts.stream()
                                                .filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();
                        }

                        Map<String, Object> rec = new HashMap<>();
                        rec.put("name", student.getFullName());
                        rec.put("email", student.getEmail());
                        String roll = student.getStudentDetails() != null ? student.getStudentDetails().getRollNumber()
                                        : "N/A";
                        rec.put("id", roll != null ? roll : "N/A");
                        String course = student.getStudentDetails() != null
                                        ? student.getStudentDetails().getCourseName()
                                        : "N/A";
                        Integer sem = student.getStudentDetails() != null ? student.getStudentDetails().getSemester()
                                        : 1;
                        rec.put("program", course != null ? course.toUpperCase() + " Year "
                                        + Math.round(Math.ceil((sem != null ? sem : 1) / 2.0)) : "N/A");
                        rec.put("classesAttended", classesAttended);
                        rec.put("totalClasses", totalClasses);
                        rec.put("attendancePercent", Math.round(pct * 10) / 10.0);

                        String status = pct >= 85.0 || totalClasses == 0 ? "Good"
                                        : (pct >= 75.0 ? "Warning" : "Critical");
                        String statusClass = pct >= 85.0 || totalClasses == 0 ? "status-ready"
                                        : (pct >= 75.0 ? "status-processing" : "status-maintenance");
                        rec.put("status", status);
                        rec.put("statusClass", statusClass);

                        if (attendanceRecords.size() < 50) {
                                attendanceRecords.add(rec);
                        }
                }

                double avgAttendance = countWithData > 0 ? totalPctSum / countWithData : 0.0;

                response.put("stats", Map.of(
                                "avgAttendance", Math.round(avgAttendance * 10) / 10.0,
                                "avgAttendanceDelta", 0.0,
                                "perfectAttendance", perfectAttendance,
                                "belowThreshold", belowThreshold,
                                "belowThresholdDelta", 0,
                                "unexcusedAbsences", unexcusedAbsences,
                                "unexcusedAbsencesDelta", 0));

                response.put("trendData", Arrays.asList(
                                Map.of("week", "Week 1", "rate", 85),
                                Map.of("week", "Week 2", "rate", 88),
                                Map.of("week", "Week 3", "rate", 86),
                                Map.of("week", "Week 4", "rate", 92))); // Need time series

                attendanceRecords.sort((a, b) -> Double.compare((Double) a.get("attendancePercent"),
                                (Double) b.get("attendancePercent")));

                response.put("attendanceRecords", attendanceRecords);
                return response;
        }

        public Map<String, Object> getResourceUtilization(String department) {
                Map<String, Object> response = new HashMap<>();

                List<com.academic.platform.model.ExamVenue> venues = examVenueRepository.findAll();
                long labCount = venues.stream()
                                .filter(v -> v.getExamType() != null && v.getExamType().toLowerCase().contains("lab"))
                                .count();
                long classroomCount = venues.size() - labCount;

                int totalSystems = venues.stream()
                                .filter(v -> v.getExamType() != null && v.getExamType().toLowerCase().contains("lab"))
                                .mapToInt(v -> v.getCapacity() != null ? v.getCapacity() : 0).sum();

                Map<String, Object> stats = new HashMap<>();
                stats.put("labCount", labCount);
                stats.put("labCountDelta", 0);
                stats.put("avgLabOccupancy", labCount > 0 ? 65.5 : 0);
                stats.put("avgLabOccupancyDelta", 0);
                stats.put("classroomCount", classroomCount);
                stats.put("classroomCountDelta", 0);
                stats.put("avgClassOccupancy", classroomCount > 0 ? 82 : 0);
                stats.put("labSystems", totalSystems);
                stats.put("labSystemsInUse", totalSystems > 0 ? (int) (totalSystems * 0.4) : 0);
                stats.put("systemsMaintenance", totalSystems > 0 ? (int) (totalSystems * 0.05) : 0);
                stats.put("systemsMaintenanceDelta", 0);
                stats.put("labsMaintenance", 0);
                stats.put("dailyCheckins", 0);
                stats.put("dailyCheckinsDelta", 0);
                response.put("stats", stats);

                Map<String, Object> utilization = new HashMap<>();
                utilization.put("labsAvg", 65);
                utilization.put("classesAvg", 85);
                utilization.put("unbooked", 15);
                utilization.put("studentsPct", 60);
                utilization.put("facultyPct", 25);
                utilization.put("othersPct", 15);
                utilization.put("studentsHrs", 12000);
                utilization.put("facultyHrs", 5000);
                utilization.put("othersHrs", 3000);
                response.put("utilization", utilization);

                List<Map<String, Object>> riskLabs = new ArrayList<>();
                for (com.academic.platform.model.ExamVenue venue : venues) {
                        if (venue.getExamType() != null && venue.getExamType().toLowerCase().contains("lab")
                                        && !venue.isAvailable()) {
                                Map<String, Object> lab = new HashMap<>();
                                lab.put("name", venue.getName());
                                lab.put("occupancyColor", "bg-orange-base");
                                lab.put("occupancy", 100);
                                lab.put("systems", venue.getCapacity());
                                lab.put("maintenance", 1);
                                lab.put("statusClass", "status-processing");
                                lab.put("status", "Maintenance");
                                riskLabs.add(lab);
                        }
                }
                response.put("riskLabs", riskLabs);

                return response;
        }
}
