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
package com.ctrip.framework.apollo.audit.api;

import com.ctrip.framework.apollo.audit.annotation.OpType;
import java.util.List;

public interface ApolloAuditLogRecordApi {

  AutoCloseable appendSpan(OpType type, String name);

  AutoCloseable appendSpan(OpType type, String name, String description);

  void appendDataInfluenceWrapper(Class<?> clazz);

  void appendDataInfluenceWrapper(Class<?> clazz, ApolloAuditEntityWrapper wrapper);

  void appendSingleDataInfluence(String entityId, String entityName, String fieldName,
      String fieldOldValue, String fieldNewValue);

  <T> void appendDataInfluences(List<T> entities, boolean isDeleted, Class<?> clazz);

  <T> void appendDataInfluences(List<T> entities, boolean isDeleted, ApolloAuditEntityWrapper wrapper);

}
