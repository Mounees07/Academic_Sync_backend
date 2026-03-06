package com.academic.platform.repository;

import com.academic.platform.model.ExamSeating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamSeatingRepository extends JpaRepository<ExamSeating, Long> {
    List<ExamSeating> findByExamId(Long examId);

    List<ExamSeating> findByVenueId(Long venueId);

    List<ExamSeating> findByStudentId(Long studentId);

    List<ExamSeating> findByStudentFirebaseUid(String firebaseUid);

    /**
     * Bulk-delete by exam — executed immediately as a single SQL DELETE, bypassing
     * Hibernate's lazy flush.
     */
    @Modifying
    @Query("DELETE FROM ExamSeating es WHERE es.exam.id = :examId")
    void deleteAllByExamIdDirect(@Param("examId") Long examId);
}
