package com.academic.platform.repository;

import com.academic.platform.model.PlacementDrive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlacementDriveRepository extends JpaRepository<PlacementDrive, Long> {
    List<PlacementDrive> findAllByOrderByDriveDateDesc();
}

