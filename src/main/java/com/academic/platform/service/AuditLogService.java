package com.academic.platform.service;

import com.academic.platform.model.AuditLog;
import com.academic.platform.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Async
    public void log(String actorUid, String actorEmail, String action, String details, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .actorUid(actorUid)
                .actorEmail(actorEmail)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsByUser(String uid) {
        return auditLogRepository.findByActorUid(uid);
    }
}
