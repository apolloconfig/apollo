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
package com.ctrip.framework.apollo.common.utils;

import com.ctrip.framework.apollo.core.utils.StringUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

/**
 * Config Validator
 *
 * @author Lv Lifeng
 */
public class ConfigValidator {

  private final ConfigurableApplicationContext applicationContext;

  public ConfigValidator(ConfigurableApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    checkDataBaseName();
  }

  private void checkDataBaseName() {
    String dataSourceUrl = System.getProperty("spring.datasource.url");
    if (!StringUtils.isBlank(dataSourceUrl) && !StringUtils.isBlank(applicationContext.getId())) {
      switch (applicationContext.getId()) {
        case "apollo-adminservice":
        case "apollo-configservice":
          Assert.isTrue(!dataSourceUrl.toLowerCase().contains("config"), "Please configure the" +
              " correct database name, it should be ApolloConfigDB!");
          break;
        case "apollo-portal":
          Assert.isTrue(!dataSourceUrl.toLowerCase().contains("portal"), "Please configure the" +
              " correct database name, it should be ApolloPortalDB!");
          break;
        default:
      }
    }
  }
}
