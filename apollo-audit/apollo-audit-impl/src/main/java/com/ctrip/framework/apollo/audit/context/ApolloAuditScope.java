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

import java.io.IOException;

public class ApolloAuditScope implements AutoCloseable {

  private final ApolloAuditScopeManager manager;
  private final ApolloAuditSpanContext activeSpanContext;
  private final ApolloAuditScope hangUp;
  private ApolloAuditSpanContext lastSpanContext;

  public ApolloAuditScope(ApolloAuditSpanContext activate, ApolloAuditScopeManager manager) {
    this.hangUp = manager.getScope();
    this.activeSpanContext = activate;
    this.manager = manager;
    this.lastSpanContext = null;
  }

  public ApolloAuditSpanContext activeContext() {
    return activeSpanContext;
  }

  @Override
  public void close() throws IOException {
    // closing span become parent-scope's last span
    if (hangUp != null) {
      hangUp.lastSpanContext = this.activeSpanContext;
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
