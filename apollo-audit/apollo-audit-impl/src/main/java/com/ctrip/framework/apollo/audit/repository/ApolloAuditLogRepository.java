package com.ctrip.framework.apollo.audit.repository;

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ApolloAuditLogRepository extends PagingAndSortingRepository<ApolloAuditLog, Long> {

  List<ApolloAuditLog> findByTraceId(String traceId);
}
