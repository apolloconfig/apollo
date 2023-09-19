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
import com.ctrip.framework.apollo.audit.constants.ApolloAuditContextConstants;
import com.ctrip.framework.apollo.audit.constants.ApolloAuditHttpHeaderConstants;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScopeManager;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditOperatorSupplier;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ApolloAuditSpanService {

  private final ApolloAuditOperatorSupplier operatorSupplier;

  public ApolloAuditSpanService(ApolloAuditOperatorSupplier operatorSupplier) {
    this.operatorSupplier = operatorSupplier;
  }

  public ApolloAuditTracer getTracer() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes != null) {
      Object tracer = requestAttributes.getAttribute(ApolloAuditContextConstants.TRACER,
          RequestAttributes.SCOPE_REQUEST);
      if (tracer != null) {
        return ((ApolloAuditTracer) tracer);
      } else {
        setTracer(new ApolloAuditTracer(new ApolloAuditScopeManager()));
        return getTracer();
      }
    }
    return null;
  }

  public void setTracer(ApolloAuditTracer tracer) {
    if (Objects.nonNull(RequestContextHolder.getRequestAttributes())) {
      RequestContextHolder.getRequestAttributes()
          .setAttribute(ApolloAuditContextConstants.TRACER, tracer,
              RequestAttributes.SCOPE_REQUEST);
    }
  }

  private ApolloAuditSpanContext getParentSpanContextFromHttp() {
    ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (servletRequestAttributes == null) {
      return null;
    }
    HttpServletRequest request = servletRequestAttributes.getRequest();
    String traceId = request.getHeader(ApolloAuditHttpHeaderConstants.TRACE_ID);
    String spanId = request.getHeader(ApolloAuditHttpHeaderConstants.SPAN_ID);
    String operator = request.getHeader(ApolloAuditHttpHeaderConstants.OPERATOR);
    if (Objects.isNull(traceId) || Objects.isNull(spanId) || Objects.isNull(operator)) {
      return null;
    } else {
      ApolloAuditSpanContext context = new ApolloAuditSpanContext(traceId, spanId);
      context.setOperator(operator);
      return context;
    }
  }

  private ApolloAuditSpanContext getParentSpanContextFromContext() {
    ApolloAuditTracer tracer = getTracer();
    return tracer.scopeManager().activeSpanContext();
  }

  public ApolloAuditSpanContext getParentSpanContext() {
    ApolloAuditSpanContext parentSpanContext = getParentSpanContextFromContext();
    if (parentSpanContext != null) {
      return parentSpanContext;
    }
    parentSpanContext = getParentSpanContextFromHttp();
    // might be null, root span generate should be done in other place
    return parentSpanContext;

  }

  public String getOperator() {
    return operatorSupplier.getOperator();
  }

  String getFollowsFromId() {
    ApolloAuditTracer tracer = getTracer();
    if (tracer.scopeManager().getScope() == null) {
      return null;
    }
    if (tracer.scopeManager().getScope().getLastSpanContext() == null) {
      return null;
    }
    return tracer.scopeManager().getScope().getLastSpanContext().getSpanId();
  }

  // depend on parentSpanContext
  public ApolloAuditSpan generateSpan(OpType type, String name, String description) {
    ApolloAuditTracer tracer = getTracer();
    ApolloAuditSpanContext parentSpanContext = getParentSpanContext();
    if (parentSpanContext == null) {
      return tracer.buildSpan(type, name).asRootSpan(getOperator()).description(description)
          .followsFrom(getFollowsFromId()).build();
    } else {
      return tracer.buildSpan(type, name).asChildOf(parentSpanContext).description(description)
          .followsFrom(getFollowsFromId()).build();
    }
  }
}
