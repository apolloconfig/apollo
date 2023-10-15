package com.ctrip.framework.apollo.audit.exception;

public class ApolloAuditEntityBeanDefinitionException extends RuntimeException{
  private String beanClassName; // 自定义属性示例

  public ApolloAuditEntityBeanDefinitionException(String message) {
    super(message);
  }

  public ApolloAuditEntityBeanDefinitionException(String message, Throwable cause) {
    super(message, cause);
  }

  public ApolloAuditEntityBeanDefinitionException(String message, String beanClassName) {
    super(message);
    this.beanClassName = beanClassName;
  }

  public String getBeanClassName() {
    return beanClassName;
  }
}
