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

import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.constants.ApolloAuditHttpHeader;
import com.ctrip.framework.apollo.audit.util.ApolloAuditUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApolloAuditTracer {

  private final ApolloAuditScopeManager manager;

  public ApolloAuditTracer(ApolloAuditScopeManager manager) {
    this.manager = manager;
  }

  public ApolloAuditScopeManager scopeManager() {
    return manager;
  }

  public Map<String, List<String>> extract() {

    if(manager.activeSpanContext() == null) {
      return null;
    }
    Map<String, List<String>> map = new HashMap<>();
    map.put(ApolloAuditHttpHeader.TRACE_ID,
        Collections.singletonList(manager.activeSpanContext().getTraceId()));
    map.put(ApolloAuditHttpHeader.SPAN_ID,
        Collections.singletonList(manager.activeSpanContext().getSpanId()));
    map.put(ApolloAuditHttpHeader.OPERATOR,
        Collections.singletonList(manager.activeSpanContext().getOperator()));
    return map;
  }

  public ApolloAuditSpanContext inject(Map<String, String> map) {
    if (map.isEmpty()) {
      return null;
    }
    ApolloAuditSpanContext context = new ApolloAuditSpanContext(
        map.get(ApolloAuditHttpHeader.TRACE_ID), ApolloAuditHttpHeader.SPAN_ID);
    context.setOperator(ApolloAuditHttpHeader.OPERATOR);
    return context;
  }

  public AuditSpanBuilder buildSpan(OpType type, String name) {
    return new AuditSpanBuilder(type, name);
  }

  public static class AuditSpanBuilder {

    private String traceId;
    private String spanId;
    private String operator;

    private String parentId;
    private String followsFromId;
    private final OpType opType;
    private final String opName;
    private String description;

    public AuditSpanBuilder(OpType type, String name) {
      opType = type;
      opName = name;
    }

    public AuditSpanBuilder asChildOf(ApolloAuditSpanContext parentContext) {
      traceId = parentContext.getTraceId();
      operator = parentContext.getOperator();
      parentId = parentContext.getSpanId();
      return this;
    }

    public AuditSpanBuilder asRootSpan(String operator) {
      traceId = ApolloAuditUtil.generateId();
      this.operator = operator;
      return this;
    }

    public AuditSpanBuilder followsFrom(String id) {
      this.followsFromId = id;
      return this;
    }

    public AuditSpanBuilder description(String val) {
      this.description = val;
      return this;
    }

    public AuditSpanBuilder context(ApolloAuditSpanContext val) {
      this.traceId = val.getTraceId();
      this.operator = val.getOperator();
      return this;
    }

    public ApolloAuditSpan build() {
      spanId = ApolloAuditUtil.generateId();
      ApolloAuditSpanContext context = new ApolloAuditSpanContext(traceId, spanId);
      context.setOperator(operator);

      ApolloAuditSpan span = new ApolloAuditSpan();
      span.setSpanContext(context);
      span.setDescription(description);
      span.setFollowsFromId(followsFromId);
      span.setParentId(parentId);
      span.setOpName(opName);
      span.setOpType(opType);
      return span;
    }
  }
}
