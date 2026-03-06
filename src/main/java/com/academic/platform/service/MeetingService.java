package com.academic.platform.service;

import com.academic.platform.model.MentorshipMeeting;
import com.academic.platform.model.User;
import com.academic.platform.repository.MentorshipMeetingRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MeetingService {

        @Autowired
        private MentorshipMeetingRepository meetingRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private EmailService emailService;

        public MentorshipMeeting scheduleMeeting(String mentorUid, String menteeUid, MentorshipMeeting meeting) {
                User mentor = userRepository.findByFirebaseUid(mentorUid)
                                .orElseThrow(() -> new RuntimeException("Mentor not found"));
                User mentee = userRepository.findByFirebaseUid(menteeUid)
                                .orElseThrow(() -> new RuntimeException("Mentee not found"));

                meeting.setMentor(mentor);
                meeting.setMentee(mentee);
                MentorshipMeeting savedMeeting = meetingRepository.save(meeting);

                // Send Email Notification
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a");
                emailService.sendMeetingNotification(
                                mentee.getEmail(),
                                mentor.getFullName(),
                                meeting.getTitle(),
                                meeting.getStartTime().format(formatter),
                                meeting.getLocation());

                return savedMeeting;
        }

        public List<MentorshipMeeting> scheduleGroupMeeting(String mentorUid, MentorshipMeeting meetingDetails) {
                User mentor = userRepository.findByFirebaseUid(mentorUid)
                                .orElseThrow(() -> new RuntimeException("Mentor not found"));

                List<User> mentees = userRepository.findByStudentDetails_Mentor_FirebaseUid(mentorUid);
                if (mentees.isEmpty())
                        return List.of();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a");

                // 1. Save meetings for each mentee
                List<MentorshipMeeting> savedMeetings = mentees.stream().map(mentee -> {
                        MentorshipMeeting meeting = MentorshipMeeting.builder()
                                        .title(meetingDetails.getTitle())
                                        .description(meetingDetails.getDescription())
                                        .location(meetingDetails.getLocation())
                                        .startTime(meetingDetails.getStartTime())
                                        .endTime(meetingDetails.getEndTime())
                                        .mentor(mentor)
                                        .mentee(mentee)
                                        .status("SCHEDULED")
                                        .build();
                        return meetingRepository.save(meeting);
                }).toList();

                // 2. Send Bulk Email
                String[] menteeEmails = mentees.stream().map(User::getEmail).toArray(String[]::new);
                emailService.sendBulkMeetingNotification(
                                menteeEmails,
                                mentor.getFullName(),
                                meetingDetails.getTitle(),
                                meetingDetails.getStartTime().format(formatter),
                                meetingDetails.getLocation());

                return savedMeetings;
        }

        public List<MentorshipMeeting> getMeetingsForMentor(String mentorUid) {
                return meetingRepository.findByMentorFirebaseUid(mentorUid);
        }

        public List<MentorshipMeeting> getMeetingsForMentee(String menteeUid) {
                return meetingRepository.findByMenteeFirebaseUid(menteeUid);
        }

        public void deleteMeeting(Long meetingId) {
                MentorshipMeeting meeting = meetingRepository.findById(meetingId)
                                .orElseThrow(() -> new RuntimeException("Meeting not found"));

                // Notify Mentee via Email
                emailService.sendHtmlEmail(
                                meeting.getMentee().getEmail(),
                                "Meeting Cancelled: " + meeting.getTitle(),
                                "<html><body><p>Dear " + meeting.getMentee().getFullName() + ",</p>" +
                                                "<p>The meeting <strong>" + meeting.getTitle()
                                                + "</strong> scheduled for "
                                                +
                                                meeting.getStartTime()
                                                + " has been <strong>cancelled</strong> by the mentor.</p></body></html>");

                meetingRepository.delete(meeting);
        }

        public MentorshipMeeting updateMeeting(Long meetingId, MentorshipMeeting updatedDetails) {
                MentorshipMeeting meeting = meetingRepository.findById(meetingId)
                                .orElseThrow(() -> new RuntimeException("Meeting not found"));

                meeting.setTitle(updatedDetails.getTitle());
                meeting.setStartTime(updatedDetails.getStartTime());
                meeting.setLocation(updatedDetails.getLocation());
                meeting.setDescription(updatedDetails.getDescription());

                MentorshipMeeting saved = meetingRepository.save(meeting);

                // Notify Mentee
                emailService.sendHtmlEmail(
                                saved.getMentee().getEmail(),
                                "Meeting Rescheduled: " + saved.getTitle(),
                                "<html><body><p>Dear " + saved.getMentee().getFullName() + ",</p>" +
                                                "<p>The meeting <strong>" + saved.getTitle()
                                                + "</strong> has been rescheduled to: " +
                                                saved.getStartTime() + "</p>" +
                                                "<p>Location: " + saved.getLocation() + "</p></body></html>");

                return saved;
        }

        @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000) // Run every minute
        public void cleanupPastMeetings() {
                // Remove meetings that started more than 1 hour ago
                java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusHours(1);

                List<MentorshipMeeting> pastMeetings = meetingRepository.findByStartTimeBefore(threshold);

                if (!pastMeetings.isEmpty()) {
                        System.out.println("Cleaning up " + pastMeetings.size() + " past meetings...");
                        meetingRepository.deleteAll(pastMeetings);
                }
        }
}
