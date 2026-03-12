package com.academic.platform.repository;

import com.academic.platform.model.PlacementProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PlacementProfileRepository extends JpaRepository<PlacementProfile, Long> {

    // ✅ Use firebaseUid — the actual field name on User entity
    Optional<PlacementProfile> findByStudentFirebaseUid(String firebaseUid);
}
