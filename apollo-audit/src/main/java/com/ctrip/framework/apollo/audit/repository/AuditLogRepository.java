package com.ctrip.framework.apollo.audit.repository;

import com.ctrip.framework.apollo.audit.entity.AuditLog;
import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;


public interface AuditLogRepository extends PagingAndSortingRepository <AuditLog, Long>{

  List<AuditLog> findByTraceId(String traceId);

}
