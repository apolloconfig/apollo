package com.ctrip.framework.apollo.audit.service;

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogDataInfluenceRepository;
import java.util.List;


public class ApolloAuditLogDataInfluenceService {

  private final ApolloAuditLogDataInfluenceRepository dataInfluenceRepository;

  public ApolloAuditLogDataInfluenceService(
      ApolloAuditLogDataInfluenceRepository dataInfluenceRepository) {
    this.dataInfluenceRepository = dataInfluenceRepository;
  }

  public ApolloAuditLogDataInfluence save(ApolloAuditLogDataInfluence dataInfluence) {
    // protect
    dataInfluence.setId(0);
    return dataInfluenceRepository.save(dataInfluence);
  }

  public void batchSave(List<ApolloAuditLogDataInfluence> dataInfluences) {
    dataInfluenceRepository.saveAll(dataInfluences);
  }

  public List<ApolloAuditLogDataInfluence> findBySpanId(String spanId) {
    return dataInfluenceRepository.findBySpanId(spanId);
  }


}
