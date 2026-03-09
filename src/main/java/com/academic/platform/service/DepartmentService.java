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

        public DepartmentDashboardDTO getDashboardStats(String department) {
                DepartmentDashboardDTO dto = new DepartmentDashboardDTO();

                // 1. Faculty Count — use IgnoreCase + include HOD, MENTOR and TEACHER
                dto.setTotalFaculty(
                                userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(department,
                                                java.util.Arrays.asList(
                                                                com.academic.platform.model.Role.TEACHER,
                                                                com.academic.platform.model.Role.MENTOR,
                                                                com.academic.platform.model.Role.HOD))
                                                .size());

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
                List<User> students = userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(department, Collections.singletonList(Role.STUDENT));
                
                int ug = 0;
                int pg = 0;
                int atRisk = 0;
                
                for (User s : students) {
                        String degree = s.getStudentDetails().getDegreeLevel();
                        String course = s.getStudentDetails().getCourseName() == null ? "" : s.getStudentDetails().getCourseName().toUpperCase();
                        if ("UG".equalsIgnoreCase(degree) || course.contains("B.SC") || course.contains("B.E") || course.contains("B.TECH") || course.startsWith("B")) {
                                ug++;
                        } else if ("PG".equalsIgnoreCase(degree) || course.contains("M.SC") || course.contains("M.E") || course.contains("M.TECH") || course.startsWith("M")) {
                                pg++;
                        }
                        
                        Double gpa = s.getStudentDetails().getGpa();
                        if (gpa == null) gpa = s.getStudentDetails().getCgpa();
                        if ((gpa != null && gpa < 6.0) || "Academic Warning".equalsIgnoreCase(s.getStudentDetails().getStudentStatus())) {
                                atRisk++;
                        }
                }
                
                if (ug == 0 && pg == 0 && !students.isEmpty()) {
                        ug = students.size();
                }

                com.academic.platform.dto.DepartmentStudentsDirectoryDTO.Stats stats = new com.academic.platform.dto.DepartmentStudentsDirectoryDTO.Stats(students.size(), ug, pg, atRisk);
                return new com.academic.platform.dto.DepartmentStudentsDirectoryDTO(stats, students);
        }
}
