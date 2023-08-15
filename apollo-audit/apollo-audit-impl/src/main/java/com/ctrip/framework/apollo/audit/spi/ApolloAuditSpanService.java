package com.ctrip.framework.apollo.audit.spi;

import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;

public interface ApolloAuditSpanService {

  ApolloAuditSpanContext tryToGetParentSpanContext();

  String getOperator();
}
