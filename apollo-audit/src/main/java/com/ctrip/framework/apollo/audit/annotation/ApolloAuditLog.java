package com.ctrip.framework.apollo.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApolloAuditLog {
  OpType type();
  String name();
  String description() default "no description";
  boolean logData() default true;
}
