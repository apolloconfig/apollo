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

import com.ctrip.framework.apollo.audit.api.ApolloAuditEntityWrapper;
import java.util.HashMap;
import java.util.Map;

public class ApolloAuditSpanContext {

  private String traceId;

  private String spanId;

  private String operator;

  private Map<Class<?>, ApolloAuditEntityWrapper> wrapperMap = new HashMap<>();

  public ApolloAuditSpanContext(String traceId, String spanId) {
    this.traceId = traceId;
    this.spanId = spanId;
  }

  public ApolloAuditSpanContext(String traceId, String spanId, String operator) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.operator = operator;
  }

  public void putWrapper(Class<?> clazz, ApolloAuditEntityWrapper wrapper) {
    wrapperMap.put(clazz, wrapper);
  }

  public ApolloAuditEntityWrapper getWrapper (Class<?> clazz) {
    if(wrapperMap.containsKey(clazz)) {
      return wrapperMap.get(clazz);
    }
    return null;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getSpanId() {
    return spanId;
  }

  public void setSpanId(String spanId) {
    this.spanId = spanId;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

}
