package com.ctrip.framework.apollo.audit.repository;

import com.ctrip.framework.apollo.audit.entity.AuditLog;
import com.ctrip.framework.apollo.audit.entity.AuditLogDataInfluence;
import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

public interface AuditLogDataInfluenceRepository extends PagingAndSortingRepository<AuditLogDataInfluence, Long> {

  List<AuditLogDataInfluence> findBySpanId(String spanId);
}
