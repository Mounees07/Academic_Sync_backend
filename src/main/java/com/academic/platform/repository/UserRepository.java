package com.academic.platform.repository;

import com.academic.platform.model.User;
import com.academic.platform.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByFirebaseUid(String firebaseUid);

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    @Query("""
            SELECT u.firebaseUid, u.fullName, u.email, sd.department, sd.rollNumber, sd.semester,
                   COALESCE(sd.cgpa, sd.gpa)
            FROM User u
            LEFT JOIN u.studentDetails sd
            WHERE u.role = :role
            """)
    List<Object[]> findPlacementStudentSnapshotsByRole(@Param("role") Role role);

    List<User> findByRoleIn(List<Role> roles);

    // Mentor moved to StudentDetails
    List<User> findByStudentDetails_Mentor_FirebaseUid(String mentorUid);

    // Department moved to StudentDetails
    List<User> findByStudentDetails_Department(String department);

    List<User> findByStudentDetails_DepartmentAndStudentDetails_Semester(String department, Integer semester);

    List<User> findByStudentDetails_DepartmentAndRoleIn(String department, List<Role> roles);

    List<User> findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(String department, List<Role> roles);

    /**
     * Find faculty/HOD by department (case-insensitive).
     * Also includes users whose studentDetails has no department set (null or
     * blank)
     * so that faculty who haven't been assigned a department still appear.
     */
    @Query("SELECT u FROM User u LEFT JOIN u.studentDetails sd WHERE u.role IN :roles AND " +
            "(LOWER(sd.department) = LOWER(:department) OR sd.department IS NULL OR sd.department = '')")
    List<User> findFacultyByDepartmentOrNoDepartment(@Param("department") String department,
            @Param("roles") List<Role> roles);

    Optional<User> findByStudentDetails_RollNumber(String rollNumber);

    List<User> findByStudentDetails_RollNumberBetween(String start, String end);

    long countByRole(Role role);

    long countByRoleIn(List<Role> roles);

    long countByRoleAndGender(Role role, String gender);
}
