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
package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.spi.MetaServerProvider;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.foundation.Foundation;
import com.google.common.base.Strings;
import org.slf4j.Logger;

public class DefaultMetaServerProvider implements MetaServerProvider {

  public static final int ORDER = 0;
  private static final Logger logger = DeferredLoggerFactory
      .getLogger(DefaultMetaServerProvider.class);

  private final String metaServerAddress;

  public DefaultMetaServerProvider() {
    metaServerAddress = initMetaServerAddress();
  }

  private String initMetaServerAddress() {
    // 1. Get from System Property
    String metaAddress = System.getProperty(ConfigConsts.APOLLO_META_KEY);
    if (Strings.isNullOrEmpty(metaAddress)) {
      // 2. Get from OS environment variable, which could not contain dot and is normally in UPPER case
      metaAddress = System.getenv("APOLLO_META");
    }
    if (Strings.isNullOrEmpty(metaAddress)) {
      // 3. Get from server.properties
      metaAddress = Foundation.server().getProperty(ConfigConsts.APOLLO_META_KEY, null);
    }
    if (Strings.isNullOrEmpty(metaAddress)) {
      // 4. Get from app.properties
      metaAddress = Foundation.app().getProperty(ConfigConsts.APOLLO_META_KEY, null);
    }

    if (Strings.isNullOrEmpty(metaAddress)) {
      logger.warn(
          "Could not find meta server address, because it is not available in neither (1) JVM system property 'apollo.meta', (2) OS env variable 'APOLLO_META' (3) property 'apollo.meta' from server.properties nor (4) property 'apollo.meta' from app.properties");
    } else {
      metaAddress = metaAddress.trim();
      logger.info("Located meta services from apollo.meta configuration: {}!", metaAddress);
    }

    return metaAddress;
  }

  @Override
  public String getMetaServerAddress(Env targetEnv) {
    //for default meta server provider, we don't care the actual environment
    return metaServerAddress;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }
}
