package com.academic.platform.repository;

import com.academic.platform.model.FacultyWorkload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyWorkloadRepository extends JpaRepository<FacultyWorkload, Long> {

    Optional<FacultyWorkload> findByFaculty_FirebaseUid(String firebaseUid);

    Optional<FacultyWorkload> findByFaculty_Id(Long facultyId);

    List<FacultyWorkload> findByFaculty_StudentDetails_DepartmentIgnoreCase(String department);
}
