package com.academic.platform.repository;

import com.academic.platform.model.PlacementCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlacementCompanyRepository extends JpaRepository<PlacementCompany, Long> {
    List<PlacementCompany> findAllByOrderByCompanyNameAsc();
}

