package com.academic.platform.repository;

import com.academic.platform.model.CollegeCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CollegeCalendarEventRepository extends JpaRepository<CollegeCalendarEvent, Long> {
    List<CollegeCalendarEvent> findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscEndDateAsc(
            LocalDate endDate,
            LocalDate startDate
    );

    List<CollegeCalendarEvent> findTop12ByEndDateGreaterThanEqualOrderByStartDateAscEndDateAsc(LocalDate date);
}
