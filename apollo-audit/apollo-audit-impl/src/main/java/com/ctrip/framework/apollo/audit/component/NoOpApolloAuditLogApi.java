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
package com.ctrip.framework.apollo.audit.component;

import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.api.ApolloAuditEntityWrapper;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import java.util.List;

public class NoOpApolloAuditLogApi implements ApolloAuditLogApi {

  //do nothing, for default impl

  @Override
  public AutoCloseable appendSpan(OpType type, String name) {
    return appendSpan(type, name, null);
  }

  @Override
  public AutoCloseable appendSpan(OpType type, String name, String description) {
    return () -> {
    };
  }

  @Override
  public void appendSingleDataInfluence(String entityId, String entityName, String fieldName,
      String fieldOldValue, String fieldNewValue) {

  }

  @Override
  public <T> void appendDataInfluencesByManagedClass(List<T> entities, OpType type,
      Class<?> managedClass) {

  }

  @Override
  public <T> void appendDataInfluencesByWrapper(List<T> entities, OpType type,
      ApolloAuditEntityWrapper wrapper) {

  }
}
