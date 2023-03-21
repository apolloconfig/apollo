package com.ctrip.framework.apollo.common.exception;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/21
 */
public class AppNotFountException extends NotFoundException{

  public AppNotFountException(String appId) {
      super("app not found for appId:%s", appId);
  }
}
