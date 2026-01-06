package com.cbrviewer.service;

import com.cbrviewer.model.AuditLog;
import com.cbrviewer.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(Long userId, String action, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog(userId, action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        // TODO: Extract IP address from HttpServletRequest if available
        auditLogRepository.save(log);
    }

    public List<AuditLog> getUserActivity(Long userId, int limit) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
            .getContent();
    }
}
