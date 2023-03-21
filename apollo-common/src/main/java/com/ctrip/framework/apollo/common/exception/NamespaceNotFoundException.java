package com.ctrip.framework.apollo.common.exception;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/21
 */
public class NamespaceNotFoundException extends NotFoundException{

  public NamespaceNotFoundException(String appId, String clusterName, String namespaceName) {
    super("namespace not found for appId:%s clusterName:%s namespaceName:%s", appId, clusterName, namespaceName);
  }

  public NamespaceNotFoundException(long namespaceId) {
    super("namespace not found for namespaceId:%s", namespaceId);
  }
}
