package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.dto.NamespaceLockDTO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.entity.vo.LockInfo;
import com.ctrip.framework.apollo.portal.service.NamespaceLockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NamespaceLockController {

  private final NamespaceLockService namespaceLockService;

  public NamespaceLockController(final NamespaceLockService namespaceLockService) {
    this.namespaceLockService = namespaceLockService;
  }

  @Deprecated
  @GetMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/lock")
  public NamespaceLockDTO getNamespaceLock(@PathVariable String appId, @PathVariable String env,
                                           @PathVariable String clusterName, @PathVariable String namespaceName) {

    return namespaceLockService.getNamespaceLock(appId, Env.valueOf(env), clusterName, namespaceName);
  }

  @GetMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/lock-info")
  public LockInfo getNamespaceLockInfo(@PathVariable String appId, @PathVariable String env,
                                       @PathVariable String clusterName, @PathVariable String namespaceName) {

    return namespaceLockService.getNamespaceLockInfo(appId, Env.valueOf(env), clusterName, namespaceName);

  }


}
