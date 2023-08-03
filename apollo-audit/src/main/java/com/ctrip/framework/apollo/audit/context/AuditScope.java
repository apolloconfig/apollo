package com.ctrip.framework.apollo.audit.context;

import java.io.Closeable;
import java.io.IOException;

public class AuditScope implements Closeable {

  private final AuditScopeManager manager;
  private final AuditSpanContext activate;
  private final AuditScope hangUp;
  private AuditSpanContext lastSpanContext;

  public AuditScope(AuditSpanContext activate, AuditScopeManager manager) {
    this.hangUp = manager.getScope();
    this.activate = activate;
    this.manager = manager;
    this.lastSpanContext = null;
  }

  public AuditSpanContext activeContext(){
    return activate;
  }

  @Override
  public void close() throws IOException {
    // 将要关闭的span成为 挂起的scope 中的"上一个span"
    if(hangUp != null) {
      hangUp.lastSpanContext = this.activate;
    }
    this.manager.setScope(hangUp);
  }

  public AuditSpanContext getLastSpanContext() {
    return lastSpanContext;
  }

  public void setLastSpanContext(AuditSpanContext lastSpanContext) {
    this.lastSpanContext = lastSpanContext;
  }
}
