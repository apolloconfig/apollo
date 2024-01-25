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
package com.ctrip.framework.apollo.portal.environment;

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * load meta server addressed from database.
 * PortalDB.ServerConfig
 */
class DatabasePortalMetaServerProvider implements PortalMetaServerProvider {
  private static final Logger logger = LoggerFactory.getLogger(DatabasePortalMetaServerProvider.class);

  /**
   * read config from database
   */
  private final PortalConfig portalConfig;

  private volatile Map<Env, String> addresses;

  DatabasePortalMetaServerProvider(final PortalConfig portalConfig) {
    this.portalConfig = portalConfig;
    reload();
  }

  @Override
  public String getMetaServerAddress(Env targetEnv) {
    return addresses.get(targetEnv);
  }

  @Override
  public boolean exists(Env targetEnv) {
    return addresses.containsKey(targetEnv);
  }

  @Override
  public void reload() {
    Map<String, String> map = portalConfig.getMetaServers();
    addresses = Env.transformToEnvMap(map);
    logger.info("Loaded meta server addresses from portal config: {}", addresses);
  }

}
