package com.academic.platform.repository;

import com.academic.platform.model.AcademicSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AcademicScheduleRepository extends JpaRepository<AcademicSchedule, Long> {
    List<AcademicSchedule> findByDateAfterOrderByDateAsc(LocalDate date);

    List<AcademicSchedule> findByDepartmentAndDateAfterOrderByDateAsc(String department, LocalDate date);

    List<AcademicSchedule> findTop20ByOrderByIdDesc();

    List<AcademicSchedule> findByDateAndSubjectNameIgnoreCase(LocalDate date, String subjectName);

    List<AcademicSchedule> findByDate(LocalDate date);
}
