package com.ctrip.framework.apollo.audit.context;

import java.io.Closeable;
import java.io.IOException;

public class ApolloAuditScope implements Closeable {

  private final ApolloAuditScopeManager manager;
  private final ApolloAuditSpanContext activate;
  private final ApolloAuditScope hangUp;
  private ApolloAuditSpanContext lastSpanContext;

  public ApolloAuditScope(ApolloAuditSpanContext activate, ApolloAuditScopeManager manager) {
    this.hangUp = manager.getScope();
    this.activate = activate;
    this.manager = manager;
    this.lastSpanContext = null;
  }

  public ApolloAuditSpanContext activeContext(){
    return activate;
  }

  @Override
  public void close() throws IOException {
    // 将要关闭的span成为 父scope 中的"上一个span"
    if(hangUp != null) {
      hangUp.lastSpanContext = this.activate;
    }
    this.manager.setScope(hangUp);
  }

  public ApolloAuditSpanContext getLastSpanContext() {
    return lastSpanContext;
  }

  public void setLastSpanContext(ApolloAuditSpanContext lastSpanContext) {
    this.lastSpanContext = lastSpanContext;
  }
}
