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
package com.ctrip.framework.foundation.internals.provider;

import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.core.utils.DeprecatedPropertyNotifyUtil;
import com.ctrip.framework.foundation.internals.Utils;
import com.ctrip.framework.foundation.internals.io.BOMInputStream;
import com.ctrip.framework.foundation.spi.provider.ApplicationProvider;
import com.ctrip.framework.foundation.spi.provider.Provider;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.slf4j.Logger;

public class DefaultApplicationProvider implements ApplicationProvider {

  private static final Logger logger = DeferredLoggerFactory
      .getLogger(DefaultApplicationProvider.class);
  public static final String APP_PROPERTIES_CLASSPATH = "/META-INF/app.properties";
  private Properties m_appProperties = new Properties();

  private String m_appId;
  private String m_appLabel;
  private String accessKeySecret;

  @Override
  public void initialize() {
    try {
      InputStream in = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream(APP_PROPERTIES_CLASSPATH.substring(1));
      if (in == null) {
        in = DefaultApplicationProvider.class.getResourceAsStream(APP_PROPERTIES_CLASSPATH);
      }

      initialize(in);
    } catch (Throwable ex) {
      logger.error("Initialize DefaultApplicationProvider failed.", ex);
    }
  }

  @Override
  public void initialize(InputStream in) {
    try {
      if (in != null) {
        try {
          m_appProperties
              .load(new InputStreamReader(new BOMInputStream(in), StandardCharsets.UTF_8));
        } finally {
          in.close();
        }
      }

      initAppId();
      initAppLabel();
      initAccessKey();
    } catch (Throwable ex) {
      logger.error("Initialize DefaultApplicationProvider failed.", ex);
    }
  }

  @Override
  public String getAppId() {
    return m_appId;
  }

  @Override
  public String getApolloLabel() {
    return m_appLabel;
  }

  @Override
  public String getAccessKeySecret() {
    return accessKeySecret;
  }

  @Override
  public boolean isAppIdSet() {
    return !Utils.isBlank(m_appId);
  }

  @Override
  public String getProperty(String name, String defaultValue) {
    if (ApolloClientSystemConsts.APP_ID.equals(name)) {
      String val = getAppId();
      return val == null ? defaultValue : val;
    }

    if (ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET.equals(name)) {
      String val = getAccessKeySecret();
      return val == null ? defaultValue : val;
    }

    String val = m_appProperties.getProperty(name, defaultValue);
    return val == null ? defaultValue : val;
  }

  @Override
  public Class<? extends Provider> getType() {
    return ApplicationProvider.class;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  private void initAppId() {
    // 1. Get app.id from System Property
    m_appId = System.getProperty(ApolloClientSystemConsts.APP_ID);
    if (!Utils.isBlank(m_appId)) {
      m_appId = m_appId.trim();
      logger.info("App ID is set to {} by app.id property from System Property", m_appId);
      return;
    }

    //2. Try to get app id from OS environment variable
    m_appId = System.getenv(ApolloClientSystemConsts.APP_ID_ENVIRONMENT_VARIABLES);
    if (!Utils.isBlank(m_appId)) {
      m_appId = m_appId.trim();
      logger.info("App ID is set to {} by APP_ID property from OS environment variable", m_appId);
      return;
    }

    // 3. Try to get app id from app.properties.
    m_appId = m_appProperties.getProperty(ApolloClientSystemConsts.APP_ID);
    if (!Utils.isBlank(m_appId)) {
      m_appId = m_appId.trim();
      logger.info("App ID is set to {} by app.id property from {}", m_appId,
          APP_PROPERTIES_CLASSPATH);
      return;
    }

    m_appId = null;
    logger.warn("app.id is not available from System Property and {}. It is set to null",
        APP_PROPERTIES_CLASSPATH);
  }

  private void initAppLabel() {
    // 1. Get app.label from System Property
    m_appLabel = System.getProperty(ApolloClientSystemConsts.APOLLO_LABEL);
    if (!Utils.isBlank(m_appLabel)) {
      m_appLabel = m_appLabel.trim();
      logger.info("App Label is set to {} by app.label property from System Property", m_appLabel);
      return;
    }

    //2. Try to get app label from OS environment variable
    m_appLabel = System.getenv(ApolloClientSystemConsts.APOLLO_LABEL_ENVIRONMENT_VARIABLES);
    if (!Utils.isBlank(m_appLabel)) {
      m_appLabel = m_appLabel.trim();
      logger.info("App Label is set to {} by APP_LABEL property from OS environment variable", m_appLabel);
      return;
    }

    // 3. Try to get app label from app.properties.
    m_appLabel = m_appProperties.getProperty(ApolloClientSystemConsts.APOLLO_LABEL);
    if (!Utils.isBlank(m_appLabel)) {
      m_appLabel = m_appLabel.trim();
      logger.info("App Label is set to {} by app.label property from {}", m_appLabel,
          APP_PROPERTIES_CLASSPATH);
      return;
    }

    m_appLabel = null;
    logger.warn("app.label is not available from System Property and {}. It is set to null",
        APP_PROPERTIES_CLASSPATH);
  }

  private void initAccessKey() {
    // 1. Get ACCESS KEY SECRET from System Property
    accessKeySecret = System.getProperty(ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET);
    if (!Utils.isBlank(accessKeySecret)) {
      accessKeySecret = accessKeySecret.trim();
      logger
          .info("ACCESS KEY SECRET is set by apollo.access-key.secret property from System Property");
      return;
    }

    //2. Try to get ACCESS KEY SECRET from OS environment variable
    accessKeySecret = System.getenv(ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET_ENVIRONMENT_VARIABLES);
    if (!Utils.isBlank(accessKeySecret)) {
      accessKeySecret = accessKeySecret.trim();
      logger.info(
          "ACCESS KEY SECRET is set by APOLLO_ACCESS_KEY_SECRET property from OS environment variable");
      return;
    }

    // 3. Try to get ACCESS KEY SECRET from app.properties.
    accessKeySecret = m_appProperties.getProperty(ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET);
    if (!Utils.isBlank(accessKeySecret)) {
      accessKeySecret = accessKeySecret.trim();
      logger.info("ACCESS KEY SECRET is set by apollo.access-key.secret property from {}",
          APP_PROPERTIES_CLASSPATH);
      return;
    }

    // 4. Try to get ACCESS KEY SECRET from deprecated config.
    accessKeySecret = initDeprecatedAccessKey();
    if (!Utils.isBlank(accessKeySecret)) {
      return;
    }
    accessKeySecret = null;
  }

  @SuppressWarnings("deprecation")
  private String initDeprecatedAccessKey() {
    // 1. Get ACCESS KEY SECRET from System Property
    String accessKeySecret = System
        .getProperty(ApolloClientSystemConsts.DEPRECATED_APOLLO_ACCESS_KEY_SECRET);
    if (!Utils.isBlank(accessKeySecret)) {
      accessKeySecret = accessKeySecret.trim();
      logger.info(
          "ACCESS KEY SECRET is set by apollo.accesskey.secret property from System Property");
      DeprecatedPropertyNotifyUtil
          .warn(ApolloClientSystemConsts.DEPRECATED_APOLLO_ACCESS_KEY_SECRET,
              ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET);
      return accessKeySecret;
    }

    //2. Try to get ACCESS KEY SECRET from OS environment variable
    accessKeySecret = System
        .getenv(ApolloClientSystemConsts.DEPRECATED_APOLLO_ACCESS_KEY_SECRET_ENVIRONMENT_VARIABLES);
    if (!Utils.isBlank(accessKeySecret)) {
      accessKeySecret = accessKeySecret.trim();
      logger.info(
          "ACCESS KEY SECRET is set by APOLLO_ACCESSKEY_SECRET property from OS environment variable");
      DeprecatedPropertyNotifyUtil
          .warn(ApolloClientSystemConsts.DEPRECATED_APOLLO_ACCESS_KEY_SECRET_ENVIRONMENT_VARIABLES,
              ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET_ENVIRONMENT_VARIABLES);
      return accessKeySecret;
    }

    // 3. Try to get ACCESS KEY SECRET from app.properties.
    accessKeySecret = m_appProperties
        .getProperty(ApolloClientSystemConsts.DEPRECATED_APOLLO_ACCESS_KEY_SECRET);
    if (!Utils.isBlank(accessKeySecret)) {
      accessKeySecret = accessKeySecret.trim();
      logger.info("ACCESS KEY SECRET is set by apollo.accesskey.secret property from {}",
          APP_PROPERTIES_CLASSPATH);
      DeprecatedPropertyNotifyUtil
          .warn(ApolloClientSystemConsts.DEPRECATED_APOLLO_ACCESS_KEY_SECRET,
              ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET);
      return accessKeySecret;
    }
    return null;
  }

  @Override
  public String toString() {
    return "appId [" + getAppId() + "] properties: " + m_appProperties
        + " (DefaultApplicationProvider)";
  }
}
