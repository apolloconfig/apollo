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
package com.ctrip.framework.apollo.core.spi;

import com.ctrip.framework.apollo.core.enums.Env;

/**
 * @since 1.0.0
 */
public interface MetaServerProvider extends Ordered {

  /**
   * Provide the Apollo meta server address, could be a domain url or comma separated ip addresses, like http://1.2.3.4:8080,http://2.3.4.5:8080.
   * <br/>
   * In production environment, we suggest using one single domain like http://config.xxx.com(backed by software load balancers like nginx) instead of multiple ip addresses
   */
  String getMetaServerAddress(Env targetEnv);
}
