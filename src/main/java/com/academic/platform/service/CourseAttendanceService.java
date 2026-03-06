package com.academic.platform.service;

import com.academic.platform.model.CourseAttendance;
import com.academic.platform.model.CourseAttendanceSession;
import com.academic.platform.model.Section;
import com.academic.platform.model.User;
import com.academic.platform.repository.CourseAttendanceRepository;
import com.academic.platform.repository.CourseAttendanceSessionRepository;
import com.academic.platform.repository.SectionRepository;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.repository.EnrollmentRepository;
import com.academic.platform.repository.AcademicScheduleRepository;
import com.academic.platform.model.AcademicSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class CourseAttendanceService {

    @Autowired
    private CourseAttendanceSessionRepository sessionRepo;

    @Autowired
    private CourseAttendanceRepository attendanceRepo;

    @Autowired
    private SectionRepository sectionRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private EnrollmentRepository enrollmentRepo;

    @Autowired
    private AcademicScheduleRepository scheduleRepo;

    private static final int OTP_VALIDITY_MINUTES = 2;

    public CourseAttendanceSession generateOtp(Long sectionId, String facultyUid) {
        Section section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        if (!section.getFaculty().getFirebaseUid().equals(facultyUid)) {
            throw new RuntimeException("Unauthorized: Only the faculty of this section can generate OTP.");
        }

        // Deactivate existing active sessions for this section
        List<CourseAttendanceSession> history = sessionRepo.findBySectionIdOrderByCreatedAtDesc(sectionId);
        for (CourseAttendanceSession s : history) {
            if (s.isActive()) {
                s.setActive(false);
                sessionRepo.save(s);
            }
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Auto-match timetable schedule for today and the section course name
        AcademicSchedule matchedSchedule = scheduleRepo
                .findByDateAndSubjectNameIgnoreCase(LocalDate.now(), section.getCourse().getName()).stream().findFirst()
                .orElse(null);

        CourseAttendanceSession newSession = CourseAttendanceSession.builder()
                .section(section)
                .schedule(matchedSchedule)
                .otp(otp)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES))
                .active(true)
                .build();

        return sessionRepo.save(newSession);
    }

    public CourseAttendanceSession deactivateSession(Long sessionId, String facultyUid) {
        CourseAttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getSection().getFaculty().getFirebaseUid().equals(facultyUid)) {
            throw new RuntimeException("Unauthorized: Only the faculty of this section can deactivate the session.");
        }

        session.setActive(false);
        return sessionRepo.save(session);
    }

    public CourseAttendance markAttendance(String otp, String studentUid) {
        CourseAttendanceSession session = sessionRepo.findFirstByOtpAndActiveTrue(otp)
                .orElseThrow(() -> new RuntimeException("Invalid or inactive OTP"));

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            session.setActive(false);
            sessionRepo.save(session);
            throw new RuntimeException("OTP has expired");
        }

        User student = userRepo.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if student is enrolled in this section
        boolean isEnrolled = enrollmentRepo.findByStudent(student).stream()
                .anyMatch(e -> e.getSection().getId().equals(session.getSection().getId()));

        if (!isEnrolled) {
            throw new RuntimeException("Student is not enrolled in this course section");
        }

        LocalDateTime todayStart = LocalDateTime.now().with(java.time.LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.now().with(java.time.LocalTime.MAX);
        boolean alreadyMarkedToday = attendanceRepo
                .findBySessionSectionIdAndStudentId(session.getSection().getId(), student.getId()).stream()
                .anyMatch(a -> a.getMarkedAt().isAfter(todayStart) && a.getMarkedAt().isBefore(todayEnd));

        if (alreadyMarkedToday) {
            throw new RuntimeException("Attendance already marked for today");
        }

        CourseAttendance attendance = CourseAttendance.builder()
                .session(session)
                .student(student)
                .markedAt(LocalDateTime.now())
                .status("P")
                .build();

        return attendanceRepo.save(attendance);
    }

    public List<CourseAttendance> getSessionAttendances(Long sessionId) {
        return attendanceRepo.findBySessionId(sessionId);
    }

    public List<CourseAttendanceSession> getSectionSessions(Long sectionId) {
        return sessionRepo.findBySectionIdOrderByCreatedAtDesc(sectionId);
    }

    public CourseAttendanceSession getActiveSession(Long sectionId) {
        return sessionRepo.findBySectionIdOrderByCreatedAtDesc(sectionId).stream()
                .filter(CourseAttendanceSession::isActive)
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElse(null);
    }

    public List<CourseAttendance> getStudentAttendanceForSection(Long sectionId, Long studentId) {
        return attendanceRepo.findBySessionSectionIdAndStudentId(sectionId, studentId);
    }

    public List<CourseAttendanceSession> getSessionsByDate(Long sectionId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return sessionRepo.findBySectionIdAndCreatedAtBetweenOrderByCreatedAtDesc(sectionId, start, end);
    }

    public List<CourseAttendance> getPresentStudentsForSession(Long sessionId) {
        return attendanceRepo.findBySessionId(sessionId);
    }

    public CourseAttendanceSession saveBulkAttendance(Long sectionId, String facultyUid,
            java.util.List<java.util.Map<String, String>> attendanceList) {
        Section section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        if (!section.getFaculty().getFirebaseUid().equals(facultyUid)) {
            throw new RuntimeException("Unauthorized: Only the faculty of this section can save attendance.");
        }

        // Deactivate existing active sessions
        List<CourseAttendanceSession> history = sessionRepo.findBySectionIdOrderByCreatedAtDesc(sectionId);
        for (CourseAttendanceSession s : history) {
            if (s.isActive()) {
                s.setActive(false);
                sessionRepo.save(s);
            }
        }

        // Auto-match timetable schedule for today and the section course name
        AcademicSchedule matchedSchedule = scheduleRepo
                .findByDateAndSubjectNameIgnoreCase(LocalDate.now(), section.getCourse().getName()).stream().findFirst()
                .orElse(null);

        // Create a new inactive session for manual entry
        CourseAttendanceSession newSession = CourseAttendanceSession.builder()
                .section(section)
                .schedule(matchedSchedule)
                .otp("MANUAL")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now())
                .active(false)
                .build();
        newSession = sessionRepo.save(newSession);

        LocalDateTime todayStart = LocalDateTime.now().with(java.time.LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.now().with(java.time.LocalTime.MAX);

        for (java.util.Map<String, String> entry : attendanceList) {
            Long studentId = Long.parseLong(entry.get("studentId"));
            String status = entry.get("status");

            User student = userRepo.findById(studentId).orElse(null);
            if (student != null) {
                CourseAttendance existingToday = attendanceRepo.findBySessionSectionIdAndStudentId(sectionId, studentId)
                        .stream()
                        .filter(a -> a.getMarkedAt().isAfter(todayStart) && a.getMarkedAt().isBefore(todayEnd))
                        .findFirst()
                        .orElse(null);

                if (existingToday != null) {
                    existingToday.setStatus(status);
                    existingToday.setSession(newSession);
                    existingToday.setMarkedAt(LocalDateTime.now());
                    attendanceRepo.save(existingToday);
                } else {
                    CourseAttendance attendance = CourseAttendance.builder()
                            .session(newSession)
                            .student(student)
                            .markedAt(LocalDateTime.now())
                            .status(status)
                            .build();
                    attendanceRepo.save(attendance);
                }
            }
        }

        return newSession;
    }
}
