/*
 * Copyright 2023 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.audit.context;

public class ApolloAuditScope implements AutoCloseable {

  private final ApolloAuditScopeManager manager;

  private ApolloAuditSpan activeSpan;
  private ApolloAuditScope hangUp;
  private String lastSpanId;

  public ApolloAuditScope(ApolloAuditSpan activeSpan, ApolloAuditScopeManager manager) {
    this.hangUp = manager.getScope();
    this.activeSpan = activeSpan;
    this.manager = manager;
    this.lastSpanId = null;
  }

  public ApolloAuditSpan active() {
    return this.activeSpan;
  }

  @Override
  public void close(){
    // closing span become parent-scope's last span
    if (hangUp != null) {
      hangUp.lastSpanId = this.activeSpan.spanId();
    }
    this.manager.setScope(hangUp);
  }

  public ApolloAuditScopeManager getManager() {
    return manager;
  }

  public ApolloAuditSpan getActiveSpan() {
    return activeSpan;
  }

  public void setActiveSpan(ApolloAuditSpan activeSpan) {
    this.activeSpan = activeSpan;
  }

  public ApolloAuditScope getHangUp() {
    return hangUp;
  }

  public void setHangUp(ApolloAuditScope hangUp) {
    this.hangUp = hangUp;
  }

  public String getLastSpanId() {
    return lastSpanId;
  }

  public void setLastSpanId(String lastSpanId) {
    this.lastSpanId = lastSpanId;
  }
}
