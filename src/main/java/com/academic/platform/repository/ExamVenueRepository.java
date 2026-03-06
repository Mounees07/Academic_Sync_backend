package com.academic.platform.repository;

import com.academic.platform.model.ExamVenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamVenueRepository extends JpaRepository<ExamVenue, Long> {
    List<ExamVenue> findByExamType(String examType);

    List<ExamVenue> findByIsAvailableTrue();
}
