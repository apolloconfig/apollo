package com.ctrip.framework.apollo.audit.util;

import java.util.UUID;

public class ApolloAuditUtil {

  public static String generateId(){
    return UUID.randomUUID().toString();
  }



}
