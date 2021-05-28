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
package com.ctrip.framework.foundation;

import com.ctrip.framework.foundation.internals.NullProviderManager;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import com.ctrip.framework.foundation.spi.ProviderManager;
import com.ctrip.framework.foundation.spi.provider.ApplicationProvider;
import com.ctrip.framework.foundation.spi.provider.NetworkProvider;
import com.ctrip.framework.foundation.spi.provider.ServerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Foundation {
  private static final Logger logger = LoggerFactory.getLogger(Foundation.class);
  private static Object lock = new Object();

  private static volatile ProviderManager s_manager;

  // Encourage early initialization and fail early if it happens.
  static {
    getManager();
  }

  private static ProviderManager getManager() {
    try {
      if (s_manager == null) {
        // Double locking to make sure only one thread initializes ProviderManager.
        synchronized (lock) {
          if (s_manager == null) {
            s_manager = ServiceBootstrap.loadFirst(ProviderManager.class);
          }
        }
      }

      return s_manager;
    } catch (Throwable ex) {
      s_manager = new NullProviderManager();
      logger.error("Initialize ProviderManager failed.", ex);
      return s_manager;
    }
  }

  public static String getProperty(String name, String defaultValue) {
    try {
      return getManager().getProperty(name, defaultValue);
    } catch (Throwable ex) {
      logger.error("getProperty for {} failed.", name, ex);
      return defaultValue;
    }
  }

  public static NetworkProvider net() {
    try {
      return getManager().provider(NetworkProvider.class);
    } catch (Exception ex) {
      logger.error("Initialize NetworkProvider failed.", ex);
      return NullProviderManager.provider;
    }
  }

  public static ServerProvider server() {
    try {
      return getManager().provider(ServerProvider.class);
    } catch (Exception ex) {
      logger.error("Initialize ServerProvider failed.", ex);
      return NullProviderManager.provider;
    }
  }

  public static ApplicationProvider app() {
    try {
      return getManager().provider(ApplicationProvider.class);
    } catch (Exception ex) {
      logger.error("Initialize ApplicationProvider failed.", ex);
      return NullProviderManager.provider;
    }
  }
}
