package com.ctrip.framework.apollo.audit.util;

import org.springframework.security.core.context.SecurityContextHolder;

public class OperationUtil {

  public static String getOperatorByFramework() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }

}
