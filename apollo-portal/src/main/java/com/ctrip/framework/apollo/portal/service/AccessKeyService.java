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

import com.ctrip.framework.apollo.common.dto.AccessKeyDTO;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI.AccessKeyAPI;
import com.ctrip.framework.apollo.portal.constant.TracerEventType;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.tracer.Tracer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessKeyService {

  private final AdminServiceAPI.AccessKeyAPI accessKeyAPI;

  public AccessKeyService(AccessKeyAPI accessKeyAPI) {
    this.accessKeyAPI = accessKeyAPI;
  }

  public List<AccessKeyDTO> findByAppId(Env env, String appId) {
    return accessKeyAPI.findByAppId(env, appId);
  }

  public AccessKeyDTO createAccessKey(Env env, AccessKeyDTO accessKey) {
    AccessKeyDTO accessKeyDTO = accessKeyAPI.create(env, accessKey);
    Tracer.logEvent(TracerEventType.CREATE_ACCESS_KEY, accessKey.getAppId());
    return accessKeyDTO;
  }

  public void deleteAccessKey(Env env, String appId, long id, String operator) {
    accessKeyAPI.delete(env, appId, id, operator);
  }

  public void enable(Env env, String appId, long id, int mode, String operator) {
    accessKeyAPI.enable(env, appId, id, mode, operator);
  }

  public void disable(Env env, String appId, long id, String operator) {
    accessKeyAPI.disable(env, appId, id, operator);
  }
}
