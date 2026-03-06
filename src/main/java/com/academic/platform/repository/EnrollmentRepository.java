package com.academic.platform.repository;

import com.academic.platform.model.Enrollment;
import com.academic.platform.model.User;
import com.academic.platform.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(User student);

    List<Enrollment> findBySection(Section section);

    Optional<Enrollment> findByStudentAndSection(User student, Section section);

    List<Enrollment> findBySectionId(Long sectionId);

    Optional<Enrollment> findByStudentAndSection_Course(User student, com.academic.platform.model.Course course);
}
