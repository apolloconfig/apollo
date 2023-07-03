package com.ctrip.framework.apollo.audit;

import java.util.List;

public class AuditLogEvent<T> {
  private List<T> influencedList;
  private String operator;
  private String ip;


  public AuditLogEvent<T> setList(List<T> list){
    this.influencedList = list;
    return this;
  }

  public AuditLogEvent<T> setOperator(String operator){
    this.operator = operator;
    return this;
  }

  public AuditLogEvent<T> setIp(String ip){
    this.ip = ip;
    return this;
  }

}
