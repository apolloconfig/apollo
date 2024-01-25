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
package com.ctrip.framework.apollo.portal.listener;

import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.dto.AppNamespaceDTO;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.tracer.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeletionListener {

  private static final Logger logger = LoggerFactory.getLogger(DeletionListener.class);

  private final PortalSettings portalSettings;
  private final AdminServiceAPI.AppAPI appAPI;
  private final AdminServiceAPI.NamespaceAPI namespaceAPI;

  public DeletionListener(
      final PortalSettings portalSettings,
      final AdminServiceAPI.AppAPI appAPI,
      final AdminServiceAPI.NamespaceAPI namespaceAPI) {
    this.portalSettings = portalSettings;
    this.appAPI = appAPI;
    this.namespaceAPI = namespaceAPI;
  }

  @EventListener
  public void onAppDeletionEvent(AppDeletionEvent event) {
    AppDTO appDTO = BeanUtils.transform(AppDTO.class, event.getApp());
    String appId = appDTO.getAppId();
    String operator = appDTO.getDataChangeLastModifiedBy();

    List<Env> envs = portalSettings.getActiveEnvs();
    for (Env env : envs) {
      try {
        appAPI.deleteApp(env, appId, operator);
      } catch (Throwable e) {
        logger.error("Delete app failed. Env = {}, AppId = {}", env, appId, e);
        Tracer.logError(String.format("Delete app failed. Env = %s, AppId = %s", env, appId), e);
      }
    }
  }

  @EventListener
  public void onAppNamespaceDeletionEvent(AppNamespaceDeletionEvent event) {
    AppNamespaceDTO appNamespace = BeanUtils.transform(AppNamespaceDTO.class, event.getAppNamespace());
    List<Env> envs = portalSettings.getActiveEnvs();
    String appId = appNamespace.getAppId();
    String namespaceName = appNamespace.getName();
    String operator = appNamespace.getDataChangeLastModifiedBy();

    for (Env env : envs) {
      try {
        namespaceAPI.deleteAppNamespace(env, appId, namespaceName, operator);
      } catch (Throwable e) {
        logger.error("Delete appNamespace failed. appId = {}, namespace = {}, env = {}", appId, namespaceName, env, e);
        Tracer.logError(String
            .format("Delete appNamespace failed. appId = %s, namespace = %s, env = %s", appId, namespaceName, env), e);
      }
    }
  }
}
