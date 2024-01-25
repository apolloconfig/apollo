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
package com.ctrip.framework.apollo.audit.aop;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.constants.ApolloAuditConstants;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cglib.core.ReflectUtils;

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
    try (AutoCloseable scope = api.appendAuditLog(auditLog.type(), opName,
        auditLog.description())) {
      Object proceed = pjp.proceed();
      auditDataInfluenceArg(pjp);
      return proceed;
    }
  }

  void auditDataInfluenceArg(ProceedingJoinPoint pjp) {
    Method method = findMethod(pjp);
    if (Objects.isNull(method)) {
      return;
    }
    Object[] args = pjp.getArgs();
    for (int i = 0; i < args.length; i++) {
      Object arg = args[i];
      Annotation[] annotations = method.getParameterAnnotations()[i];


      boolean needAudit = false;
      String entityName = null;
      String fieldName = null;

      for (Annotation annotation : annotations) {
        if (annotation instanceof ApolloAuditLogDataInfluence) {
          needAudit = true;
        }
        if (annotation instanceof ApolloAuditLogDataInfluenceTable) {
          entityName = ((ApolloAuditLogDataInfluenceTable) annotation).tableName();
        }
        if (annotation instanceof ApolloAuditLogDataInfluenceTableField) {
          fieldName = ((ApolloAuditLogDataInfluenceTableField) annotation).fieldName();
        }
      }

      if (needAudit) {
        parseArgAndAppend(entityName, fieldName, arg);
      }
    }
  }

  Method findMethod(ProceedingJoinPoint pjp) {
    Class<?> clazz = pjp.getTarget().getClass();
    Signature pjpSignature = pjp.getSignature();
    String methodName = pjp.getSignature().getName();
    Class[] parameterTypes = null;
    if (pjpSignature instanceof MethodSignature) {
      parameterTypes = ((MethodSignature) pjpSignature).getParameterTypes();
    }
    try {
      Method method = ReflectUtils.findDeclaredMethod(clazz, methodName, parameterTypes);
      return method;
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  void parseArgAndAppend(String entityName, String fieldName, Object arg) {
    if (entityName == null || fieldName == null || arg == null) {
      return;
    }

    if (arg instanceof Collection) {
      for (Object o : (Collection<?>) arg) {
        String matchedValue = String.valueOf(o);
        api.appendDataInfluence(entityName, ApolloAuditConstants.ANY_MATCHED_ID, fieldName, matchedValue);
      }
    } else {
      String matchedValue = String.valueOf(arg);
      api.appendDataInfluence(entityName, ApolloAuditConstants.ANY_MATCHED_ID, fieldName, matchedValue);
    }
  }
}
