/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.core.internals;

import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.spi.MetaServerProvider;
import com.ctrip.framework.apollo.core.utils.ResourceUtils;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * For legacy meta server configuration use, i.e. apollo-env.properties
 */
public class LegacyMetaServerProvider implements MetaServerProvider {

  // make it as lowest as possible, yet not the lowest
  public static final int ORDER = MetaServerProvider.LOWEST_PRECEDENCE - 1;
  private static final Map<Env, String> domains = new HashMap<>();

  public LegacyMetaServerProvider() {
    initialize();
  }

  private void initialize() {
    Properties prop = new Properties();
    prop = ResourceUtils.readConfigFile("apollo-env.properties", prop);

    domains.put(Env.LOCAL, getMetaServerAddress(prop, "local_meta", "local.meta"));
    domains.put(Env.DEV, getMetaServerAddress(prop, "dev_meta", "dev.meta"));
    domains.put(Env.FAT, getMetaServerAddress(prop, "fat_meta", "fat.meta"));
    domains.put(Env.UAT, getMetaServerAddress(prop, "uat_meta", "uat.meta"));
    domains.put(Env.LPT, getMetaServerAddress(prop, "lpt_meta", "lpt.meta"));
    domains.put(Env.PRO, getMetaServerAddress(prop, "pro_meta", "pro.meta"));
  }

  private String getMetaServerAddress(Properties prop, String sourceName, String propName) {
    // 1. Get from System Property.
    String metaAddress = System.getProperty(sourceName);
    if (Strings.isNullOrEmpty(metaAddress)) {
      // 2. Get from OS environment variable, which could not contain dot and is normally in UPPER case,like DEV_META.
      metaAddress = System.getenv(sourceName.toUpperCase());
    }
    if (Strings.isNullOrEmpty(metaAddress)) {
      // 3. Get from properties file.
      metaAddress = prop.getProperty(propName);
    }
    return metaAddress;
  }

  @Override
  public String getMetaServerAddress(Env targetEnv) {
    String metaServerAddress = domains.get(targetEnv);
    return metaServerAddress == null ? null : metaServerAddress.trim();
  }

  @Override
  public int getOrder() {
    return ORDER;
  }
}
