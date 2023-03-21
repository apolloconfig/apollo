package com.ctrip.framework.apollo.common.exception;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/21
 */
public class ReleaseNotFoundException extends NotFoundException{

  public ReleaseNotFoundException(Object releaseId) {
    super("release not found for releaseId:%s", releaseId);
  }
}
