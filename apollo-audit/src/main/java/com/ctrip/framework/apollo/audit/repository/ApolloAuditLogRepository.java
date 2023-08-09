package com.ctrip.framework.apollo.audit.repository;

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface ApolloAuditLogRepository extends PagingAndSortingRepository<ApolloAuditLog, Long> {

  List<ApolloAuditLog> findByTraceId(@Param("traceId") String traceId);
}
