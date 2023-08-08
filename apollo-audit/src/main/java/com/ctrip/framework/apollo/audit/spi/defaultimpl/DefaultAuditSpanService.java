package com.ctrip.framework.apollo.audit.spi.defaultimpl;

import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;

public class DefaultAuditSpanService implements ApolloAuditSpanService {

  @Override
  public ApolloAuditSpanContext tryToGetParentSpanContext() {
    return null;
  }

  @Override
  public String getOperator() {
    return "anonymous";
  }
}
