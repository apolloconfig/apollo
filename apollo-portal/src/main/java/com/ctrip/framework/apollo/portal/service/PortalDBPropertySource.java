/*
 * Copyright 2023 Apollo Authors
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

import com.google.common.collect.Maps;

import com.ctrip.framework.apollo.common.config.RefreshablePropertySource;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.repository.ServerConfigRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;


/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Component
public class PortalDBPropertySource extends RefreshablePropertySource {
  private static final Logger logger = LoggerFactory.getLogger(PortalDBPropertySource.class);

  private final ServerConfigRepository serverConfigRepository;

  private final DataSource dataSource;

  public PortalDBPropertySource(final String name,
      final Map<String, Object> source,
      final ServerConfigRepository serverConfigRepository, DataSource dataSource) {
    super(name, source);
    this.serverConfigRepository = serverConfigRepository;
    this.dataSource = dataSource;
  }
  @Autowired
  public PortalDBPropertySource(final ServerConfigRepository serverConfigRepository, DataSource dataSource) {
    super("DBConfig", Maps.newConcurrentMap());
    this.serverConfigRepository = serverConfigRepository;
    this.dataSource = dataSource;
  }

  @Profile("h2")
  @PostConstruct
  public void runSqlScript() throws Exception {
    Resource resource = new ClassPathResource("jpa/init.h2.sql");
    if (resource.exists()) {
      DatabasePopulatorUtils.execute(new ResourceDatabasePopulator(resource), dataSource);
    }
  }

  @Override
  protected void refresh() {
    Iterable<ServerConfig> dbConfigs = serverConfigRepository.findAll();

    for (ServerConfig config: dbConfigs) {
      String key = config.getKey();
      Object value = config.getValue();

      if (this.source.isEmpty()) {
        logger.info("Load config from DB : {} = {}", key, value);
      } else if (!Objects.equals(this.source.get(key), value)) {
        logger.info("Load config from DB : {} = {}. Old value = {}", key,
                    value, this.source.get(key));
      }

      this.source.put(key, value);
    }
  }


}
