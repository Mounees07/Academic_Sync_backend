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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Comparator;
import java.util.stream.Collectors;

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

    private boolean isPresentStatus(String status) {
        if (status == null) {
            return false;
        }
        String value = status.trim().toUpperCase();
        return value.equals("P") || value.equals("PRESENT") || value.equals("L") || value.equals("LATE");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isAttendanceEligibleType(AcademicSchedule schedule) {
        if (schedule == null || schedule.getType() == null) {
            return true;
        }
        return schedule.getType() == AcademicSchedule.ScheduleType.ACADEMIC
                || schedule.getType() == AcademicSchedule.ScheduleType.LAB_SLOT
                || schedule.getType() == AcademicSchedule.ScheduleType.SKILL_TRAINING;
    }

    private List<AcademicSchedule> findScheduleCandidates(Section section, LocalDate date) {
        String courseName = section.getCourse() != null ? normalize(section.getCourse().getName()) : "";
        String courseDepartment = section.getCourse() != null ? normalize(section.getCourse().getDepartment()) : "";

        return scheduleRepo.findByDateAndSubjectNameIgnoreCase(date, courseName).stream()
                .filter(this::isAttendanceEligibleType)
                .filter(schedule -> normalize(schedule.getSubjectName()).equalsIgnoreCase(courseName))
                .filter(schedule -> courseDepartment.isBlank()
                        || normalize(schedule.getDepartment()).isBlank()
                        || normalize(schedule.getDepartment()).equalsIgnoreCase(courseDepartment))
                .filter(schedule -> schedule.getStartTime() != null && schedule.getEndTime() != null)
                .sorted(Comparator.comparing(AcademicSchedule::getStartTime))
                .collect(Collectors.toList());
    }

    private boolean isWithinScheduleWindow(AcademicSchedule schedule, LocalDateTime dateTime) {
        if (schedule == null || schedule.getDate() == null || schedule.getStartTime() == null || schedule.getEndTime() == null) {
            return false;
        }
        LocalDate targetDate = dateTime.toLocalDate();
        LocalTime targetTime = dateTime.toLocalTime();
        return schedule.getDate().equals(targetDate)
                && !targetTime.isBefore(schedule.getStartTime())
                && !targetTime.isAfter(schedule.getEndTime());
    }

    private void ensureSessionWithinAllowedWindow(CourseAttendanceSession session) {
        if (session == null || !session.isActive()) {
            return;
        }

        boolean expired = session.getExpiresAt() == null || !session.getExpiresAt().isAfter(LocalDateTime.now());
        boolean outsideSchedule = session.getSchedule() == null || !isWithinScheduleWindow(session.getSchedule(), LocalDateTime.now());

        if (expired || outsideSchedule) {
            session.setActive(false);
            sessionRepo.save(session);
        }
    }

    private AcademicSchedule findCurrentScheduleSlot(Section section) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        return findScheduleCandidates(section, today).stream()
                .filter(schedule -> !now.isBefore(schedule.getStartTime()) && !now.isAfter(schedule.getEndTime()))
                .sorted(Comparator.comparing(AcademicSchedule::getStartTime))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Attendance can only be generated in the allocated academic schedule slot for this course."));
    }

    private void deactivateOtherActiveSessions(Long sectionId, Long keepSessionId) {
        List<CourseAttendanceSession> history = sessionRepo.findBySectionIdOrderByCreatedAtDesc(sectionId);
        for (CourseAttendanceSession session : history) {
            if (session.isActive() && (keepSessionId == null || !session.getId().equals(keepSessionId))) {
                session.setActive(false);
                sessionRepo.save(session);
            }
        }
    }

    public CourseAttendanceSession generateOtp(Long sectionId, String facultyUid) {
        Section section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        if (!section.getFaculty().getFirebaseUid().equals(facultyUid)) {
            throw new RuntimeException("Unauthorized: Only the faculty of this section can generate OTP.");
        }

        AcademicSchedule matchedSchedule = findCurrentScheduleSlot(section);

        List<CourseAttendanceSession> slotSessions = sessionRepo
                .findBySectionIdAndScheduleIdOrderByCreatedAtDesc(sectionId, matchedSchedule.getId());
        if (!slotSessions.isEmpty()) {
            CourseAttendanceSession latest = slotSessions.get(0);
            ensureSessionWithinAllowedWindow(latest);
            if (latest.isActive() && latest.getExpiresAt() != null && latest.getExpiresAt().isAfter(LocalDateTime.now())) {
                return latest;
            }
            if (latest.getCreatedAt() != null && latest.getCreatedAt().toLocalDate().equals(LocalDate.now())) {
                throw new RuntimeException("Attendance session already created for this allocated slot.");
            }
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        CourseAttendanceSession newSession = CourseAttendanceSession.builder()
                .section(section)
                .schedule(matchedSchedule)
                .otp(otp)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES))
                .active(true)
                .build();

        CourseAttendanceSession saved = sessionRepo.save(newSession);
        deactivateOtherActiveSessions(sectionId, saved.getId());
        return saved;
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

        ensureSessionWithinAllowedWindow(session);
        if (!session.isActive()) {
            throw new RuntimeException("Attendance is allowed only during the allotted class timing for this course.");
        }

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

        if (attendanceRepo.existsBySessionIdAndStudentId(session.getId(), student.getId())) {
            throw new RuntimeException("Attendance already marked for this session");
        }

        CourseAttendance attendance = CourseAttendance.builder()
                .session(session)
                .student(student)
                .markedAt(LocalDateTime.now())
                .status("PRESENT")
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
        List<CourseAttendanceSession> sessions = sessionRepo.findBySectionIdOrderByCreatedAtDesc(sectionId);
        for (CourseAttendanceSession session : sessions) {
            ensureSessionWithinAllowedWindow(session);
            if (session.isActive() && session.getExpiresAt() != null && session.getExpiresAt().isAfter(LocalDateTime.now())) {
                return session;
            }
        }
        return null;
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

        AcademicSchedule matchedSchedule = findCurrentScheduleSlot(section);
        List<CourseAttendanceSession> slotSessions = sessionRepo
                .findBySectionIdAndScheduleIdOrderByCreatedAtDesc(sectionId, matchedSchedule.getId());
        CourseAttendanceSession newSession;
        if (!slotSessions.isEmpty()) {
            newSession = slotSessions.get(0);
            newSession.setOtp("MANUAL");
            newSession.setActive(false);
            newSession.setExpiresAt(LocalDateTime.now());
            newSession = sessionRepo.save(newSession);
        } else {
            newSession = CourseAttendanceSession.builder()
                    .section(section)
                    .schedule(matchedSchedule)
                    .otp("MANUAL")
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now())
                    .active(false)
                    .build();
            newSession = sessionRepo.save(newSession);
        }
        deactivateOtherActiveSessions(sectionId, newSession.getId());

        for (java.util.Map<String, String> entry : attendanceList) {
            Long studentId = Long.parseLong(entry.get("studentId"));
            String status = entry.get("status");

            User student = userRepo.findById(studentId).orElse(null);
            if (student != null) {
                CourseAttendance existingToday = slotSessions.isEmpty()
                        ? null
                        : attendanceRepo.findBySessionId(newSession.getId()).stream()
                                .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(studentId))
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

    public Map<String, Object> getAttendanceWindowStatus(Long sectionId, String facultyUid) {
        Section section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        if (facultyUid != null && !facultyUid.isBlank()
                && !section.getFaculty().getFirebaseUid().equals(facultyUid)) {
            throw new RuntimeException("Unauthorized: Only the faculty of this section can view attendance status.");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<AcademicSchedule> todaySchedules = findScheduleCandidates(section, today);
        AcademicSchedule currentSchedule = todaySchedules.stream()
                .filter(schedule -> isWithinScheduleWindow(schedule, now))
                .findFirst()
                .orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("allowed", currentSchedule != null);
        response.put("currentSchedule", currentSchedule);
        response.put("todaysSchedules", todaySchedules);
        response.put("serverTime", now);

        if (currentSchedule != null) {
            response.put("message",
                    "OTP generation is enabled for " + normalize(currentSchedule.getSubjectName())
                            + " at " + normalize(currentSchedule.getLocation())
                            + " during " + currentSchedule.getStartTime() + " - " + currentSchedule.getEndTime() + ".");
            return response;
        }

        if (!todaySchedules.isEmpty()) {
            AcademicSchedule nextSchedule = todaySchedules.get(0);
            response.put("message",
                    "OTP generation is restricted outside the allotted slot. Today's mapped class runs "
                            + nextSchedule.getStartTime() + " - " + nextSchedule.getEndTime()
                            + " at " + normalize(nextSchedule.getLocation()) + ".");
        } else {
            response.put("message", "No academic schedule is mapped for this course today, so OTP generation is blocked.");
        }
        return response;
    }

    public List<Map<String, Object>> getStudentAttendanceTimeline(String studentUid) {
        List<CourseAttendance> attendances = attendanceRepo.findByStudentFirebaseUidOrderByMarkedAtDesc(studentUid);

        return attendances.stream().map(attendance -> {
            Map<String, Object> row = new LinkedHashMap<>();
            CourseAttendanceSession session = attendance.getSession();
            AcademicSchedule schedule = session != null ? session.getSchedule() : null;
            Section section = session != null ? session.getSection() : null;

            row.put("id", attendance.getId());
            row.put("status", attendance.getStatus());
            row.put("markedAt", attendance.getMarkedAt());
            row.put("courseName", section != null && section.getCourse() != null ? section.getCourse().getName() : "Unknown");
            row.put("courseCode", section != null && section.getCourse() != null ? section.getCourse().getCode() : null);
            row.put("sectionId", section != null ? section.getId() : null);
            row.put("date", schedule != null ? schedule.getDate() : attendance.getMarkedAt() != null ? attendance.getMarkedAt().toLocalDate() : null);
            row.put("session", schedule != null ? schedule.getSession() : null);
            row.put("startTime", schedule != null ? schedule.getStartTime() : null);
            row.put("endTime", schedule != null ? schedule.getEndTime() : null);
            row.put("venue", schedule != null ? schedule.getLocation() : null);
            return row;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMentorAttendanceOverview(String mentorUid, LocalDate date) {
        List<User> mentees = userRepo.findByStudentDetails_Mentor_FirebaseUid(mentorUid);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<Map<String, Object>> response = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (User mentee : mentees) {
            List<CourseAttendance> dayAttendances = attendanceRepo.findByStudentFirebaseUidOrderByMarkedAtDesc(mentee.getFirebaseUid())
                    .stream()
                    .filter(attendance -> {
                        AcademicSchedule schedule = attendance.getSession() != null ? attendance.getSession().getSchedule() : null;
                        LocalDate attendanceDate = schedule != null && schedule.getDate() != null
                                ? schedule.getDate()
                                : attendance.getMarkedAt() != null ? attendance.getMarkedAt().toLocalDate() : null;
                        return targetDate.equals(attendanceDate);
                    })
                    .sorted(Comparator.comparing(CourseAttendance::getMarkedAt).reversed())
                    .collect(Collectors.toList());

            for (CourseAttendance attendance : dayAttendances) {
                AcademicSchedule schedule = attendance.getSession() != null ? attendance.getSession().getSchedule() : null;
                Section section = attendance.getSession() != null ? attendance.getSession().getSection() : null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", attendance.getId());
                row.put("studentName", mentee.getFullName());
                row.put("studentEmail", mentee.getEmail());
                row.put("rollNumber", mentee.getStudentDetails() != null ? mentee.getStudentDetails().getRollNumber() : null);
                row.put("courseName", section != null && section.getCourse() != null ? section.getCourse().getName() : "Unknown");
                row.put("session", schedule != null ? schedule.getSession() : null);
                row.put("slot", schedule != null && schedule.getStartTime() != null && schedule.getEndTime() != null
                        ? schedule.getStartTime().format(timeFormatter) + " - " + schedule.getEndTime().format(timeFormatter)
                        : null);
                row.put("venue", schedule != null ? schedule.getLocation() : null);
                row.put("status", attendance.getStatus());
                row.put("markedAt", attendance.getMarkedAt());
                response.add(row);
            }
        }

        return response;
    }
}
