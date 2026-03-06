package com.academic.platform.repository;

import com.academic.platform.model.Assignment;
import com.academic.platform.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findBySection(Section section);
}
