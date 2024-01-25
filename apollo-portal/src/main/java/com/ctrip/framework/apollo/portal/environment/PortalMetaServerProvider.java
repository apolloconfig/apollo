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

/**
 * For the supporting of multiple meta server address providers.
 * From configuration file,
 * from OS environment,
 * From database,
 * ...
 * Just implement this interface
 * @author wxq
 */
public interface PortalMetaServerProvider {

  /**
   * @param targetEnv environment
   * @return meta server address matched environment
   */
  String getMetaServerAddress(Env targetEnv);

  /**
   * @param targetEnv environment
   * @return environment's meta server address exists or not
   */
  boolean exists(Env targetEnv);

  /**
   * reload the meta server address in runtime
   */
  void reload();

}
