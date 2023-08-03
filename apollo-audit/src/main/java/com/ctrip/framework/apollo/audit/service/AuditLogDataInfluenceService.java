package com.ctrip.framework.apollo.audit.service;

import com.ctrip.framework.apollo.audit.repository.AuditLogDataInfluenceRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogDataInfluenceService {

  private final AuditLogDataInfluenceRepository dataInfluenceRepository;


  public AuditLogDataInfluenceService(AuditLogDataInfluenceRepository dataInfluenceRepository) {
    this.dataInfluenceRepository = dataInfluenceRepository;
  }
}
