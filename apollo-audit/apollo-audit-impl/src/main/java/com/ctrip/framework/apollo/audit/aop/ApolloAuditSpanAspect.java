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
package com.ctrip.framework.apollo.audit.aop;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
public class ApolloAuditSpanAspect {

  private final ApolloAuditLogApi api;

  public ApolloAuditSpanAspect(ApolloAuditLogApi api) {
    this.api = api;
  }

  @Pointcut("@annotation(auditLog)")
  public void setAuditSpan(ApolloAuditLog auditLog) {
  }

  @Around(value = "setAuditSpan(auditLog)")
  public Object around(ProceedingJoinPoint pjp, ApolloAuditLog auditLog) throws Throwable {
    String opName = auditLog.name();
    if (opName.equals("") && RequestContextHolder.getRequestAttributes() != null) {
      ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      opName = servletRequestAttributes.getRequest().getRequestURI();
    }
    try (AutoCloseable scope = api.appendAuditLog(auditLog.type(), opName,
        auditLog.description())) {
      return pjp.proceed();
    }
  }

}
