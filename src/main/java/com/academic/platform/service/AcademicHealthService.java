package com.academic.platform.service;

import com.academic.platform.model.*;
import com.academic.platform.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Aggregates attendance, fees, internal marks to compute
 * the Academic Health Score (0-100) for a student.
 */
@Service
public class AcademicHealthService {

    @Autowired
    private SubjectAttendanceRepository subjectAttendanceRepo;

    @Autowired
    private FeeTransactionRepository feeTransactionRepo;

    @Autowired
    private InternalMarkRepository internalMarkRepo;

    @Autowired
    private UserRepository userRepository;

    public Map<String, Object> getHealthCard(String studentUid) {
        Map<String, Object> health = new LinkedHashMap<>();

        // 1. Attendance risk
        List<SubjectAttendance> attRecords = subjectAttendanceRepo.findByStudentFirebaseUid(studentUid);
        double avgAttendance = attRecords.isEmpty() ? 0 :
                attRecords.stream().mapToDouble(SubjectAttendance::getPercentage).average().orElse(0);
        String attRisk = avgAttendance >= 80 ? "LOW" : avgAttendance >= 75 ? "MEDIUM" : "HIGH";
        double attScore = Math.min(100, avgAttendance); // out of 100

        // 2. Fee status
        List<FeeTransaction> txns = feeTransactionRepo.findByStudentFirebaseUidOrderByCreatedAtDesc(studentUid);
        boolean hasPendingFees = txns.isEmpty() || txns.stream().anyMatch(t -> "PENDING".equals(t.getStatus()));
        String feeStatus = hasPendingFees ? "PENDING" : "PAID";
        double feeScore = hasPendingFees ? 50 : 100;

        // 3. Internal performance
        List<InternalMark> marks = internalMarkRepo.findByStudentFirebaseUidOrderBySubjectName(studentUid);
        double avgPerformance = marks.isEmpty() ? 0 :
                marks.stream().mapToDouble(InternalMark::getPercentageScore).average().orElse(0);
        String performanceStatus = avgPerformance >= 75 ? "EXCELLENT" : avgPerformance >= 60 ? "GOOD" : avgPerformance >= 40 ? "AVERAGE" : "POOR";
        double performanceScore = avgPerformance;

        // Overall health score: weighted avg
        // Attendance 40% + Performance 40% + Fees 20%
        double overallScore = (attScore * 0.40) + (performanceScore * 0.40) + (feeScore * 0.20);
        overallScore = Math.round(overallScore * 100.0) / 100.0;

        String grade;
        if (overallScore >= 85) grade = "A";
        else if (overallScore >= 70) grade = "B";
        else if (overallScore >= 55) grade = "C";
        else if (overallScore >= 40) grade = "D";
        else grade = "F";

        // Alerts
        List<String> alerts = new ArrayList<>();
        if ("HIGH".equals(attRisk)) alerts.add("Attendance is critically low. Risk of detention.");
        if (hasPendingFees) alerts.add("Semester fee payment is pending.");
        if ("POOR".equals(performanceStatus)) alerts.add("Internal marks are below passing threshold.");

        health.put("attendanceRisk", attRisk);
        health.put("attendanceScore", Math.round(attScore * 100.0) / 100.0);
        health.put("avgAttendance", Math.round(avgAttendance * 100.0) / 100.0);
        health.put("feeStatus", feeStatus);
        health.put("feeScore", feeScore);
        health.put("performanceStatus", performanceStatus);
        health.put("performanceScore", Math.round(performanceScore * 100.0) / 100.0);
        health.put("overallHealthScore", overallScore);
        health.put("grade", grade);
        health.put("alerts", alerts);

        return health;
    }
}
