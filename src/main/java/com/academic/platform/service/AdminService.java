package com.academic.platform.service;

import com.academic.platform.model.CollegeExpense;
import com.academic.platform.model.FeeRecord;
import com.academic.platform.model.Role;
import com.academic.platform.model.User;
import com.academic.platform.repository.AttendanceRepository;
import com.academic.platform.repository.CollegeExpenseRepository;
import com.academic.platform.repository.FeeRecordRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private FeeRecordRepository feeRecordRepository;

    @Autowired
    private CollegeExpenseRepository collegeExpenseRepository;

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
        List<String> studentGenders = userRepository.findByRole(Role.STUDENT).stream()
                .map(User::getGender)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(gender -> !gender.isBlank())
                .collect(Collectors.toList());

        long boys = studentGenders.stream()
                .filter(this::isMaleGender)
                .count();
        long girls = studentGenders.stream()
                .filter(this::isFemaleGender)
                .count();

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

        // 4. Earnings (real fee income vs recorded college expenses)
        stats.put("earningsData", buildEarningsChartData());

        return stats;
    }

    private List<Map<String, Object>> buildEarningsChartData() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(6);

        Map<YearMonth, Map<String, Object>> monthlyData = new LinkedHashMap<>();
        for (YearMonth month = startMonth; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
            Map<String, Object> point = new HashMap<>();
            point.put("name", month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            point.put("income", 0.0);
            point.put("expense", 0.0);
            monthlyData.put(month, point);
        }

        List<FeeRecord> feeRecords = feeRecordRepository.findAll();
        for (FeeRecord feeRecord : feeRecords) {
            if (feeRecord.getPaymentStatus() == null || !"PAID".equalsIgnoreCase(feeRecord.getPaymentStatus())) {
                continue;
            }

            if (feeRecord.getPaymentDate() == null) {
                continue;
            }

            YearMonth paymentMonth = YearMonth.from(feeRecord.getPaymentDate());
            if (paymentMonth.isBefore(startMonth) || paymentMonth.isAfter(currentMonth)) {
                continue;
            }

            Map<String, Object> point = monthlyData.get(paymentMonth);
            double currentIncome = ((Number) point.get("income")).doubleValue();
            double amount = feeRecord.getTotalAmount() != null ? feeRecord.getTotalAmount() : 0.0;
            point.put("income", currentIncome + amount);
        }

        List<CollegeExpense> expenses = collegeExpenseRepository.findAllByOrderByExpenseDateDesc();
        for (CollegeExpense expense : expenses) {
            if (expense.getExpenseDate() == null) {
                continue;
            }

            YearMonth expenseMonth = YearMonth.from(expense.getExpenseDate());
            if (expenseMonth.isBefore(startMonth) || expenseMonth.isAfter(currentMonth)) {
                continue;
            }

            Map<String, Object> point = monthlyData.get(expenseMonth);
            double currentExpense = ((Number) point.get("expense")).doubleValue();
            double amount = expense.getAmount() != null ? expense.getAmount() : 0.0;
            point.put("expense", currentExpense + amount);
        }

        return monthlyData.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean isMaleGender(String gender) {
        String normalized = gender.trim().toLowerCase(Locale.ENGLISH);
        return normalized.equals("male")
                || normalized.equals("m")
                || normalized.equals("boy")
                || normalized.equals("boys");
    }

    private boolean isFemaleGender(String gender) {
        String normalized = gender.trim().toLowerCase(Locale.ENGLISH);
        return normalized.equals("female")
                || normalized.equals("f")
                || normalized.equals("girl")
                || normalized.equals("girls");
    }
}
