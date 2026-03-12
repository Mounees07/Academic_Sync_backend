package com.academic.platform.repository;

import com.academic.platform.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ✅ Use firebaseUid — the actual field name on User entity
    List<Notification> findByUserFirebaseUidOrderByCreatedAtDesc(String firebaseUid);

    List<Notification> findByUserFirebaseUidAndIsReadFalseOrderByCreatedAtDesc(String firebaseUid);

    long countByUserFirebaseUidAndIsReadFalse(String firebaseUid);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.firebaseUid = :uid")
    int markAllReadByUserFirebaseUid(@Param("uid") String uid);
}
