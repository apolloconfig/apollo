package com.ctrip.framework.apollo.audit.context;

import java.io.IOException;
import org.springframework.stereotype.Component;

public class AuditScopeManager {

  private AuditScope scope;

  public AuditScopeManager(){}

  public AuditScope activate(AuditSpanContext spanContext){
    scope = new AuditScope(spanContext, this);
    return scope;
  }
  public void deactivate() throws IOException {
    scope.close();
  }

  public AuditSpanContext activeSpanContext(){
    return scope == null ? null : scope.activeContext();
  }

  public AuditScope getScope() {
    return scope;
  }

  public void setScope(AuditScope scope) {
    this.scope = scope;
  }
}
