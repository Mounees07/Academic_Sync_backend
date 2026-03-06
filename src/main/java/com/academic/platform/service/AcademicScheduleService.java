package com.academic.platform.service;

import com.academic.platform.model.AcademicSchedule;
import com.academic.platform.model.User;
import com.academic.platform.repository.AcademicScheduleRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AcademicScheduleService {

    @Autowired
    private AcademicScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;

    public List<AcademicSchedule> getAllUpcomingSchedules() {
        // Fetch schedules from 30 days ago up to future to ensure current visibility
        LocalDate cutoff = LocalDate.now().minusDays(30);
        List<AcademicSchedule> list = scheduleRepository.findByDateAfterOrderByDateAsc(cutoff);
        System.out.println("Fetching schedules after " + cutoff + ": Found " + list.size());
        return list;
    }

    public List<AcademicSchedule> searchSchedules(LocalDate date, String subjectName) {
        if (subjectName != null && !subjectName.isEmpty()) {
            return scheduleRepository.findByDateAndSubjectNameIgnoreCase(date, subjectName);
        }
        return scheduleRepository.findByDate(date);
    }

    private void validateAccess(User modifier, AcademicSchedule.ScheduleType type) {
        String role = modifier.getRole().name();
        boolean isCOE = "COE".equals(role);
        boolean isHOD = "HOD".equals(role);
        boolean isAdmin = "ADMIN".equals(role);

        if (!isCOE && !isHOD && !isAdmin) {
            throw new RuntimeException("Unauthorized: Only COE, HOD, or ADMIN can manage schedules.");
        }

        if (type == null) {
            return; // Only role check required
        }

        if (!isAdmin) {
            if (isCOE) {
                if (type != AcademicSchedule.ScheduleType.INTERNAL_EXAM
                        && type != AcademicSchedule.ScheduleType.SEMESTER_EXAM) {
                    throw new RuntimeException("COE can only manage INTERNAL_EXAM or SEMESTER_EXAM.");
                }
            } else { // HOD
                if (type == AcademicSchedule.ScheduleType.INTERNAL_EXAM
                        || type == AcademicSchedule.ScheduleType.SEMESTER_EXAM) {
                    throw new RuntimeException("HOD cannot manage Exam schedules. Please contact COE.");
                }
            }
        }
    }

    public AcademicSchedule updateSchedule(Long id, AcademicSchedule updated, String modifierUid) {
        AcademicSchedule existing = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + id));

        User modifier = userRepository.findByFirebaseUid(modifierUid)
                .orElseThrow(() -> new RuntimeException("User not found: " + modifierUid));

        // Validate permission for the EXISTING type (to ensure they can edit this item)
        // AND the NEW type (to ensure they don't escalate)
        validateAccess(modifier, existing.getType());
        if (existing.getType() != updated.getType()) {
            validateAccess(modifier, updated.getType());
        }

        if (updated.getTitle() != null)
            existing.setTitle(updated.getTitle());
        if (updated.getType() != null)
            existing.setType(updated.getType());
        if (updated.getDate() != null)
            existing.setDate(updated.getDate());
        if (updated.getSession() != null)
            existing.setSession(updated.getSession());
        if (updated.getStartTime() != null)
            existing.setStartTime(updated.getStartTime());
        if (updated.getEndTime() != null)
            existing.setEndTime(updated.getEndTime());
        if (updated.getSubjectName() != null)
            existing.setSubjectName(updated.getSubjectName());
        if (updated.getDescription() != null)
            existing.setDescription(updated.getDescription());
        if (updated.getLocation() != null)
            existing.setLocation(updated.getLocation());

        return scheduleRepository.save(existing);
    }

    public void deleteSchedule(Long id, String modifierUid) {
        AcademicSchedule existing = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + id));

        User modifier = userRepository.findByFirebaseUid(modifierUid)
                .orElseThrow(() -> new RuntimeException("User not found: " + modifierUid));

        validateAccess(modifier, existing.getType());

        scheduleRepository.delete(existing);
    }

    @org.springframework.transaction.annotation.Transactional
    public List<AcademicSchedule> processBulkUpload(MultipartFile file, String hodUid) {
        System.out.println("Processing upload for HOD/COE: " + hodUid);
        User uploader = userRepository.findByFirebaseUid(hodUid)
                .orElseThrow(() -> new RuntimeException("Uploader not found with UID: " + hodUid));

        // Initial Role Check (Generic)
        validateAccess(uploader, null);
        // Note: passing null to just check role presence, but logic inside checks type
        // if !isAdmin.
        // We need to refactor validateAccess to handle null type or do specific check
        // here?
        // Actually, validateAccess throws if type is mismatched.
        // Let's keep logic inside loop for type specific check.
        // Or better: overload `validateAccess` or just check basic role here.

        // Let's just use the strict check inside the loop for each item.
        String role = uploader.getRole().name();
        boolean isAdmin = "ADMIN".equals(role);

        List<AcademicSchedule> schedules = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                // ... (lines 61-90 unchanged) ...
                if (lineNum == 0 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                lineNum++;
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                if (line.trim().isEmpty())
                    continue;

                try {
                    String[] data = line.split(",", -1);
                    if (data.length < 3 && line.contains(";")) {
                        data = line.split(";", -1);
                    }

                    if (data.length < 3) {
                        errors.add("Line " + lineNum + ": Insufficient columns.");
                        continue;
                    }

                    for (int i = 0; i < data.length; i++)
                        data[i] = data[i].trim();

                    AcademicSchedule schedule = new AcademicSchedule();
                    schedule.setTitle(data[0]);

                    AcademicSchedule.ScheduleType type;
                    try {
                        String typeStr = data[1].toUpperCase().replace(" ", "_");
                        type = AcademicSchedule.ScheduleType.valueOf(typeStr);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid ScheduleType '" + data[1] + "'.");
                    }

                    // Strict Check via Helper
                    try {
                        validateAccess(uploader, type);
                    } catch (RuntimeException e) {
                        throw new IllegalArgumentException(e.getMessage());
                    }

                    schedule.setType(type);
                    // ... (rest of parsing) ...
                    try {
                        schedule.setDate(parseDate(data[2]));
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid Date '" + data[2] + "'.");
                    }

                    if (data.length > 3)
                        schedule.setSession(data[3]);

                    if (data.length > 4 && !data[4].isEmpty()) {
                        try {
                            schedule.setStartTime(parseTime(data[4]));
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid StartTime '" + data[4] + "'.");
                        }
                    }

                    if (data.length > 5 && !data[5].isEmpty()) {
                        try {
                            schedule.setEndTime(parseTime(data[5]));
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid EndTime '" + data[5] + "'.");
                        }
                    }

                    if (data.length > 6)
                        schedule.setSubjectName(data[6]);
                    if (data.length > 7)
                        schedule.setDescription(data[7]);

                    schedule.setDepartment(uploader.getStudentDetails().getDepartment() != null
                            ? uploader.getStudentDetails().getDepartment()
                            : "General");
                    schedules.add(schedule);

                } catch (Exception e) {
                    System.err.println("Error processing line " + lineNum + ": " + e.getMessage());
                    errors.add("Line " + lineNum + ": " + e.getMessage());
                }
            }

            // ... (save block) ...
            if (!errors.isEmpty()) {
                throw new RuntimeException("Validation Failed: " + String.join("; ", errors));
            }

            if (schedules.isEmpty()) {
                throw new RuntimeException("File appears to be empty or contains only a header.");
            }

            System.out.println("Validation successful. Saving " + schedules.size() + " schedules.");
            return scheduleRepository.saveAll(schedules);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Processing failed: " + e.getMessage());
        }
    }

    private LocalTime parseTime(String timeStr) {
        // Normalize whitespace
        timeStr = timeStr.trim();

        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:mm:ss"),
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("h:mm a"), // 1:30 PM
                DateTimeFormatter.ofPattern("hh:mm a"), // 01:30 PM
                DateTimeFormatter.ofPattern("h:mm:ss a"), // 1:30:00 PM
                DateTimeFormatter.ofPattern("hh:mm:ss a"), // 01:30:00 PM
                DateTimeFormatter.ofPattern("h:mma"), // 1:30PM
                DateTimeFormatter.ofPattern("hh:mma") // 01:30PM
        );

        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalTime.parse(timeStr.toUpperCase(), fmt);
            } catch (DateTimeParseException ignored) {
            }
        }

        // Manual fallback for single digit hours without leading zero if formats fail
        // e.g. "9:30" handled by H:mm
        throw new IllegalArgumentException("Invalid time format: " + timeStr);
    }

    private LocalDate parseDate(String dateStr) {
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ISO_LOCAL_DATE, // yyyy-MM-dd
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d.M.yyyy"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yy"),
                DateTimeFormatter.ofPattern("dd-MM-yy"),
                DateTimeFormatter.ofPattern("d/M/yy"),
                DateTimeFormatter.ofPattern("dd/MM/yy"),
                DateTimeFormatter.ofPattern("d.M.yy"),
                DateTimeFormatter.ofPattern("dd.MM.yy"),
                DateTimeFormatter.ofPattern("M/d/yy"),
                DateTimeFormatter.ofPattern("MM/dd/yy"));

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Invalid date format: " + dateStr);
    }

    public List<AcademicSchedule> saveSchedules(List<AcademicSchedule> schedules, String hodUid) {
        User hod = userRepository.findByFirebaseUid(hodUid)
                .orElseThrow(() -> new RuntimeException("HOD not found"));

        schedules.forEach(s -> s.setDepartment(hod.getStudentDetails().getDepartment()));
        return scheduleRepository.saveAll(schedules);
    }

    public List<AcademicSchedule> getRecentSchedules() {
        return scheduleRepository.findTop20ByOrderByIdDesc();
    }
}
