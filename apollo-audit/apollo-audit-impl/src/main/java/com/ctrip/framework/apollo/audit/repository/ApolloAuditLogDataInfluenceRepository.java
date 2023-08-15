package com.ctrip.framework.apollo.audit.repository;

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ApolloAuditLogDataInfluenceRepository extends PagingAndSortingRepository<ApolloAuditLogDataInfluence, Long> {

  List<ApolloAuditLogDataInfluence> findBySpanId(String spanId);

}
