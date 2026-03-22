package com.academic.platform.service;

import com.academic.platform.model.Attendance;
import com.academic.platform.model.User;
import com.academic.platform.repository.AttendanceRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemSettingService systemSettingService;

    public Attendance markAttendance(String studentUid) {
        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (attendanceRepository.findByStudentAndDate(student, LocalDate.now()).isPresent()) {
            throw new RuntimeException("Attendance already marked for today.");
        }

        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setDate(LocalDate.now());
        attendance.setCheckInTime(LocalTime.now());
        // Simple logic: Late if after 10:00 AM
        if (LocalTime.now().isAfter(LocalTime.of(10, 0))) {
            attendance.setStatus("LATE");
        } else {
            attendance.setStatus("PRESENT");
        }

        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getStudentAttendance(String studentUid) {
        return attendanceRepository.findByStudentFirebaseUidOrderByDateDesc(studentUid);
    }

    public boolean isAttendanceMarkedToday(String studentUid) {
        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return attendanceRepository.findByStudentAndDate(student, LocalDate.now()).isPresent();
    }

    public List<Attendance> getMenteesAttendance(String mentorUid, LocalDate date) {
        return attendanceRepository.findByStudentStudentDetails_Mentor_FirebaseUidAndDate(mentorUid,
                date != null ? date : LocalDate.now());
    }

    public Map<String, Object> getStudentStats(String studentUid) {
        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        LocalDate today = LocalDate.now();
        LocalDate configuredSemesterStartDate = getSemesterStartDate();
        final LocalDate semesterStartDate = (configuredSemesterStartDate == null || configuredSemesterStartDate.isAfter(today))
                ? today
                : configuredSemesterStartDate;
        List<Attendance> history = attendanceRepository.findByStudentFirebaseUidOrderByDateDesc(studentUid);
        Set<LocalDate> presentDates = history.stream()
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus()))
                .map(Attendance::getDate)
                .filter(date -> date != null && !date.isBefore(semesterStartDate) && !date.isAfter(today))
                .collect(Collectors.toSet());
        int presentDays = presentDates.size();
        long workingDays = calculateWorkingDays(semesterStartDate, today);

        if (workingDays == 0)
            workingDays = 1; // avoid div/0

        double percentage = (double) presentDays / workingDays * 100.0;
        if (percentage > 100.0)
            percentage = 100.0; // edge case if extra classes/Saturdays attended

        Map<String, Object> stats = new HashMap<>();
        stats.put("present", presentDays);
        stats.put("total", workingDays);
        stats.put("percentage", Math.round(percentage * 10.0) / 10.0);
        stats.put("semesterStartDate", semesterStartDate.toString());

        return stats;
    }

    private LocalDate getSemesterStartDate() {
        String configuredDate = systemSettingService.getSetting("policy.attendance.semesterStartDate");
        if (configuredDate == null || configuredDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(configuredDate);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private long calculateWorkingDays(LocalDate start, LocalDate end) {
        if (start.isAfter(end))
            return 0;
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        long workingDays = 0;
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays++;
            }
        }
        return workingDays;
    }
}
