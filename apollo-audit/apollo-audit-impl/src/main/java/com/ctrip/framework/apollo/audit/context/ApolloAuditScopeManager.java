package com.ctrip.framework.apollo.audit.context;

import java.io.IOException;

public class ApolloAuditScopeManager {

  private ApolloAuditScope scope;

  public ApolloAuditScopeManager(){}

  public ApolloAuditScope activate(ApolloAuditSpanContext spanContext){
    scope = new ApolloAuditScope(spanContext, this);
    return scope;
  }
  public void deactivate() throws IOException {
    scope.close();
  }

  public ApolloAuditSpanContext activeSpanContext(){
    return scope == null ? null : scope.activeContext();
  }

  public ApolloAuditScope getScope() {
    return scope;
  }

  public void setScope(ApolloAuditScope scope) {
    this.scope = scope;
  }
}
