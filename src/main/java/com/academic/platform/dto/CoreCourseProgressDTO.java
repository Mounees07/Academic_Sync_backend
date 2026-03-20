package com.academic.platform.dto;

public class CoreCourseProgressDTO {
    private Long sectionId;
    private String courseCode;
    private String courseName;
    private String primaryInstructor;
    private String primaryInstructorInitials;
    private Integer syllabusCompletion;

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getPrimaryInstructor() {
        return primaryInstructor;
    }

    public void setPrimaryInstructor(String primaryInstructor) {
        this.primaryInstructor = primaryInstructor;
    }

    public String getPrimaryInstructorInitials() {
        return primaryInstructorInitials;
    }

    public void setPrimaryInstructorInitials(String primaryInstructorInitials) {
        this.primaryInstructorInitials = primaryInstructorInitials;
    }

    public Integer getSyllabusCompletion() {
        return syllabusCompletion;
    }

    public void setSyllabusCompletion(Integer syllabusCompletion) {
        this.syllabusCompletion = syllabusCompletion;
    }
}

