package com.academic.platform.repository;

import com.academic.platform.model.InternalMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InternalMarkRepository extends JpaRepository<InternalMark, Long> {

    // ✅ Use firebaseUid — the actual field name on User entity
    List<InternalMark> findByStudentFirebaseUidOrderBySubjectName(String firebaseUid);

    List<InternalMark> findByStudentFirebaseUidAndSemester(String firebaseUid, int semester);

    Optional<InternalMark> findByStudentFirebaseUidAndSectionId(String firebaseUid, Long sectionId);

    List<InternalMark> findBySectionId(Long sectionId);
}
