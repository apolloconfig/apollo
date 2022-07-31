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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

/**
 * Config Validator
 *
 * @author Lifeng Lv
 */
public class ConfigValidator {

  private final ConfigurableApplicationContext applicationContext;

  public ConfigValidator(ConfigurableApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    checkDataBaseName();
  }

  /**
   * if apollo.check.database-name is null or ture
   */
  private void checkDataBaseName() {
    String checkFlag = applicationContext.getEnvironment().getProperty("apollo.check.database-name");
    if (!StringUtils.isBlank(checkFlag)
        && StringUtils.equals(Boolean.FALSE.toString(), checkFlag.toLowerCase())) {
      return;
    }
    String applicationName = applicationContext.getEnvironment().getProperty("spring.application.name");
    String dataSourceUrl = applicationContext.getEnvironment().getProperty("spring.datasource.url");
    if (!StringUtils.isBlank(dataSourceUrl)
        && !StringUtils.isBlank(applicationName)) {
      String dbName = determineDatabaseName(dataSourceUrl);
      if (!StringUtils.isEmpty(dbName)) {
        switch (applicationName) {
          case "apollo-adminservice":
          case "apollo-configservice":
            Assert.isTrue(dbName.toLowerCase().contains("config"), "Please configure the "
                + "correct database name, it should be ApolloConfigDB! If you have changed the name of the"
                + " database and want to disable the check, please set apollo.check.database-name to false!");
            break;
          case "apollo-portal":
            Assert.isTrue(dbName.toLowerCase().contains("portal"), "Please configure the "
                + "correct database name, it should be ApolloPortalDB! If you have changed the name of "
                + "the database and want to disable the check, please set apollo.check.database-name to false!");
            break;
          default:
        }
      }
    }
  }

  private String determineDatabaseName(String dataSourceUrl) {
    // jdbc:mysql://127.0.0.1:3306/TestDB?a=b
    // jdbc:mysql:///TestDB?a=b
    String pattern = "jdbc:(?<type>[a-z]+)://((?<host>[a-zA-Z0-9-//.]+):(?<port>[0-9]+))?/(?<databaseName>[a-zA-Z0-9_]+)?";
    Pattern namePattern = Pattern.compile(pattern);
    Matcher dateMatcher = namePattern.matcher(dataSourceUrl);
    return dateMatcher.find() ? dateMatcher.group("databaseName") : null;
  }
}
