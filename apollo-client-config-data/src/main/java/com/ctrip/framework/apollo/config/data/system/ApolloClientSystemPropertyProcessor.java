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
package com.ctrip.framework.apollo.config.data.system;

import com.ctrip.framework.apollo.config.data.util.Slf4jLogMessageFormatter;
import com.ctrip.framework.apollo.spring.boot.ApolloApplicationContextInitializer;
import org.apache.commons.logging.Log;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.util.StringUtils;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloClientSystemPropertyProcessor {

  private final Log log;

  public ApolloClientSystemPropertyProcessor(Log log) {
    this.log = log;
  }

  public void setSystemProperties(Binder binder, BindHandler bindHandler) {
    for (String propertyName : ApolloApplicationContextInitializer.APOLLO_SYSTEM_PROPERTIES) {
      if (ConfigurationPropertyName.isValid(propertyName)) {
        this.fillSystemPropertyFromBinder(propertyName, propertyName, binder, bindHandler);
        continue;
      }
      this.fillSystemPropertyFromBinder(propertyName, this.camelCasedToKebabCase(propertyName),
          binder, bindHandler);
    }
  }

  private void fillSystemPropertyFromBinder(String propertyName, String bindName, Binder binder,
      BindHandler bindHandler) {
    if (System.getProperty(propertyName) != null) {
      return;
    }
    String propertyValue = binder.bind(bindName, Bindable.of(String.class), bindHandler)
        .orElse(null);
    if (!StringUtils.hasText(propertyValue)) {
      return;
    }
    log.debug(Slf4jLogMessageFormatter
        .format("apollo client set system property key=[{}] value=[{}]", propertyName,
            propertyValue));
    System.setProperty(propertyName, propertyValue);
  }

  /**
   * {@link ConfigurationPropertyName#isValid(java.lang.CharSequence)}
   *
   * @param source origin propertyName
   * @return valid propertyName
   */
  private String camelCasedToKebabCase(String source) {
    StringBuilder stringBuilder = new StringBuilder(source.length() * 2);
    for (char ch : source.toCharArray()) {
      if (Character.isUpperCase(ch)) {
        stringBuilder.append("-").append(Character.toLowerCase(ch));
        continue;
      }
      stringBuilder.append(ch);
    }
    return stringBuilder.toString();
  }
}