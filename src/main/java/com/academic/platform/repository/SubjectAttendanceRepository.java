package com.academic.platform.repository;

import com.academic.platform.model.SubjectAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectAttendanceRepository extends JpaRepository<SubjectAttendance, Long> {

    // ✅ Use firebaseUid — the actual field name on User entity
    List<SubjectAttendance> findByStudentFirebaseUid(String firebaseUid);

    List<SubjectAttendance> findByStudentFirebaseUidAndMonthAndYear(String firebaseUid, int month, int year);

    Optional<SubjectAttendance> findByStudentFirebaseUidAndSectionIdAndMonthAndYear(
            String firebaseUid, Long sectionId, int month, int year);

    @Query("SELECT sa FROM SubjectAttendance sa WHERE sa.student.firebaseUid = :uid " +
           "ORDER BY sa.year DESC, sa.month DESC")
    List<SubjectAttendance> findAllByStudentUidOrderByDate(@Param("uid") String uid);

    @Query("SELECT sa FROM SubjectAttendance sa WHERE sa.student.firebaseUid = :uid " +
           "AND sa.year = :year ORDER BY sa.month ASC")
    List<SubjectAttendance> findByStudentUidAndYear(@Param("uid") String uid, @Param("year") int year);
}
