package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.TrustOverrideAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TrustOverrideAuditLogRepository extends JpaRepository<TrustOverrideAuditLog, Long> {

    List<TrustOverrideAuditLog> findTop30ByOrderByCreatedAtDesc();

    @Query("""
            select log
            from TrustOverrideAuditLog log
            where (:operatorLoginId is null or lower(log.operatorLoginId) like lower(concat('%', :operatorLoginId, '%')))
              and (:targetLoginId is null or lower(log.targetLoginId) like lower(concat('%', :targetLoginId, '%')))
              and (:actionType is null or log.actionType = :actionType)
              and (:clearContext is null or log.clearContext = :clearContext)
              and (:createdFrom is null or log.createdAt >= :createdFrom)
              and (:createdTo is null or log.createdAt <= :createdTo)
            order by log.createdAt desc
            """)
    List<TrustOverrideAuditLog> searchAuditLogs(@Param("operatorLoginId") String operatorLoginId,
                                                @Param("targetLoginId") String targetLoginId,
                                                @Param("actionType") TrustOverrideAuditLog.ActionType actionType,
                                                @Param("clearContext") Boolean clearContext,
                                                @Param("createdFrom") LocalDateTime createdFrom,
                                                @Param("createdTo") LocalDateTime createdTo,
                                                Pageable pageable);
}
