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

import com.ctrip.framework.apollo.core.utils.StringUtils;

/**
 * A utility class for the {@link Env} enum.
 * <p>
 * The class provides simple functionalities that extend the capabilities of {@link Env}
 *
 * @author Diego Krupitza(info@diegokrupitza.com)
 */
public final class EnvUtils {

  /**
   * Transforms a given String to its matching {@link Env}
   *
   * @param envName the String to convert
   * @return the matching {@link Env} for the given String
   */
  public static Env transformEnv(String envName) {
    if (StringUtils.isBlank(envName)) {
      return Env.UNKNOWN;
    }

    String cleanedEnvName = envName.trim().toUpperCase();

    // fix up in case there is a typo
    // like prod/pro
    if (cleanedEnvName.equals("PROD")) {
      return Env.PRO;
    }

    if (cleanedEnvName.equals("FWS")) {
      // special case that FAT & FWS
      // should return the same
      return Env.FAT;
    }

    try {
      return Env.valueOf(cleanedEnvName);
    } catch (IllegalArgumentException e) {
      // the name could not be found
      // or there is a typo we dont handle
      return Env.UNKNOWN;
    }
  }
}
