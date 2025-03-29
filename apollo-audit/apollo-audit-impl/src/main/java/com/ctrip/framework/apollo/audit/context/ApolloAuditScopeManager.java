/*
 * Copyright 2024 Apollo Authors
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

public class ApolloAuditScopeManager {

  private ApolloAuditScope scope;

  public ApolloAuditScopeManager() {
  }

  public ApolloAuditScope activate(ApolloAuditSpan span) {
    setScope(new ApolloAuditScope(span, this));
    return getScope();
  }

  public void deactivate() throws IOException {
    getScope().close();
  }

  public ApolloAuditSpan activeSpan() {
    return getScope() == null ? null : getScope().activeSpan();
  }

  public ApolloAuditScope getScope() {
    return scope;
  }

  public void setScope(ApolloAuditScope scope) {
    this.scope = scope;
  }
}
