package com.academic.platform.service;

import com.academic.platform.model.Role;
import com.academic.platform.repository.AttendanceRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. User Counts
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        stats.put("totalStudents", totalStudents);
        stats.put("totalTeachers", userRepository.countByRoleIn(List.of(Role.TEACHER, Role.MENTOR, Role.HOD)));
        stats.put("totalStaff",
                userRepository.countByRoleIn(List.of(Role.ADMIN, Role.COE, Role.GATE_SECURITY, Role.PRINCIPAL)));
        stats.put("totalAwards", 0); // Mock data

        // 2. Gender Distribution (Students)
        // Note: Ideally enforce case-insensitivity on DB or clean data. For now
        // assuming standardized inputs.
        // Or fetch all students and filter in memory if volume is low, but count query
        // is better.
        // We will try simple counts. If gender is null or mixed case, this might need
        // refinement.
        long boys = userRepository.countByRoleAndGender(Role.STUDENT, "Male");
        long girls = userRepository.countByRoleAndGender(Role.STUDENT, "Female");

        // Fallback or detailed check: if pure counts are 0, maybe gender is stored
        // differently (e.g. M/F/male/female)
        // For robustness, we could just fetch all students and stream count, but let's
        // stick to repository for perf.

        stats.put("studentGenderData", List.of(
                Map.of("name", "Boys", "value", boys, "color", "#4D44B5"),
                Map.of("name", "Girls", "value", girls, "color", "#FCC43E")));

        // 3. Attendance Overview (Last 7 Days)
        LocalDate oneWeekAgo = LocalDate.now().minusDays(6); // Include today
        List<Object[]> attendanceData = attendanceRepository.findDailyAttendanceStats(oneWeekAgo);

        // Map data to chart format
        // We need to ensure we have entries for all days even if count is 0
        Map<LocalDate, Long> attendanceMap = attendanceData.stream()
                .collect(Collectors.toMap(
                        obj -> (LocalDate) obj[0],
                        obj -> (Long) obj[1]));

        List<Map<String, Object>> attendanceChart = oneWeekAgo.datesUntil(LocalDate.now().plusDays(1))
                .map(date -> {
                    Long presentCount = attendanceMap.getOrDefault(date, 0L);
                    long absent = totalStudents - presentCount;
                    if (absent < 0)
                        absent = 0;

                    Map<String, Object> dayStats = new HashMap<>();
                    dayStats.put("day", date.getDayOfWeek().toString().substring(0, 3)); // MON, TUE
                    dayStats.put("present", presentCount);
                    dayStats.put("absent", absent);
                    return dayStats;
                }).collect(Collectors.toList());

        stats.put("attendanceData", attendanceChart);

        // 4. Earnings (Mock - as this module likely doesn't exist)
        stats.put("earningsData", List.of(
                Map.of("name", "Jun", "income", 400, "expense", 240),
                Map.of("name", "Jul", "income", 300, "expense", 139),
                Map.of("name", "Aug", "income", 500, "expense", 280),
                Map.of("name", "Sep", "income", 200, "expense", 390),
                Map.of("name", "Oct", "income", 278, "expense", 190),
                Map.of("name", "Nov", "income", 189, "expense", 480),
                Map.of("name", "Dec", "income", 239, "expense", 380)));

        return stats;
    }
}
