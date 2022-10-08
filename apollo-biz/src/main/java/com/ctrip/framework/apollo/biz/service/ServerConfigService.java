/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.entity.ServerConfig;
import com.ctrip.framework.apollo.biz.repository.ServerConfigRepository;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServerConfigService {

  private final ServerConfigRepository serverConfigRepository;

  public ServerConfigService(final ServerConfigRepository serverConfigRepository) {
    this.serverConfigRepository = serverConfigRepository;
  }

  public List<ServerConfig> findAll() {
    Iterable<ServerConfig> all = serverConfigRepository.findAll();

    List<ServerConfig> serverConfigs = new ArrayList<>();

    Iterator it = all.iterator();
    while (it.hasNext()) {
      serverConfigs.add((ServerConfig) it.next());
    }

    return serverConfigs;
  }

  @Transactional
  public ServerConfig save(ServerConfig entity) {
    entity.setId(0);//protection
    entity.setCluster("default");
    ServerConfig serverConfig = serverConfigRepository.save(entity);

    return serverConfig;
  }

  @Transactional
  public void update(ServerConfig serverConfig) {
    serverConfig.setCluster("default");
    serverConfigRepository.save(serverConfig);
  }
}
