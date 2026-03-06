package com.academic.platform.repository;

import com.academic.platform.model.CourseAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseAttendanceRepository extends JpaRepository<CourseAttendance, Long> {
    List<CourseAttendance> findBySessionId(Long sessionId);

    List<CourseAttendance> findByStudentFirebaseUidOrderByMarkedAtDesc(String firebaseUid);

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<CourseAttendance> findBySessionSectionIdAndStudentId(Long sectionId, Long studentId);
}
