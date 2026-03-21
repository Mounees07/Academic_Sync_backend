package com.academic.platform.repository;

import com.academic.platform.model.PlacementDriveApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlacementDriveApplicationRepository extends JpaRepository<PlacementDriveApplication, Long> {
    List<PlacementDriveApplication> findByDriveIdOrderByCreatedAtAsc(Long driveId);
    List<PlacementDriveApplication> findByDriveIdAndStatusIn(Long driveId, Collection<String> statuses);
    List<PlacementDriveApplication> findByStudentFirebaseUidOrderByCreatedAtDesc(String studentUid);
    Optional<PlacementDriveApplication> findByDriveIdAndStudentFirebaseUid(Long driveId, String studentUid);
    void deleteByDriveId(Long driveId);
}
