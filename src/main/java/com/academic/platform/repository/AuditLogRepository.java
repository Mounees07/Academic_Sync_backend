package com.academic.platform.repository;

import com.academic.platform.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrderByTimestampDesc();

    List<AuditLog> findByActorUid(String actorUid);

    List<AuditLog> findByAction(String action);
}
