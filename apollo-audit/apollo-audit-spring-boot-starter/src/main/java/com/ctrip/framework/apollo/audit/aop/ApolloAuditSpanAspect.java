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
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class ApolloAuditSpanAspect {

  private final ApolloAuditLogApi api;

  public ApolloAuditSpanAspect(ApolloAuditLogApi api) {
    this.api = api;
  }

  // get parameters with @ApolloAuditLogDataInfluence
  static List<Object> getDataInfluenceList(ProceedingJoinPoint pjp) {
    Object[] args = pjp.getArgs();
    List<Object> dataInfluenceList = new ArrayList<>();
    MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
    Method method = methodSignature.getMethod();
    Parameter[] parameters = method.getParameters();

    for (int i = 0; i < parameters.length; i++) {
      Annotation[] parameterAnnotations = parameters[i].getAnnotations();
      for (Annotation annotation : parameterAnnotations) {
        if (annotation instanceof ApolloAuditLogDataInfluence) {
          dataInfluenceList.addAll(toRealList(args[i]));
        }
      }
    }
    return dataInfluenceList;
  }

  // if obj is list, split to real list
  static List<Object> toRealList(Object obj) {
    if (Objects.isNull(obj)) {
      return Collections.emptyList();
    }
    if (obj instanceof Collection) {
      Collection<?> collection = (Collection<?>) obj;
      return new ArrayList<>(collection);
    } else {
      return Collections.singletonList(obj);
    }
  }

  @Pointcut("@annotation(auditLog)")
  public void setAuditSpan(ApolloAuditLog auditLog) {
  }

  @Around(value = "setAuditSpan(auditLog)")
  public Object around(ProceedingJoinPoint pjp, ApolloAuditLog auditLog) throws Throwable {

    List<Object> dataInfluenceList = getDataInfluenceList(pjp);
    Object returnVal = null;

    try (AutoCloseable scope = api.appendSpan(auditLog.type(), auditLog.name(),
        auditLog.description())) {
      dataInfluenceList.forEach(e -> api.appendDataInfluenceWrapper(e.getClass()));
      returnVal = pjp.proceed();
    }
    return returnVal;
  }

}
