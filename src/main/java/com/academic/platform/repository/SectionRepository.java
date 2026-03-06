package com.academic.platform.repository;

import com.academic.platform.model.Section;
import com.academic.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByFaculty(User faculty);

    List<Section> findByCourseId(Long courseId);

    boolean existsByCourseAndFacultyAndSemesterAndYear(com.academic.platform.model.Course course, User faculty,
            String semester, Integer year);

    List<Section> findBySemester(String semester);
}
