package com.academic.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mentorship_meetings")
public class MentorshipMeeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String location; // Room number or Google Meet link

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mentor_id")
    private User mentor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mentee_id")
    private User mentee;

    private String status = "SCHEDULED"; // SCHEDULED, COMPLETED, CANCELLED

    public MentorshipMeeting() {
    }

    public MentorshipMeeting(Long id, String title, String description, String location, LocalDateTime startTime,
            LocalDateTime endTime, User mentor, User mentee, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.mentor = mentor;
        this.mentee = mentee;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public User getMentor() {
        return mentor;
    }

    public void setMentor(User mentor) {
        this.mentor = mentor;
    }

    public User getMentee() {
        return mentee;
    }

    public void setMentee(User mentee) {
        this.mentee = mentee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Builder Pattern
    public static MentorshipMeetingBuilder builder() {
        return new MentorshipMeetingBuilder();
    }

    public static class MentorshipMeetingBuilder {
        private Long id;
        private String title;
        private String description;
        private String location;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private User mentor;
        private User mentee;
        private String status = "SCHEDULED";

        public MentorshipMeetingBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public MentorshipMeetingBuilder title(String title) {
            this.title = title;
            return this;
        }

        public MentorshipMeetingBuilder description(String description) {
            this.description = description;
            return this;
        }

        public MentorshipMeetingBuilder location(String location) {
            this.location = location;
            return this;
        }

        public MentorshipMeetingBuilder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public MentorshipMeetingBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public MentorshipMeetingBuilder mentor(User mentor) {
            this.mentor = mentor;
            return this;
        }

        public MentorshipMeetingBuilder mentee(User mentee) {
            this.mentee = mentee;
            return this;
        }

        public MentorshipMeetingBuilder status(String status) {
            this.status = status;
            return this;
        }

        public MentorshipMeeting build() {
            return new MentorshipMeeting(id, title, description, location, startTime, endTime, mentor, mentee, status);
        }
    }
}
