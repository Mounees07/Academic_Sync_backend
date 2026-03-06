package com.academic.platform.dto;

import java.util.List;
import com.academic.platform.model.LeaveRequest;

public class DepartmentDashboardDTO {
    private long totalFaculty;
    private long totalStudents;
    private long totalCourses;
    private long pendingLeaves; // "Budget Alerts" replacement? Or "Pending Requests"
    private List<LeaveRequest> recentActivities;

    // Getters and Setters
    public long getTotalFaculty() {
        return totalFaculty;
    }

    public void setTotalFaculty(long totalFaculty) {
        this.totalFaculty = totalFaculty;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(long totalCourses) {
        this.totalCourses = totalCourses;
    }

    public long getPendingLeaves() {
        return pendingLeaves;
    }

    public void setPendingLeaves(long pendingLeaves) {
        this.pendingLeaves = pendingLeaves;
    }

    public List<LeaveRequest> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<LeaveRequest> recentActivities) {
        this.recentActivities = recentActivities;
    }
}
