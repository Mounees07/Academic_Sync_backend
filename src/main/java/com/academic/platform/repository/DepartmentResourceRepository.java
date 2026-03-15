package com.academic.platform.repository;

import com.academic.platform.model.DepartmentResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentResourceRepository extends JpaRepository<DepartmentResource, Long> {

    List<DepartmentResource> findByDepartmentIgnoreCase(String department);

    List<DepartmentResource> findByDepartmentIgnoreCaseAndResourceType(String department, String resourceType);

    List<DepartmentResource> findByDepartmentIgnoreCaseAndStatus(String department, String status);
}
