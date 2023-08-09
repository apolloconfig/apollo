package com.ctrip.framework.apollo.audit.aop;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScope;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
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

  private final ApolloAuditTracer tracer;
  private final ApolloAuditLogService logService;
  private final ApolloAuditSpanService spanService;

  public ApolloAuditSpanAspect(ApolloAuditTracer tracer, ApolloAuditLogService logService,
      ApolloAuditSpanService spanService) {
    this.tracer = tracer;
    this.logService = logService;
    this.spanService = spanService;
  }

  @Pointcut("@annotation(auditLog)")
  public void setAuditSpan(ApolloAuditLog auditLog){}

  @Around(value = "setAuditSpan(auditLog)")
  public Object around(ProceedingJoinPoint pjp, ApolloAuditLog auditLog) throws Throwable {

    ApolloAuditSpanContext parentSpanContext = getParentContext();
    ApolloAuditSpan span = generateSpan(parentSpanContext, auditLog);

    List<Object> dataInfluenceList = getDataInfluenceList(pjp);
    Object returnVal = null;

    try (ApolloAuditScope scope = tracer.scopeManager().activate(span.getSpanContext())) {
      span.log();
      returnVal = pjp.proceed();
    } catch (Exception e) {
      e.printStackTrace();
    }

    if(Objects.nonNull(returnVal) && auditLog.logData()){
      logService.logSpan(span);
      switch (auditLog.type()) {
        case CREATE: // default record return value as the new changed data
          logService.logCreateDataInfluence(span.id(), span.operator(), toRealList(returnVal));
          break;
        case DELETE: // default record marked(@AALDataInfluence) parameter as the new changed data
          logService.logDeleteDataInfluence(span.id(), span.operator(), dataInfluenceList);
          break;
        case UPDATE: // param as olds, return value as news
          logService.logUpdateDataInfluence(span.id(), span.operator(), dataInfluenceList, toRealList(returnVal));
          break;
      }
    }
    return returnVal;
  }

  String getFollowSFromId(){
    if(tracer.scopeManager().getScope() == null){
      return null;
    }
    if(tracer.scopeManager().getScope().getLastSpanContext() == null){
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

  ApolloAuditSpan generateSpan(ApolloAuditSpanContext parentSpanContext, ApolloAuditLog auditLog) {
    if(parentSpanContext == null){
      return tracer.buildSpan(auditLog.type(), auditLog.name())
          .asRootSpan(spanService.getOperator())
          .description(auditLog.description())
          .followsFrom(getFollowSFromId())
          .build();
    } else {
      return tracer.buildSpan(auditLog.type(), auditLog.name())
          .asChildOf(parentSpanContext)
          .description(auditLog.description())
          .followsFrom(getFollowSFromId())
          .build();
    }
  }

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

  static List<Object> toRealList(Object obj) {
    if(obj instanceof Collection) {
      Collection<?> collection = (Collection<?>) obj;
      return new ArrayList<>(collection);
    } else {
      return Collections.singletonList(obj);
    }
  }

}
