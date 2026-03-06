package com.academic.platform.repository;

import com.academic.platform.model.Attendance;
import com.academic.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByStudentAndDate(User student, LocalDate date);

    List<Attendance> findByStudentFirebaseUidOrderByDateDesc(String studentUid);

    // Using a reliable query method name or custom query
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Attendance a WHERE a.student.studentDetails.mentor.firebaseUid = :mentorUid AND a.date = :date")
    List<Attendance> findByStudentStudentDetails_Mentor_FirebaseUidAndDate(
            @org.springframework.data.repository.query.Param("mentorUid") String mentorUid,
            @org.springframework.data.repository.query.Param("date") LocalDate date);

    Optional<Attendance> findFirstByOrderByDateAsc();

    @org.springframework.data.jpa.repository.Query("SELECT a.date, COUNT(a) FROM Attendance a WHERE a.date >= :startDate GROUP BY a.date ORDER BY a.date ASC")
    List<Object[]> findDailyAttendanceStats(
            @org.springframework.data.repository.query.Param("startDate") LocalDate startDate);
}
