package com.academic.platform.repository;

import com.academic.platform.model.Result;
import com.academic.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findByStudent(User student);

    List<Result> findByStudentAndSemester(User student, Integer semester);

    List<Result> findTop50ByOrderByPublishedDateDesc();
}
