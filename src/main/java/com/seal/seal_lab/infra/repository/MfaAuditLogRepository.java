package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.MfaAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MfaAuditLogRepository extends JpaRepository<MfaAuditLog, Long> {

    @Query("""
            select log
            from MfaAuditLog log
            where (:loginId is null or lower(log.loginId) like lower(concat('%', :loginId, '%')))
              and (:flowType is null or log.flowType = :flowType)
              and (:resultType is null or log.resultType = :resultType)
              and (:createdFrom is null or log.createdAt >= :createdFrom)
              and (:createdTo is null or log.createdAt <= :createdTo)
            order by log.createdAt desc
            """)
    List<MfaAuditLog> searchAuditLogs(@Param("loginId") String loginId,
                                      @Param("flowType") MfaAuditLog.FlowType flowType,
                                      @Param("resultType") MfaAuditLog.ResultType resultType,
                                      @Param("createdFrom") LocalDateTime createdFrom,
                                      @Param("createdTo") LocalDateTime createdTo,
                                      Pageable pageable);
}
