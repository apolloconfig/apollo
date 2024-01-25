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

import com.ctrip.framework.apollo.audit.constants.ApolloAuditConstants;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditOperatorSupplier;
import java.util.Objects;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class ApolloAuditTraceContext {

  private final ApolloAuditOperatorSupplier operatorSupplier;

  public ApolloAuditTraceContext(ApolloAuditOperatorSupplier operatorSupplier) {
    this.operatorSupplier = operatorSupplier;
  }

  // if not get one, create one and re-get it
  public ApolloAuditTracer tracer() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes != null) {
      Object tracer = requestAttributes.getAttribute(ApolloAuditConstants.TRACER,
          RequestAttributes.SCOPE_REQUEST);
      if (tracer != null) {
        return ((ApolloAuditTracer) tracer);
      } else {
        ApolloAuditTracer newTracer = new ApolloAuditTracer(new ApolloAuditScopeManager(), operatorSupplier);
        setTracer(newTracer);
        return newTracer;
      }
    }
    return null;
  }

  void setTracer(ApolloAuditTracer tracer) {
    if (Objects.nonNull(RequestContextHolder.getRequestAttributes())) {
      RequestContextHolder.getRequestAttributes()
          .setAttribute(ApolloAuditConstants.TRACER, tracer, RequestAttributes.SCOPE_REQUEST);
    }
  }


}
