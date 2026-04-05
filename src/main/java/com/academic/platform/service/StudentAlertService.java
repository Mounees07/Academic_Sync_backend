package com.academic.platform.service;

import com.academic.platform.model.Role;
import com.academic.platform.model.AcademicSchedule;
import com.academic.platform.model.User;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StudentAlertService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public void notifySchedulePublished(String department, int scheduleCount, LocalDate earliestDate) {
        if (department == null || department.isBlank()) {
            return;
        }

        String title = "New Schedule Uploaded";
        String message = scheduleCount > 1
                ? "A new academic schedule has been uploaded for your department. Please review the latest timetable."
                : "A new academic schedule entry has been uploaded for your department. Please review the latest timetable.";

        String emailSubject = "Schedule Update for " + department;
        String htmlBody = buildStudentEmail(
                "Schedule Published",
                "A new academic schedule has been uploaded for your department.",
                List.of(
                        "Department: " + department,
                        "Items added: " + scheduleCount,
                        "Starts from: " + formatDate(earliestDate)
                ),
                "Open your schedule page to review the latest updates.",
                "/schedule"
        );

        notifyDepartmentStudents(department, title, message, "/schedule", "SCHEDULE_PUBLISHED", emailSubject, htmlBody);
    }

    public void notifySchedulePublished(List<AcademicSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return;
        }

        List<AcademicSchedule> validSchedules = schedules.stream()
                .filter(Objects::nonNull)
                .toList();
        if (validSchedules.isEmpty()) {
            return;
        }

        LocalDate earliestDate = validSchedules.stream()
                .map(AcademicSchedule::getDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        LinkedHashMap<Long, User> recipients = new LinkedHashMap<>();
        for (AcademicSchedule schedule : validSchedules) {
            for (User student : resolveStudentsForDepartment(schedule.getDepartment())) {
                recipients.putIfAbsent(student.getId(), student);
            }
        }

        int scheduleCount = validSchedules.size();
        String title = "New Schedule Uploaded";
        String message = scheduleCount > 1
                ? "A new academic schedule has been uploaded. Please review the latest timetable."
                : "A new academic schedule entry has been uploaded. Please review the latest timetable.";

        String departments = validSchedules.stream()
                .map(AcademicSchedule::getDepartment)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        String emailSubject = departments.isBlank()
                ? "New Schedule Published"
                : "Schedule Update for " + departments;
        String htmlBody = buildStudentEmail(
                "Schedule Published",
                "A new academic schedule has been uploaded for your academic portal view.",
                List.of(
                        "Departments: " + defaultString(departments, "All Students"),
                        "Items added: " + scheduleCount,
                        "Starts from: " + formatDate(earliestDate)
                ),
                "Open your schedule page to review the latest updates.",
                "/schedule"
        );

        for (User student : recipients.values()) {
            notifyStudent(student, "SCHEDULE_PUBLISHED", title, message, "/schedule", emailSubject, htmlBody);
        }
    }

    public void notifyResultPublished(User student, Integer semester) {
        if (student == null) {
            return;
        }

        String title = "Semester Results Published";
        String message = semester == null
                ? "Your latest semester results have been published. Open the portal to review your marks."
                : "Semester " + semester + " results have been published. Open the portal to review your marks.";

        String emailSubject = semester == null
                ? "Semester Results Published"
                : "Semester " + semester + " Results Published";
        String htmlBody = buildStudentEmail(
                "Results Published",
                "Your latest exam results are now available on the portal.",
                List.of(
                        "Student: " + defaultString(student.getFullName(), "Student"),
                        "Semester: " + (semester == null ? "Latest" : semester)
                ),
                "Log in to the website to view subject-wise marks and result details.",
                "/student/results"
        );

        notifyStudent(student, "RESULT_PUBLISHED", title, message, "/student/results", emailSubject, htmlBody);
    }

    public void notifyLeaveStatus(User student, String leaveType, String status, String remarks) {
        if (student == null) {
            return;
        }

        String normalizedStatus = defaultString(status, "UPDATED").toUpperCase();
        String title = "Leave Status Updated";
        String message = "Your " + defaultString(leaveType, "leave") + " request is now "
                + normalizedStatus.toLowerCase() + ". Open the portal to see the full update.";

        String emailSubject = "Leave Request " + normalizedStatus;
        String htmlBody = buildStudentEmail(
                "Leave Status Update",
                "Your leave request has been updated.",
                List.of(
                        "Leave type: " + defaultString(leaveType, "General Leave"),
                        "Status: " + normalizedStatus,
                        "Remarks: " + defaultString(remarks, "No additional remarks")
                ),
                "Visit your leave page on the website to see the complete details.",
                "/student/leaves"
        );

        notifyStudent(student, "LEAVE_UPDATE", title, message, "/student/leaves", emailSubject, htmlBody);
    }

    public void notifyPlacementDriveOpened(User student, String companyName, String roleTitle, String driveDate, String location) {
        if (student == null) {
            return;
        }

        String title = "Placement Registration Open";
        String message = "Registration is now open for " + companyName + " - " + roleTitle
                + ". Visit your placement page for details.";

        String emailSubject = "Placement Registration Open: " + companyName;
        String htmlBody = buildStudentEmail(
                "Placement Registration Open",
                "A new placement opportunity is available for you.",
                List.of(
                        "Company: " + defaultString(companyName, "Company"),
                        "Role: " + defaultString(roleTitle, "Role"),
                        "Drive date: " + defaultString(driveDate, "To be announced"),
                        "Location: " + defaultString(location, "To be announced")
                ),
                "Open the placement page on the portal to review eligibility and apply.",
                "/student/placement"
        );

        notifyStudent(student, "PLACEMENT_DRIVE", title, message, "/student/placement", emailSubject, htmlBody);
    }

    public void notifyPlacementResult(User student, String companyName, String roleTitle, String status, String remarks) {
        if (student == null) {
            return;
        }

        String normalizedStatus = defaultString(status, "UPDATED").toUpperCase();
        String title = "Placement Result Update";
        String message = "Your application for " + companyName + " - " + roleTitle + " is now "
                + normalizedStatus + ". Visit the placement page for full details.";

        String emailSubject = "Placement Result: " + normalizedStatus;
        String htmlBody = buildStudentEmail(
                "Placement Result Update",
                "There is an update on your placement application.",
                List.of(
                        "Company: " + defaultString(companyName, "Company"),
                        "Role: " + defaultString(roleTitle, "Role"),
                        "Status: " + normalizedStatus,
                        "Remarks: " + defaultString(remarks, "No additional remarks")
                ),
                "Open the placement page on the website to view the latest result details.",
                "/student/placement"
        );

        notifyStudent(student, "PLACEMENT_REVIEW", title, message, "/student/placement", emailSubject, htmlBody);
    }

    private void notifyDepartmentStudents(String department, String title, String message, String actionUrl,
                                          String type, String emailSubject, String htmlBody) {
        List<User> students = resolveStudentsForDepartment(department);

        for (User student : students) {
            notifyStudent(student, type, title, message, actionUrl, emailSubject, htmlBody);
        }
    }

    private List<User> resolveStudentsForDepartment(String department) {
        String normalizedDepartment = defaultString(department, "").trim();

        List<User> students;
        if (normalizedDepartment.isBlank() || "General".equalsIgnoreCase(normalizedDepartment)) {
            students = userRepository.findByRole(Role.STUDENT);
        } else {
            students = userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(
                    normalizedDepartment,
                    List.of(Role.STUDENT)
            );

            if (students.isEmpty()) {
                String target = normalizedDepartment.toLowerCase();
                students = userRepository.findByRole(Role.STUDENT).stream()
                        .filter(user -> user.getStudentDetails() != null)
                        .filter(user -> matchesDepartment(user, target))
                        .collect(Collectors.toList());
            }
        }

        return students.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));
    }

    private boolean matchesDepartment(User user, String targetDepartment) {
        if (user.getStudentDetails() == null) {
            return false;
        }

        return equalsIgnoreCase(user.getStudentDetails().getDepartment(), targetDepartment)
                || equalsIgnoreCase(user.getStudentDetails().getBranchName(), targetDepartment)
                || equalsIgnoreCase(user.getStudentDetails().getBranchCode(), targetDepartment);
    }

    private boolean equalsIgnoreCase(String value, String expectedLowerCase) {
        return value != null && value.trim().toLowerCase().equals(expectedLowerCase);
    }

    private void notifyStudent(User student, String type, String title, String message, String actionUrl,
                               String emailSubject, String htmlBody) {
        try {
            notificationService.createNotification(student.getFirebaseUid(), type, title, message, actionUrl);
        } catch (Exception ignored) {
        }

        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            try {
                emailService.sendHtmlEmail(student.getEmail(), emailSubject, personalizeHtml(htmlBody, student));
            } catch (Exception ignored) {
            }
        }
    }

    private String personalizeHtml(String htmlBody, User student) {
        return htmlBody.replace("{{studentName}}", defaultString(student.getFullName(), "Student"));
    }

    private String buildStudentEmail(String heading, String intro, List<String> facts, String footerText, String actionPath) {
        String factsHtml = facts.stream()
                .filter(Objects::nonNull)
                .map(item -> "<li style='margin:6px 0;color:#374151;'>" + item + "</li>")
                .reduce("", String::concat);

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f3f4f6;margin:0;padding:24px;'>"
                + "<div style='max-width:560px;margin:0 auto;background:#ffffff;border-radius:14px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.08);'>"
                + "<div style='background:#1d4ed8;padding:24px 28px;color:#fff;'>"
                + "<h1 style='margin:0;font-size:22px;'>Academic Platform</h1>"
                + "<p style='margin:8px 0 0;font-size:14px;opacity:0.9;'>" + heading + "</p>"
                + "</div>"
                + "<div style='padding:28px;'>"
                + "<p style='margin-top:0;color:#111827;'>Dear <strong>{{studentName}}</strong>,</p>"
                + "<p style='color:#374151;line-height:1.6;'>" + intro + "</p>"
                + "<div style='background:#f8fafc;border:1px solid #e5e7eb;border-radius:10px;padding:16px 18px;margin:18px 0;'>"
                + "<ul style='padding-left:18px;margin:0;'>" + factsHtml + "</ul>"
                + "</div>"
                + "<p style='color:#4b5563;line-height:1.6;'>" + footerText + "</p>"
                + "<a href='" + buildFrontendUrl(actionPath) + "' style='display:inline-block;padding:12px 18px;background:#2563eb;color:#fff;text-decoration:none;border-radius:8px;font-weight:600;'>View in Website</a>"
                + "<p style='margin:18px 0 0;color:#6b7280;font-size:12px;'>If the button does not open directly, log in to your Academic Platform account and visit the relevant page.</p>"
                + "</div></div></body></html>";
    }

    private String buildFrontendUrl(String actionPath) {
        String baseUrl = defaultString(frontendUrl, "http://localhost:5173").trim();
        String normalizedPath = defaultString(actionPath, "/").trim();

        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            return normalizedPath;
        }

        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) + normalizedPath
                : baseUrl + normalizedPath;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatDate(LocalDate value) {
        return value == null ? "To be announced" : value.format(DATE_FORMATTER);
    }
}
