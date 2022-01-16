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
package com.ctrip.framework.apollo.core.enums;

import com.google.common.base.Preconditions;

/**
 * Here is the brief description for all the predefined environments:
 * <ul>
 *   <li>LOCAL: Local Development environment, assume you are working at the beach with no network access</li>
 *   <li>DEV: Development environment</li>
 *   <li>FWS: Feature Web Service Test environment</li>
 *   <li>FAT: Feature Acceptance Test environment</li>
 *   <li>UAT: User Acceptance Test environment</li>
 *   <li>LPT: Load and Performance Test environment</li>
 *   <li>PRO: Production environment</li>
 *   <li>TOOLS: Tooling environment, a special area in production environment which allows
 * access to test environment, e.g. Apollo Portal should be deployed in tools environment</li>
 * </ul>
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public enum Env {
  LOCAL,
  DEV,
  FWS,
  FAT,
  UAT,
  LPT,
  PRO,
  TOOLS,
  UNKNOWN;

  public static Env fromString(String env) {
    Env environment = EnvUtils.transformEnv(env);
    Preconditions.checkArgument(environment != UNKNOWN, String.format("Env %s is invalid", env));
    return environment;
  }
}
