/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.dto.NamespaceLockDTO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.vo.LockInfo;
import org.springframework.stereotype.Service;

@Service
public class NamespaceLockService {

  private final AdminServiceAPI.NamespaceLockAPI namespaceLockAPI;
  private final PortalConfig portalConfig;

  public NamespaceLockService(final AdminServiceAPI.NamespaceLockAPI namespaceLockAPI, final PortalConfig portalConfig) {
    this.namespaceLockAPI = namespaceLockAPI;
    this.portalConfig = portalConfig;
  }


  public NamespaceLockDTO getNamespaceLock(String appId, Env env, String clusterName, String namespaceName) {
    return namespaceLockAPI.getNamespaceLockOwner(appId, env, clusterName, namespaceName);
  }

  public LockInfo getNamespaceLockInfo(String appId, Env env, String clusterName, String namespaceName) {
    LockInfo lockInfo = new LockInfo();

    NamespaceLockDTO namespaceLockDTO = namespaceLockAPI.getNamespaceLockOwner(appId, env, clusterName, namespaceName);
    String lockOwner = namespaceLockDTO == null ? "" : namespaceLockDTO.getDataChangeCreatedBy();
    lockInfo.setLockOwner(lockOwner);

    lockInfo.setEmergencyPublishAllowed(portalConfig.isEmergencyPublishAllowed(env));

    return lockInfo;
  }

}
