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

import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.constants.ApolloAuditConstants;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditOperatorSupplier;
import com.ctrip.framework.apollo.audit.util.ApolloAuditUtil;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ApolloAuditTracer {

  private final ApolloAuditScopeManager manager;
  private final ApolloAuditOperatorSupplier operatorSupplier;

  public ApolloAuditTracer(ApolloAuditScopeManager manager,
      ApolloAuditOperatorSupplier operatorSupplier) {
    this.manager = manager;
    this.operatorSupplier = operatorSupplier;
  }

  protected ApolloAuditScopeManager scopeManager() {
    return manager;
  }

  public HttpRequest inject(HttpRequest request) {
    Map<String, List<String>> map = new HashMap<>();
    if (manager.activeSpan() == null) {
      return request;
    }
    map.put(ApolloAuditConstants.TRACE_ID,
        Collections.singletonList(manager.activeSpan().traceId()));
    map.put(ApolloAuditConstants.SPAN_ID,
        Collections.singletonList(manager.activeSpan().spanId()));
    map.put(ApolloAuditConstants.OPERATOR,
        Collections.singletonList(manager.activeSpan().operator()));
    map.put(ApolloAuditConstants.PARENT_ID,
        Collections.singletonList(manager.activeSpan().parentId()));
    map.put(ApolloAuditConstants.FOLLOWS_FROM_ID,
        Collections.singletonList(manager.activeSpan().followsFromId()));

    HttpHeaders headers = request.getHeaders();
    headers.putAll(map);

    return request;
  }

  public ApolloAuditSpan startSpan(OpType type, String name, String description) {
    ApolloAuditSpan activeSpan = getActiveSpan();
    AuditSpanBuilder builder = new AuditSpanBuilder(type, name);
    builder = builder.description(description);
    builder = activeSpan == null ? builder.asRootSpan(operatorSupplier.getOperator())
        : builder.asChildOf(activeSpan);
    String followsFromId = scopeManager().getScope() == null ?
        null : scopeManager().getScope().getLastSpanId();
    builder = builder.followsFrom(followsFromId);

    return builder.build();
  }

  public ApolloAuditScope startActiveSpan(OpType type, String name, String description) {
    ApolloAuditSpan startSpan = startSpan(type, name, description);
    return activate(startSpan);
  }

  public ApolloAuditScope activate(ApolloAuditSpan span) {
    return scopeManager().activate(span);
  }

  private ApolloAuditSpan getActiveSpanFromHttp() {
    ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (servletRequestAttributes == null) {
      return null;
    }
    HttpServletRequest request = servletRequestAttributes.getRequest();
    String traceId = request.getHeader(ApolloAuditConstants.TRACE_ID);
    String spanId = request.getHeader(ApolloAuditConstants.SPAN_ID);
    String operator = request.getHeader(ApolloAuditConstants.OPERATOR);
    String parentId = request.getHeader(ApolloAuditConstants.PARENT_ID);
    String followsFromId = request.getHeader(ApolloAuditConstants.FOLLOWS_FROM_ID);
    if (Objects.isNull(traceId)) {
      return null;
    } else {
      ApolloAuditSpanContext context = new ApolloAuditSpanContext(traceId, spanId, operator, parentId, followsFromId);
      return spanBuilder(null, null).regenerateByContext(context);
    }
  }

  private ApolloAuditSpan getActiveSpanFromContext() {
    return scopeManager().activeSpan();
  }

  public ApolloAuditSpan getActiveSpan() {
    ApolloAuditSpan activeSpan = getActiveSpanFromContext();
    if (activeSpan != null) {
      return activeSpan;
    }
    activeSpan = getActiveSpanFromHttp();
    // might be null, root span generate should be done in other place
    return activeSpan;
  }

  public AuditSpanBuilder spanBuilder(OpType type, String name) {
    return new AuditSpanBuilder(type, name);
  }

  public static class AuditSpanBuilder {

    private final OpType opType;
    private final String opName;
    private String spanId;
    private String traceId;
    private String operator;
    private String parentId;
    private String followsFromId;
    private String description;

    public AuditSpanBuilder(OpType type, String name) {
      opType = type;
      opName = name;
    }

    public AuditSpanBuilder asChildOf(ApolloAuditSpan parent) {
      traceId = parent.traceId();
      operator = parent.operator();
      parentId = parent.spanId();
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

    public ApolloAuditSpan regenerateByContext(ApolloAuditSpanContext val) {
      ApolloAuditSpan span = new ApolloAuditSpan();
      span.setContext(val);
      return span;
    }

    public ApolloAuditSpan build() {
      ApolloAuditSpan span = new ApolloAuditSpan();
      spanId = ApolloAuditUtil.generateId();
      ApolloAuditSpanContext context = new ApolloAuditSpanContext(traceId, spanId, operator,
          parentId, followsFromId);
      span.setContext(context);
      span.setDescription(description);
      span.setOpName(opName);
      span.setOpType(opType);
      span.setStartTime(new Date());
      return span;
    }
  }
}
