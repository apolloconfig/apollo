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
package com.ctrip.framework.apollo.common.datasource;

import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.enums.EnvUtils;
import com.ctrip.framework.foundation.Foundation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TitanSettings {

  @Value("${fat.titan.url:}")
  private String fatTitanUrl;

  @Value("${uat.titan.url:}")
  private String uatTitanUrl;

  @Value("${pro.titan.url:}")
  private String proTitanUrl;

  @Value("${fat.titan.dbname:}")
  private String fatTitanDbname;

  @Value("${uat.titan.dbname:}")
  private String uatTitanDbname;

  @Value("${pro.titan.dbname:}")
  private String proTitanDbname;

  public String getTitanUrl() {
    Env env = EnvUtils.transformEnv(Foundation.server().getEnvType());
    switch (env) {
      case FAT:
      case FWS:
        return fatTitanUrl;
      case UAT:
        return uatTitanUrl;
      case TOOLS:
      case PRO:
        return proTitanUrl;
      default:
        return "";
    }
  }

  public String getTitanDbname() {
    Env env = EnvUtils.transformEnv(Foundation.server().getEnvType());
    switch (env) {
      case FAT:
      case FWS:
        return fatTitanDbname;
      case UAT:
        return uatTitanDbname;
      case TOOLS:
      case PRO:
        return proTitanDbname;
      default:
        return "";
    }
  }
}
