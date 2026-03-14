package com.academic.platform.repository;

import com.academic.platform.model.FacultyLeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacultyLeaveRequestRepository extends JpaRepository<FacultyLeaveRequest, Long> {

    // All leaves by applicant (for faculty's own view)
    List<FacultyLeaveRequest> findByApplicantFirebaseUidOrderByCreatedAtDesc(String uid);

    // Leaves for HOD to approve: applicantRole=FACULTY, hodStatus=PENDING
    // Filter by department so HOD only sees their own faculty
    @Query("SELECT f FROM FacultyLeaveRequest f " +
           "WHERE f.hodStatus = 'PENDING' " +
           "AND f.applicantRole = 'FACULTY' " +
           "AND LOWER(f.applicant.studentDetails.department) = LOWER(:department)")
    List<FacultyLeaveRequest> findPendingForHod(@Param("department") String department);

    // Leaves for Admin to approve:
    //   1. Faculty leaves where HOD has already approved (hodStatus=APPROVED)
    //   2. HOD leaves where hodStatus=SKIPPED (HOD applied, admin handles directly)
    @Query("SELECT f FROM FacultyLeaveRequest f " +
           "WHERE f.adminStatus = 'PENDING' " +
           "AND (f.hodStatus = 'APPROVED' OR f.hodStatus = 'SKIPPED') " +
           "ORDER BY f.createdAt ASC")
    List<FacultyLeaveRequest> findPendingForAdmin();

    // Count pending for HOD dashboard badge
    @Query("SELECT COUNT(f) FROM FacultyLeaveRequest f " +
           "WHERE f.hodStatus = 'PENDING' " +
           "AND f.applicantRole = 'FACULTY' " +
           "AND LOWER(f.applicant.studentDetails.department) = LOWER(:department)")
    long countPendingForHod(@Param("department") String department);

    // Count pending for Admin badge
    @Query("SELECT COUNT(f) FROM FacultyLeaveRequest f " +
           "WHERE f.adminStatus = 'PENDING' " +
           "AND (f.hodStatus = 'APPROVED' OR f.hodStatus = 'SKIPPED')")
    long countPendingForAdmin();

    // All leaves: for admin full view
    List<FacultyLeaveRequest> findAllByOrderByCreatedAtDesc();
}
