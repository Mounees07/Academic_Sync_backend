package com.academic.platform.repository;

import com.academic.platform.model.User;
import com.academic.platform.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByFirebaseUid(String firebaseUid);

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleIn(List<Role> roles);

    // Mentor moved to StudentDetails
    List<User> findByStudentDetails_Mentor_FirebaseUid(String mentorUid);

    // Department moved to StudentDetails
    List<User> findByStudentDetails_Department(String department);

    List<User> findByStudentDetails_DepartmentAndStudentDetails_Semester(String department, Integer semester);

    List<User> findByStudentDetails_DepartmentAndRoleIn(String department, List<Role> roles);

    List<User> findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(String department, List<Role> roles);

    Optional<User> findByStudentDetails_RollNumber(String rollNumber);

    List<User> findByStudentDetails_RollNumberBetween(String start, String end);

    long countByRole(Role role);

    long countByRoleIn(List<Role> roles);

    long countByRoleAndGender(Role role, String gender);
}
