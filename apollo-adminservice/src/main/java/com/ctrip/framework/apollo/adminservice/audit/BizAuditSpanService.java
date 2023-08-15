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
package com.ctrip.framework.apollo.adminservice.audit;

import com.ctrip.framework.apollo.audit.constants.ApolloAuditHttpHeader;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class BizAuditSpanService implements ApolloAuditSpanService {


  @Override
  public ApolloAuditSpanContext tryToGetParentSpanContext() {
    ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if(servletRequestAttributes == null) {
      return null;
    }
    HttpServletRequest request = servletRequestAttributes.getRequest();
    String traceId = request.getHeader(ApolloAuditHttpHeader.TRACE_ID);
    String spanId = request.getHeader(ApolloAuditHttpHeader.SPAN_ID);
    String operator = request.getHeader(ApolloAuditHttpHeader.OPERATOR);
    if(Objects.isNull(traceId) || Objects.isNull(spanId) || Objects.isNull(operator)){
      return null;
    } else {
      ApolloAuditSpanContext context = new ApolloAuditSpanContext(traceId, spanId);
      context.setOperator(operator);
      return context;
    }
  }

  @Override
  public String getOperator() {
    ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if(servletRequestAttributes == null) {
      return "anonymous";
    }
    HttpServletRequest request = servletRequestAttributes.getRequest();
    return request.getHeader(ApolloAuditHttpHeader.OPERATOR);
  }
}
