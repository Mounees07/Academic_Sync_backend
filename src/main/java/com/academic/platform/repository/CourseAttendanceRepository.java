package com.academic.platform.repository;

import com.academic.platform.model.CourseAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseAttendanceRepository extends JpaRepository<CourseAttendance, Long> {
    List<CourseAttendance> findBySessionId(Long sessionId);

    @org.springframework.data.jpa.repository.Query("SELECT ca FROM CourseAttendance ca WHERE ca.student.firebaseUid = :firebaseUid ORDER BY ca.markedAt DESC")
    List<CourseAttendance> findByStudentFirebaseUidOrderByMarkedAtDesc(
            @org.springframework.data.repository.query.Param("firebaseUid") String firebaseUid);

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<CourseAttendance> findBySessionSectionIdAndStudentId(Long sectionId, Long studentId);
}
