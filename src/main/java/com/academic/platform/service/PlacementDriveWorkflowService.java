package com.academic.platform.service;

import com.academic.platform.model.PlacementDrive;
import com.academic.platform.model.PlacementDriveApplication;
import com.academic.platform.model.User;
import com.academic.platform.repository.PlacementDriveApplicationRepository;
import com.academic.platform.repository.PlacementDriveRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlacementDriveWorkflowService {

    @Autowired
    private PlacementDriveApplicationRepository applicationRepository;

    @Autowired
    private PlacementDriveRepository driveRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    public void syncDriveApplications(PlacementDrive drive, Set<String> previousEligibleStudentUids) {
        Map<String, PlacementDriveApplication> existingByStudent = applicationRepository.findByDriveIdOrderByCreatedAtAsc(drive.getId())
                .stream()
                .collect(Collectors.toMap(app -> app.getStudent().getFirebaseUid(), Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Set<String> currentEligibleStudentUids = drive.getEligibleStudents().stream()
                .map(User::getFirebaseUid)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (User student : drive.getEligibleStudents()) {
            PlacementDriveApplication application = existingByStudent.get(student.getFirebaseUid());
            boolean isNewEligibility = !previousEligibleStudentUids.contains(student.getFirebaseUid());

            if (application == null) {
                application = PlacementDriveApplication.builder()
                        .drive(drive)
                        .student(student)
                        .status(resolveStatusFromDriveMembership(drive, student, null))
                        .build();
            } else {
                application.setStatus(resolveStatusFromDriveMembership(drive, student, application.getStatus()));
            }

            if ("APPLIED".equals(application.getStatus()) && application.getAppliedAt() == null) {
                application.setAppliedAt(LocalDateTime.now());
            }
            if ("SHORTLISTED".equals(application.getStatus()) && application.getReviewedAt() == null) {
                application.setReviewedAt(LocalDateTime.now());
            }

            applicationRepository.save(application);

            if (isNewEligibility) {
                notifyStudentOfNewDrive(drive, student);
            }
        }

        for (PlacementDriveApplication application : existingByStudent.values()) {
            String studentUid = application.getStudent().getFirebaseUid();
            if (currentEligibleStudentUids.contains(studentUid)) {
                continue;
            }
            if ("ELIGIBLE".equals(application.getStatus())) {
                applicationRepository.delete(application);
            }
        }
    }

    public PlacementDriveApplication applyForDrive(Long driveId, String studentUid) {
        PlacementDrive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new RuntimeException("Drive not found."));
        PlacementDriveApplication application = applicationRepository.findByDriveIdAndStudentFirebaseUid(driveId, studentUid)
                .orElseThrow(() -> new RuntimeException("Student is not marked eligible for this drive."));

        if (!"ELIGIBLE".equals(application.getStatus())) {
            return application;
        }

        application.setStatus("APPLIED");
        application.setAppliedAt(LocalDateTime.now());
        drive.getAppliedStudents().add(application.getStudent());
        driveRepository.save(drive);

        notificationService.createNotification(
                studentUid,
                "PLACEMENT_APPLICATION",
                "Drive Application Submitted",
                "You have applied for " + safeDriveName(drive) + ".",
                "/student/placement"
        );
        return applicationRepository.save(application);
    }

    public PlacementDriveApplication reviewApplication(Long driveId, String studentUid, String status, String remarks) {
        PlacementDrive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new RuntimeException("Drive not found."));
        PlacementDriveApplication application = applicationRepository.findByDriveIdAndStudentFirebaseUid(driveId, studentUid)
                .orElseThrow(() -> new RuntimeException("Application not found."));

        String normalizedStatus = String.valueOf(status).toUpperCase(Locale.ROOT);
        if (!Set.of("ELIGIBLE", "APPLIED", "SHORTLISTED", "REJECTED").contains(normalizedStatus)) {
            throw new RuntimeException("Unsupported application status.");
        }

        application.setStatus(normalizedStatus);
        application.setCoordinatorRemarks(remarks);
        application.setReviewedAt(LocalDateTime.now());

        if ("APPLIED".equals(normalizedStatus) && application.getAppliedAt() == null) {
            application.setAppliedAt(LocalDateTime.now());
        }

        if (Set.of("APPLIED", "SHORTLISTED", "REJECTED").contains(normalizedStatus)) {
            drive.getAppliedStudents().add(application.getStudent());
        } else {
            drive.getAppliedStudents().remove(application.getStudent());
        }

        if ("SHORTLISTED".equals(normalizedStatus)) {
            drive.getSelectedStudents().add(application.getStudent());
        } else {
            drive.getSelectedStudents().remove(application.getStudent());
        }

        driveRepository.save(drive);

        notificationService.createNotification(
                studentUid,
                "PLACEMENT_REVIEW",
                "Application Reviewed",
                "Your application for " + safeDriveName(drive) + " is now " + normalizedStatus + ".",
                "/student/placement"
        );
        if (application.getStudent().getEmail() != null && !application.getStudent().getEmail().isBlank()) {
            emailService.sendPlacementApplicationReview(
                    application.getStudent().getEmail(),
                    defaultString(application.getStudent().getFullName(), "Student"),
                    drive.getCompany().getCompanyName(),
                    drive.getRoleTitle(),
                    normalizedStatus,
                    remarks
            );
        }
        return applicationRepository.save(application);
    }

    public PlacementDriveApplication markAttendance(Long driveId, String studentUid, boolean attended) {
        PlacementDriveApplication application = applicationRepository.findByDriveIdAndStudentFirebaseUid(driveId, studentUid)
                .orElseThrow(() -> new RuntimeException("Application not found."));

        application.setAttended(attended);
        application.setAttendanceMarkedAt(attended ? LocalDateTime.now() : null);
        return applicationRepository.save(application);
    }

    public List<PlacementDriveApplication> getApplicationsForDrive(Long driveId) {
        return applicationRepository.findByDriveIdOrderByCreatedAtAsc(driveId);
    }

    public List<PlacementDriveApplication> getApplicationsForStudent(String studentUid) {
        return applicationRepository.findByStudentFirebaseUidOrderByCreatedAtDesc(studentUid);
    }

    public void deleteApplicationsForDrive(Long driveId) {
        applicationRepository.deleteByDriveId(driveId);
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendUnappliedDriveReminders() {
        LocalDate today = LocalDate.now();
        List<PlacementDrive> drives = driveRepository.findAllByOrderByDriveDateDesc().stream()
                .filter(drive -> {
                    String status = defaultString(drive.getStatus(), "PLANNED").toUpperCase(Locale.ROOT);
                    return Set.of("PLANNED", "ACTIVE").contains(status)
                            && (drive.getDriveDate() == null || !drive.getDriveDate().isBefore(today));
                })
                .toList();

        Map<String, List<PlacementDriveApplication>> mentorEscalations = new LinkedHashMap<>();

        for (PlacementDrive drive : drives) {
            for (PlacementDriveApplication application : applicationRepository.findByDriveIdAndStatusIn(drive.getId(), List.of("ELIGIBLE"))) {
                if (application.getReminderCount() < 2) {
                    if (application.getLastReminderSentAt() != null
                            && application.getLastReminderSentAt().toLocalDate().isEqual(today)) {
                        continue;
                    }
                    sendReminder(drive, application);
                    continue;
                }
                if (application.getMentorNotifiedAt() == null && application.getStudent().getStudentDetails() != null
                        && application.getStudent().getStudentDetails().getMentor() != null) {
                    String key = drive.getId() + "::" + application.getStudent().getStudentDetails().getMentor().getFirebaseUid();
                    mentorEscalations.computeIfAbsent(key, ignored -> new ArrayList<>()).add(application);
                }
            }
        }

        for (List<PlacementDriveApplication> applications : mentorEscalations.values()) {
            notifyMentorOfNonApplicants(applications);
        }
    }

    private void sendReminder(PlacementDrive drive, PlacementDriveApplication application) {
        User student = application.getStudent();
        int reminderNumber = application.getReminderCount() + 1;
        notificationService.createNotification(
                student.getFirebaseUid(),
                "PLACEMENT_REMINDER",
                "Placement Drive Reminder",
                "Reminder " + reminderNumber + ": apply for " + safeDriveName(drive) + " if you are interested.",
                "/student/placement"
        );
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            emailService.sendPlacementDriveReminder(
                    student.getEmail(),
                    defaultString(student.getFullName(), "Student"),
                    drive.getCompany().getCompanyName(),
                    drive.getRoleTitle(),
                    reminderNumber
            );
        }
        application.setReminderCount(reminderNumber);
        application.setLastReminderSentAt(LocalDateTime.now());
        applicationRepository.save(application);
    }

    private void notifyStudentOfNewDrive(PlacementDrive drive, User student) {
        notificationService.createNotification(
                student.getFirebaseUid(),
                "PLACEMENT_DRIVE",
                "New Placement Drive",
                "You are eligible for " + safeDriveName(drive) + ". Review the requirements and apply from your placement page.",
                "/student/placement"
        );
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            emailService.sendPlacementDriveInvitation(
                    student.getEmail(),
                    defaultString(student.getFullName(), "Student"),
                    drive.getCompany().getCompanyName(),
                    drive.getRoleTitle(),
                    drive.getDriveDate() == null ? "To be announced" : drive.getDriveDate().toString(),
                    defaultString(drive.getLocation(), "To be announced"),
                    defaultString(drive.getEligibilityCriteria(), "Refer to the placement portal")
            );
        }
    }

    private void notifyMentorOfNonApplicants(List<PlacementDriveApplication> applications) {
        PlacementDrive drive = applications.get(0).getDrive();
        User mentor = applications.get(0).getStudent().getStudentDetails().getMentor();
        String studentList = applications.stream()
                .map(app -> app.getStudent().getFullName() + " (" + defaultString(app.getStudent().getStudentDetails().getRollNumber(), app.getStudent().getFirebaseUid()) + ")")
                .collect(Collectors.joining("\n"));

        notificationService.createNotification(
                mentor.getFirebaseUid(),
                "PLACEMENT_MENTOR_ALERT",
                "Students Have Not Applied",
                "Some mentees have not applied for " + safeDriveName(drive) + " after two reminders.",
                "/mentor/dashboard"
        );
        if (mentor.getEmail() != null && !mentor.getEmail().isBlank()) {
            emailService.sendMentorPlacementAlert(
                    mentor.getEmail(),
                    defaultString(mentor.getFullName(), "Mentor"),
                    drive.getCompany().getCompanyName(),
                    drive.getRoleTitle(),
                    studentList
            );
        }

        LocalDateTime now = LocalDateTime.now();
        applications.forEach(app -> app.setMentorNotifiedAt(now));
        applicationRepository.saveAll(applications);
    }

    private String resolveStatusFromDriveMembership(PlacementDrive drive, User student, String currentStatus) {
        if (drive.getSelectedStudents().contains(student)) {
            return "SHORTLISTED";
        }
        if (drive.getAppliedStudents().contains(student)) {
            return "APPLIED";
        }
        if ("REJECTED".equals(currentStatus)) {
            return "REJECTED";
        }
        return "ELIGIBLE";
    }

    private String safeDriveName(PlacementDrive drive) {
        return drive.getCompany().getCompanyName() + " - " + drive.getRoleTitle();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
