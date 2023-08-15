package com.ctrip.framework.apollo.audit.aop;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogDataInfluenceProducer;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScope;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class ApolloAuditSpanAspect {

  private final ApolloAuditTracer tracer;
  private final ApolloAuditLogService logService;
  private final ApolloAuditSpanService spanService;
  private final ApolloAuditLogDataInfluenceProducer dataInfluenceProducer;

  public ApolloAuditSpanAspect(ApolloAuditTracer tracer, ApolloAuditLogService logService,
      ApolloAuditSpanService spanService,
      ApolloAuditLogDataInfluenceProducer dataInfluenceProducer) {
    this.tracer = tracer;
    this.logService = logService;
    this.spanService = spanService;
    this.dataInfluenceProducer = dataInfluenceProducer;
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

    ApolloAuditSpanContext parentSpanContext = getParentContext();
    ApolloAuditSpan span = generateSpan(parentSpanContext, auditLog);

    List<Object> dataInfluenceList = getDataInfluenceList(pjp);
    Object returnVal = null;

    try (ApolloAuditScope scope = tracer.scopeManager().activate(span.getSpanContext())) {
      span.log();
      returnVal = pjp.proceed();
      // process in the scope
      if (auditLog.attachReturnValue()) {
        dataInfluenceList.addAll(toRealList(returnVal));
      }
      logService.logSpan(span);
      if (auditLog.autoCollectDataInfluence()) {
        switch (auditLog.type()) {
          case CREATE:
          case UPDATE:// auto collect could only capture the newest entity status
            // default record return value as the new changed data
            for (Object o : dataInfluenceList) {
              dataInfluenceProducer.appendCreateDataInfluences(o);
            }
            break;
          case DELETE:
            for (Object o : dataInfluenceList) {
              dataInfluenceProducer.appendDeleteDataInfluences(o);
            }
            break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return returnVal;
  }

  String getFollowSFromId() {
    if (tracer.scopeManager().getScope() == null) {
      return null;
    }
    if (tracer.scopeManager().getScope().getLastSpanContext() == null) {
      return null;
    }
    return tracer.scopeManager().getScope().getLastSpanContext().getSpanId();
  }

  ApolloAuditSpanContext getParentContext() {
    ApolloAuditSpanContext parentContext = tracer.scopeManager().activeSpanContext();
    if (parentContext == null) {
      parentContext = spanService.tryToGetParentSpanContext();
    }
    return parentContext;
  }

  // depend on parentSpanContext
  ApolloAuditSpan generateSpan(ApolloAuditSpanContext parentSpanContext, ApolloAuditLog auditLog) {
    if (parentSpanContext == null) {
      return tracer.buildSpan(auditLog.type(), auditLog.name())
          .asRootSpan(spanService.getOperator()).description(auditLog.description())
          .followsFrom(getFollowSFromId()).build();
    } else {
      return tracer.buildSpan(auditLog.type(), auditLog.name()).asChildOf(parentSpanContext)
          .description(auditLog.description()).followsFrom(getFollowSFromId()).build();
    }
  }

}
