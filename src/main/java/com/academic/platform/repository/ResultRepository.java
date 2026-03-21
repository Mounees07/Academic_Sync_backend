package com.academic.platform.repository;

import com.academic.platform.model.Result;
import com.academic.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findByStudent(User student);

    List<Result> findByStudentAndSemester(User student, Integer semester);

    Optional<Result> findByStudentAndSemesterAndSubjectCode(User student, Integer semester, String subjectCode);

    List<Result> findTop50ByOrderByPublishedDateDesc();
}
