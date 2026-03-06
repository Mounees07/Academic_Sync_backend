package com.academic.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private String fileUrl;
    private LocalDateTime submissionDate;
    private Double grade;
    @Column(columnDefinition = "TEXT")
    private String feedback;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public Double getGrade() {
        return grade;
    }

    public void setGrade(Double grade) {
        this.grade = grade;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    // Manual Builder
    public static SubmissionBuilder builder() {
        return new SubmissionBuilder();
    }

    public static class SubmissionBuilder {
        private Assignment assignment;
        private User student;
        private String fileUrl;
        private LocalDateTime submissionDate;
        private Double grade;
        private String feedback;

        public SubmissionBuilder assignment(Assignment assignment) {
            this.assignment = assignment;
            return this;
        }

        public SubmissionBuilder student(User student) {
            this.student = student;
            return this;
        }

        public SubmissionBuilder fileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
            return this;
        }

        public SubmissionBuilder submissionDate(LocalDateTime submissionDate) {
            this.submissionDate = submissionDate;
            return this;
        }

        public SubmissionBuilder grade(Double grade) {
            this.grade = grade;
            return this;
        }

        public SubmissionBuilder feedback(String feedback) {
            this.feedback = feedback;
            return this;
        }

        public Submission build() {
            Submission s = new Submission();
            s.setAssignment(assignment);
            s.setStudent(student);
            s.setFileUrl(fileUrl);
            s.setSubmissionDate(submissionDate);
            s.setGrade(grade);
            s.setFeedback(feedback);
            return s;
        }
    }
}
