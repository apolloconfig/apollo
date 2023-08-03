package com.ctrip.framework.apollo.audit.aop;

import com.ctrip.framework.apollo.audit.context.AuditScope;
import com.ctrip.framework.apollo.audit.context.AuditSpan;
import com.ctrip.framework.apollo.audit.context.AuditSpanContext;
import com.ctrip.framework.apollo.audit.context.AuditTracer;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.constants.Carrier;
import com.ctrip.framework.apollo.audit.service.AuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditLogRepository;
import javax.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Aspect
public class AuditSpanAspect {

  private final AuditTracer tracer;
  //private final AuditLogService auditLogService;

  public AuditSpanAspect(AuditTracer tracer){
    this.tracer = tracer;
    //this.auditLogService = auditLogService;
  }

  @Pointcut("@annotation(auditEvent)")
  public void setAuditSpan(ApolloAuditLog auditEvent){}

  @Around(value = "setAuditSpan(auditEvent)")
  public Object around(ProceedingJoinPoint pjp, ApolloAuditLog auditEvent) throws Throwable {

    System.out.println("AuditEvent Caught!!!");

    AuditSpanContext activeSpanContext = tracer.scopeManager().activeSpanContext();
    if (activeSpanContext == null) {
      //this is a root span
      System.out.println("Root Span Caught");
      ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      HttpServletRequest request = servletRequestAttributes.getRequest();

      String val = request.getHeader(Carrier.HEADER_NAME);
      activeSpanContext = tracer.inject(val);
    }
    Object returnVal = null;

    AuditSpan span = tracer.buildSpan(auditEvent.type(), auditEvent.name())
        .asChildOf(activeSpanContext)
        .description(auditEvent.description())
        .followsFrom(getFollowSFromId())
        .build();

    try (AuditScope scope = tracer.scopeManager().activate(span.getSpanContext())) {
      span.log();
      //System.out.println(auditLogService.create(span));

      returnVal = pjp.proceed();

      System.out.println(returnVal);


    } catch (Exception e) {
      e.printStackTrace();
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

}
