package com.ctrip.framework.apollo.portal.audit;

import com.ctrip.framework.apollo.audit.spi.ApolloAuditLogRepository;

public class PortalAuditLogRepository implements ApolloAuditLogRepository {

  @Override
  public void test() {
    System.out.println("PortalAuditLogRepository.test called");
  }
}
