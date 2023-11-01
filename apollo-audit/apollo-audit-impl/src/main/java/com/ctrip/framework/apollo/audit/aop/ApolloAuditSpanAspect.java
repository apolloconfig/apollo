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
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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
      Object[] args = pjp.getArgs();
      Method method = findMethod(pjp.getTarget().getClass(), pjp.getSignature().getName());
      for (int i = 0; i < args.length; i++) {
        Object arg = args[i];
        Annotation[] annotations = method.getParameterAnnotations()[i];
        if (Arrays.stream(annotations).anyMatch(anno -> anno instanceof ApolloAuditLogDataInfluence)) {
          String entityName = null;
          String fieldName = null;
          for(int j = 0; j < annotations.length; j++) {
            if(annotations[j] instanceof ApolloAuditLogDataInfluenceTable) {
              entityName = ((ApolloAuditLogDataInfluenceTable) annotations[j]).tableName();
            }
            if(annotations[j] instanceof ApolloAuditLogDataInfluenceTableField) {
              fieldName = ((ApolloAuditLogDataInfluenceTableField) annotations[j]).fieldName();
            }
          }
          if (entityName != null && fieldName != null) {
            // if arg is a collection
            if (arg instanceof Collection) {
              for (Object o : ((Collection<?>) arg).toArray()) {
                String matchedValue = String.valueOf(o);
                api.appendDataInfluence(entityName, "AnyMatched", fieldName, matchedValue);
              }
            }
            else {
              String matchedValue = String.valueOf(arg);
              api.appendDataInfluence(entityName, "AnyMatched", fieldName, matchedValue);
            }
          }
        }
      }
      return pjp.proceed();
    }
  }

  Method findMethod(Class<?> clazz, String methodName) {
    for (Method method : clazz.getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    return null;
  }

}
