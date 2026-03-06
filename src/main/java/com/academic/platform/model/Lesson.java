package com.academic.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 2048)
    private String contentUrl; // Video URL or Document URL

    @Column(nullable = false)
    private String contentType; // VIDEO, PDF, DOC, etc.

    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private LocalDateTime createdAt;

    public Lesson() {
    }

    public Lesson(Long id, String title, String description, String contentUrl, String contentType, Integer orderIndex,
            Course course) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.contentUrl = contentUrl;
        this.contentType = contentType;
        this.orderIndex = orderIndex;
        this.course = course;
        this.createdAt = LocalDateTime.now();
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

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Static Builder
    public static class LessonBuilder {
        private Long id;
        private String title;
        private String description;
        private String contentUrl;
        private String contentType;
        private Integer orderIndex;
        private Course course;

        public LessonBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LessonBuilder title(String title) {
            this.title = title;
            return this;
        }

        public LessonBuilder description(String description) {
            this.description = description;
            return this;
        }

        public LessonBuilder contentUrl(String contentUrl) {
            this.contentUrl = contentUrl;
            return this;
        }

        public LessonBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public LessonBuilder orderIndex(Integer orderIndex) {
            this.orderIndex = orderIndex;
            return this;
        }

        public LessonBuilder course(Course course) {
            this.course = course;
            return this;
        }

        public Lesson build() {
            return new Lesson(id, title, description, contentUrl, contentType, orderIndex, course);
        }
    }

    public static LessonBuilder builder() {
        return new LessonBuilder();
    }
}
