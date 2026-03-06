package com.academic.platform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sections")
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    private String semester;
    private Integer year;

    @org.hibernate.annotations.Formula("(select count(*) from enrollments e where e.section_id = id)")
    private Integer enrollmentCount;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean testsEnabled = false;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public User getFaculty() {
        return faculty;
    }

    public void setFaculty(User faculty) {
        this.faculty = faculty;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getEnrollmentCount() {
        return enrollmentCount;
    }

    public void setEnrollmentCount(Integer enrollmentCount) {
        this.enrollmentCount = enrollmentCount;
    }

    public Boolean getTestsEnabled() {
        return testsEnabled;
    }

    public void setTestsEnabled(Boolean testsEnabled) {
        this.testsEnabled = testsEnabled;
    }

    // Manual Builder
    public static SectionBuilder builder() {
        return new SectionBuilder();
    }

    public static class SectionBuilder {
        private Course course;
        private User faculty;
        private String semester;
        private Integer year;

        public SectionBuilder course(Course course) {
            this.course = course;
            return this;
        }

        public SectionBuilder faculty(User faculty) {
            this.faculty = faculty;
            return this;
        }

        public SectionBuilder semester(String semester) {
            this.semester = semester;
            return this;
        }

        public SectionBuilder year(Integer year) {
            this.year = year;
            return this;
        }

        public Section build() {
            Section s = new Section();
            s.setCourse(course);
            s.setFaculty(faculty);
            s.setSemester(semester);
            s.setYear(year);
            return s;
        }
    }
}
