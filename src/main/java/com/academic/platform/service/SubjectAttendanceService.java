package com.academic.platform.service;

import com.academic.platform.model.SubjectAttendance;
import com.academic.platform.model.User;
import com.academic.platform.model.Section;
import com.academic.platform.repository.SubjectAttendanceRepository;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubjectAttendanceService {

    @Autowired
    private SubjectAttendanceRepository subjectAttendanceRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private com.academic.platform.repository.CourseAttendanceRepository courseAttendanceRepo;

    public List<Map<String, Object>> getStudentSubjectAttendance(String studentUid) {
        List<com.academic.platform.model.CourseAttendance> atts = courseAttendanceRepo.findByStudentFirebaseUidOrderByMarkedAtDesc(studentUid);
        // Group by course
        Map<com.academic.platform.model.Course, List<com.academic.platform.model.CourseAttendance>> byCourse = atts.stream()
            .collect(Collectors.groupingBy(a -> a.getSession().getSection().getCourse()));

        List<Map<String, Object>> response = new ArrayList<>();
        for (Map.Entry<com.academic.platform.model.Course, List<com.academic.platform.model.CourseAttendance>> entry : byCourse.entrySet()) {
            com.academic.platform.model.Course course = entry.getKey();
            List<com.academic.platform.model.CourseAttendance> courseAtts = entry.getValue();
            
            int totalClasses = courseAtts.size();
            int attended = (int) courseAtts.stream().filter(c -> "PRESENT".equalsIgnoreCase(c.getStatus())).count();
            double pct = totalClasses > 0 ? (attended * 100.0 / totalClasses) : 0;
            
            Map<String, Object> m = new HashMap<>();
            m.put("id", course.getId());
            m.put("subjectName", course.getName());
            m.put("subjectCode", course.getCode());
            m.put("totalClasses", totalClasses);
            m.put("attendedClasses", attended);
            m.put("percentage", Math.round(pct * 10.0) / 10.0);
            
            String colorStatus = pct >= 80 ? "green" : (pct >= 75 ? "orange" : "red");
            m.put("colorStatus", colorStatus);
            response.add(m);
        }
        return response;
    }

    /** Get subject attendance for a specific month/year */
    public List<Map<String, Object>> getMonthlyAttendance(String studentUid, int month, int year) {
        List<SubjectAttendance> records = subjectAttendanceRepo
                .findByStudentFirebaseUidAndMonthAndYear(studentUid, month, year);
        return buildResponse(records);
    }

    /** Upsert a subject attendance record (for teacher/admin) */
    public SubjectAttendance upsertAttendance(String studentUid, Long sectionId,
                                               String subjectName, String subjectCode,
                                               int month, int year,
                                               int totalClasses, int attendedClasses) {
        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentUid));
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found: " + sectionId));

        SubjectAttendance sa = subjectAttendanceRepo
                .findByStudentFirebaseUidAndSectionIdAndMonthAndYear(studentUid, sectionId, month, year)
                .orElse(SubjectAttendance.builder()
                        .student(student)
                        .section(section)
                        .subjectName(subjectName)
                        .subjectCode(subjectCode)
                        .month(month)
                        .year(year)
                        .build());

        sa.setTotalClasses(totalClasses);
        sa.setAttendedClasses(attendedClasses);
        if (subjectName != null) sa.setSubjectName(subjectName);
        if (subjectCode != null) sa.setSubjectCode(subjectCode);

        return subjectAttendanceRepo.save(sa);
    }

    /** Attendance prediction: if rate continues, what is expected final attendance? */
    public Map<String, Object> getAttendancePrediction(String studentUid) {
        List<com.academic.platform.model.CourseAttendance> atts = courseAttendanceRepo.findByStudentFirebaseUidOrderByMarkedAtDesc(studentUid);
        Map<String, Object> result = new HashMap<>();

        int currentMonth = LocalDate.now().getMonthValue();
        int totalMonths = 6; // typical semester

        // Group by subject name
        Map<String, List<com.academic.platform.model.CourseAttendance>> bySubject = atts.stream()
                .collect(Collectors.groupingBy(a -> a.getSession().getSection().getCourse().getName()));

        List<Map<String, Object>> subjectPredictions = new ArrayList<>();

        for (Map.Entry<String, List<com.academic.platform.model.CourseAttendance>> entry : bySubject.entrySet()) {
            List<com.academic.platform.model.CourseAttendance> subRecords = entry.getValue();
            int totalAttended = (int) subRecords.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
            int totalClasses = subRecords.size();

            double currentPct = totalClasses > 0 ? (totalAttended * 100.0 / totalClasses) : 0;

            // Simple linear projection: assume same rate continues
            Set<Integer> distinctMonths = subRecords.stream().map(a -> a.getMarkedAt().getMonthValue()).collect(Collectors.toSet());
            int monthsElapsed = Math.max(1, distinctMonths.size());
            int monthsRemaining = Math.max(0, totalMonths - monthsElapsed);
            double avgClassesPerMonth = (double) totalClasses / monthsElapsed;
            int projectedFutureClasses = (int) (avgClassesPerMonth * monthsRemaining);
            double projectedAttended = totalAttended + (projectedFutureClasses * currentPct / 100.0);
            int projectedTotal = totalClasses + projectedFutureClasses;
            double expectedFinalPct = projectedTotal > 0 ? Math.round((projectedAttended / projectedTotal) * 100.0) / 100.0 : 0;

            Map<String, Object> subPred = new HashMap<>();
            subPred.put("subjectName", entry.getKey());
            subPred.put("currentPercentage", Math.round(currentPct * 10.0) / 10.0);
            subPred.put("expectedFinalPercentage", expectedFinalPct);
            subPred.put("riskWarning", currentPct < 75);
            subPred.put("atRisk", expectedFinalPct < 75);
            subjectPredictions.add(subPred);
        }

        result.put("predictions", subjectPredictions);
        result.put("semesterMonth", currentMonth);
        return result;
    }

    /** Analytics: trend indicator (Improving / Declining / Stable) */
    public Map<String, Object> getAttendanceAnalytics(String studentUid) {
        List<com.academic.platform.model.CourseAttendance> atts = courseAttendanceRepo.findByStudentFirebaseUidOrderByMarkedAtDesc(studentUid);

        // Group by month
        Map<Integer, List<com.academic.platform.model.CourseAttendance>> byMonth = atts.stream()
                .filter(a -> a.getMarkedAt().getYear() == LocalDate.now().getYear())
                .collect(Collectors.groupingBy(a -> a.getMarkedAt().getMonthValue()));

        List<Map<String, Object>> monthlyData = new ArrayList<>();
        List<Integer> sortedMonths = new ArrayList<>(byMonth.keySet());
        Collections.sort(sortedMonths);

        for (int month : sortedMonths) {
            List<com.academic.platform.model.CourseAttendance> mRecords = byMonth.get(month);
            int total = mRecords.size();
            int attended = (int) mRecords.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
            double pct = total > 0 ? Math.round((attended * 100.0 / total) * 100.0) / 100.0 : 0;

            Map<String, Object> m = new HashMap<>();
            m.put("month", getMonthName(month));
            m.put("monthNumber", month);
            m.put("attended", attended);
            m.put("total", total);
            m.put("percentage", pct);
            monthlyData.add(m);
        }

        // Trend analysis (compare last 2 months)
        String trend = "STABLE";
        if (monthlyData.size() >= 2) {
            double recent = (double) monthlyData.get(monthlyData.size() - 1).get("percentage");
            double prev = (double) monthlyData.get(monthlyData.size() - 2).get("percentage");
            if (recent > prev + 2) trend = "IMPROVING";
            else if (recent < prev - 2) trend = "DECLINING";
        }

        // Late vs On-time from biometric (placeholder — merge with attendance module)
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("monthlyData", monthlyData);
        analytics.put("trend", trend);
        analytics.put("totalRecords", atts.size());

        return analytics;
    }

    private String getMonthName(int month) {
        String[] names = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        return (month >= 1 && month <= 12) ? names[month - 1] : "?";
    }

    private List<Map<String, Object>> buildResponse(List<SubjectAttendance> records) {
        return records.stream().map(sa -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", sa.getId());
            m.put("subjectName", sa.getSubjectName());
            m.put("subjectCode", sa.getSubjectCode());
            m.put("month", sa.getMonth());
            m.put("year", sa.getYear());
            m.put("totalClasses", sa.getTotalClasses());
            m.put("attendedClasses", sa.getAttendedClasses());
            m.put("percentage", sa.getPercentage());
            m.put("colorStatus", sa.getColorStatus());
            return m;
        }).collect(Collectors.toList());
    }
}
