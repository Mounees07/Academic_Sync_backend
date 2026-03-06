package com.academic.platform.repository;

import com.academic.platform.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
        List<LeaveRequest> findByStudentFirebaseUid(String studentUid);

        @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveRequest l WHERE l.student.studentDetails.mentor.firebaseUid = :mentorUid AND (l.parentStatus = 'APPROVED' OR l.parentStatus = 'PENDING')")
        List<LeaveRequest> findByStudentMentorFirebaseUidAndParentStatus(
                        @org.springframework.data.repository.query.Param("mentorUid") String mentorUid);

        Optional<LeaveRequest> findByParentActionToken(String token);

        @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveRequest l WHERE l.student.studentDetails.department = :department ORDER BY l.createdAt DESC")
        List<LeaveRequest> findByStudentStudentDetails_DepartmentOrderByCreatedAtDesc(
                        @org.springframework.data.repository.query.Param("department") String department);

        @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveRequest l WHERE l.student.studentDetails.rollNumber = :rollNumber")
        List<LeaveRequest> findByStudentStudentDetails_RollNumber(
                        @org.springframework.data.repository.query.Param("rollNumber") String rollNumber);

        List<LeaveRequest> findByStudentInAndParentStatusIn(
                        java.util.Collection<com.academic.platform.model.User> students,
                        java.util.Collection<String> statuses);

        /**
         * Find all APPROVED leaves whose date range covers the given date.
         * Used by the gate security calendar view.
         */
        @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveRequest l WHERE l.mentorStatus = 'APPROVED' "
                        +
                        "AND l.fromDate <= :date AND l.toDate >= :date")
        List<LeaveRequest> findApprovedLeavesByDate(
                        @org.springframework.data.repository.query.Param("date") LocalDate date);

        /**
         * Search APPROVED leaves by roll number (partial, case-insensitive).
         */
        @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveRequest l JOIN l.student s JOIN s.studentDetails sd "
                        + "WHERE l.mentorStatus = 'APPROVED' "
                        + "AND LOWER(sd.rollNumber) LIKE LOWER(CONCAT('%', :q, '%')) "
                        + "ORDER BY l.fromDate DESC")
        List<LeaveRequest> findApprovedLeavesByRoll(
                        @org.springframework.data.repository.query.Param("q") String q);

        /**
         * Search APPROVED leaves by student full name (partial, case-insensitive).
         */
        @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveRequest l JOIN l.student s "
                        + "WHERE l.mentorStatus = 'APPROVED' "
                        + "AND LOWER(s.fullName) LIKE LOWER(CONCAT('%', :q, '%')) "
                        + "ORDER BY l.fromDate DESC")
        List<LeaveRequest> findApprovedLeavesByName(
                        @org.springframework.data.repository.query.Param("q") String q);
}
