package com.ctrip.framework.apollo.common.jpa;

/**
 * @author nisiyong
 */
public class H2Function {

  public static long unixTimestamp(java.sql.Timestamp timestamp) {
    return timestamp.getTime() / 1000L;
  }
}
