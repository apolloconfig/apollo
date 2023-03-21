package com.ctrip.framework.apollo.common.exception;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/21
 */
public class ClusterNotFoundException extends NotFoundException{

  public ClusterNotFoundException(String appId, String clusterName) {
    super("cluster not found for appId:%s clusterName:%s", appId, clusterName);
  }
}
