package com.academic.platform.dto;

import java.util.List;

public class DepartmentAnalyticsDTO {
    public static class EnrollmentTrend {
        private String month;
        private int students;

        public EnrollmentTrend(String month, int students) {
            this.month = month;
            this.students = students;
        }

        public String getMonth() {
            return month;
        }

        public int getStudents() {
            return students;
        }
    }

    public static class PerformanceDistribution {
        private String name;
        private int value;

        public PerformanceDistribution(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }

    public static class AttendanceStats {
        private String yearClass; // "Year 1", etc.
        private double attendance;

        public AttendanceStats(String yearClass, double attendance) {
            this.yearClass = yearClass;
            this.attendance = attendance;
        }

        public String getYearClass() {
            return yearClass;
        } // Map to 'class' field in frontend

        public double getAttendance() {
            return attendance;
        }
    }

    public static class TopStudent {
        private String name;
        private String roll;
        private double attendance;
        private double score; // CGPA

        public TopStudent(String name, String roll, double attendance, double score) {
            this.name = name;
            this.roll = roll;
            this.attendance = attendance;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public String getRoll() {
            return roll;
        }

        public double getAttendance() {
            return attendance;
        }

        public double getScore() {
            return score;
        }
    }

    private int activeStudents;
    private double currentAvgAttendance;
    private double deptCGPA;
    private int activeCourses;

    private List<EnrollmentTrend> enrollmentTrends;
    private List<PerformanceDistribution> performanceDistribution;
    private List<AttendanceStats> attendanceByYear;
    private List<TopStudent> topStudents;

    // Getters and Setters
    public int getActiveStudents() {
        return activeStudents;
    }

    public void setActiveStudents(int activeStudents) {
        this.activeStudents = activeStudents;
    }

    public double getCurrentAvgAttendance() {
        return currentAvgAttendance;
    }

    public void setCurrentAvgAttendance(double currentAvgAttendance) {
        this.currentAvgAttendance = currentAvgAttendance;
    }

    public double getDeptCGPA() {
        return deptCGPA;
    }

    public void setDeptCGPA(double deptCGPA) {
        this.deptCGPA = deptCGPA;
    }

    public int getActiveCourses() {
        return activeCourses;
    }

    public void setActiveCourses(int activeCourses) {
        this.activeCourses = activeCourses;
    }

    public List<EnrollmentTrend> getEnrollmentTrends() {
        return enrollmentTrends;
    }

    public void setEnrollmentTrends(List<EnrollmentTrend> enrollmentTrends) {
        this.enrollmentTrends = enrollmentTrends;
    }

    public List<PerformanceDistribution> getPerformanceDistribution() {
        return performanceDistribution;
    }

    public void setPerformanceDistribution(List<PerformanceDistribution> performanceDistribution) {
        this.performanceDistribution = performanceDistribution;
    }

    public List<AttendanceStats> getAttendanceByYear() {
        return attendanceByYear;
    }

    public void setAttendanceByYear(List<AttendanceStats> attendanceByYear) {
        this.attendanceByYear = attendanceByYear;
    }

    public List<TopStudent> getTopStudents() {
        return topStudents;
    }

    public void setTopStudents(List<TopStudent> topStudents) {
        this.topStudents = topStudents;
    }
}
