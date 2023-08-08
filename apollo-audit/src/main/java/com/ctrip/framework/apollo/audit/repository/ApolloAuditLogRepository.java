package com.ctrip.framework.apollo.audit.repository;

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ApolloAuditLogRepository extends PagingAndSortingRepository<ApolloAuditLog, Long> {

}
