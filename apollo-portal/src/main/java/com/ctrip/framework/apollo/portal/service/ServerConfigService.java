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


import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI.ServerConfigAPI;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.repository.ServerConfigRepository;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ServerConfigService {

  private final ServerConfigRepository serverConfigRepository;

  private final AdminServiceAPI.ServerConfigAPI serverConfigAPI;
  private final UserInfoHolder userInfoHolder;

  public ServerConfigService(final ServerConfigRepository serverConfigRepository,
      ServerConfigAPI serverConfigAPI, UserInfoHolder userInfoHolder) {
    this.serverConfigRepository = serverConfigRepository;
    this.serverConfigAPI = serverConfigAPI;
    this.userInfoHolder = userInfoHolder;
  }

  public List<ServerConfig> findAllPortalDBConfig() {
    Iterable<ServerConfig> serverConfigs = serverConfigRepository.findAll();
    return Lists.newArrayList(serverConfigs);
  }
  public List<ServerConfig> findAllConfigDBConfig(Env env) {
    return serverConfigAPI.findAllConfigDBConfig(env);
  }

  @Transactional
  public ServerConfig createOrUpdatePortalDBConfig(ServerConfig serverConfig) {
    String modifiedBy = userInfoHolder.getUser().getUserId();

    ServerConfig storedConfig = serverConfigRepository.findByKey(serverConfig.getKey());

    if (Objects.isNull(storedConfig)) {//create
      serverConfig.setDataChangeCreatedBy(modifiedBy);
      serverConfig.setDataChangeLastModifiedBy(modifiedBy);
      serverConfig.setId(0L);//为空，设置ID 为0，jpa执行新增操作
      return serverConfigRepository.save(serverConfig);
    }
    //update
    BeanUtils.copyEntityProperties(serverConfig, storedConfig);
    storedConfig.setDataChangeLastModifiedBy(modifiedBy);
    return serverConfigRepository.save(storedConfig);
  }

  @Transactional
  public ServerConfig createOrUpdateConfigDBConfig(Env env, ServerConfig serverConfig) {
    String modifiedBy = userInfoHolder.getUser().getUserId();
    serverConfig.setDataChangeCreatedBy(modifiedBy);
    serverConfig.setDataChangeLastModifiedBy(modifiedBy);
    return serverConfigAPI.createOrUpdateConfigDBConfig(env, serverConfig);
  }
}
