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
package com.ctrip.framework.apollo.demo.api;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigFileChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.internals.YamlConfigFile;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import com.ctrip.framework.foundation.Foundation;
import com.google.common.base.Charsets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ApolloConfigDemo {
  private static final Logger logger = LoggerFactory.getLogger(ApolloConfigDemo.class);
  private String DEFAULT_VALUE = "undefined";
  private Config config;
  private Config yamlConfig;
  private Config publicConfig;
  private ConfigFile applicationConfigFile;
  private ConfigFile xmlConfigFile;
  private YamlConfigFile yamlConfigFile;

  public ApolloConfigDemo() {
    ConfigChangeListener changeListener =
        new ConfigChangeListener() {
          @Override
          public void onChange(ConfigChangeEvent changeEvent) {
            logger.info("Changes for namespace {}", changeEvent.getNamespace());
            for (String key : changeEvent.changedKeys()) {
              ConfigChange change = changeEvent.getChange(key);
              logger.info(
                  "Change - key: {}, oldValue: {}, newValue: {}, changeType: {}",
                  change.getPropertyName(),
                  change.getOldValue(),
                  change.getNewValue(),
                  change.getChangeType());
            }
          }
        };
    config = ConfigService.getAppConfig();
    config.addChangeListener(changeListener);
    yamlConfig = ConfigService.getConfig("application.yaml");
    yamlConfig.addChangeListener(changeListener);
    publicConfig = ConfigService.getConfig("TEST1.apollo");
    publicConfig.addChangeListener(changeListener);
    applicationConfigFile = ConfigService.getConfigFile("application", ConfigFileFormat.Properties);
    // datasources.xml
    xmlConfigFile = ConfigService.getConfigFile("datasources", ConfigFileFormat.XML);
    xmlConfigFile.addChangeListener(
        new ConfigFileChangeListener() {
          @Override
          public void onChange(ConfigFileChangeEvent changeEvent) {
            logger.info(changeEvent.toString());
          }
        });
    // application.yaml
    yamlConfigFile =
        (YamlConfigFile) ConfigService.getConfigFile("application", ConfigFileFormat.YAML);
  }

  private String getConfig(String key) {
    String result = config.getProperty(key, DEFAULT_VALUE);
    if (DEFAULT_VALUE.equals(result)) {
      result = publicConfig.getProperty(key, DEFAULT_VALUE);
    }
    if (DEFAULT_VALUE.equals(result)) {
      result = yamlConfig.getProperty(key, DEFAULT_VALUE);
    }
    logger.info(String.format("Loading key : %s with value: %s", key, result));
    return result;
  }

  private void print(String namespace) {
    switch (namespace) {
      case "application":
        print(applicationConfigFile);
        return;
      case "xml":
        print(xmlConfigFile);
        return;
      case "yaml":
        printYaml(yamlConfigFile);
        return;
    }
  }

  private void print(ConfigFile configFile) {
    if (!configFile.hasContent()) {
      System.out.println("No config file content found for " + configFile.getNamespace());
      return;
    }
    System.out.println(
        "=== Config File Content for " + configFile.getNamespace() + " is as follows: ");
    System.out.println(configFile.getContent());
  }

  private void printYaml(YamlConfigFile configFile) {
    System.out.println("=== Properties for " + configFile.getNamespace() + " is as follows: ");
    System.out.println(configFile.asProperties());
  }

  private void printEnvInfo() {
    String message =
        String.format(
            "AppId: %s, Env: %s, DC: %s, IP: %s",
            Foundation.app().getAppId(),
            Foundation.server().getEnvType(),
            Foundation.server().getDataCenter(),
            Foundation.net().getHostAddress());
    System.out.println(message);
  }

  public static void main(String[] args) throws IOException {
    ApolloConfigDemo apolloConfigDemo = new ApolloConfigDemo();
    apolloConfigDemo.printEnvInfo();
    System.out.println("Apollo Config Demo. Please input key to get the value.");
    while (true) {
      System.out.print("> ");
      String input =
          new BufferedReader(new InputStreamReader(System.in, Charsets.UTF_8)).readLine();
      if (input == null || input.length() == 0) {
        continue;
      }
      input = input.trim();
      try {
        if (input.equalsIgnoreCase("application")) {
          apolloConfigDemo.print("application");
          continue;
        }
        if (input.equalsIgnoreCase("xml")) {
          apolloConfigDemo.print("xml");
          continue;
        }
        if (input.equalsIgnoreCase("yaml") || input.equalsIgnoreCase("yml")) {
          apolloConfigDemo.print("yaml");
          continue;
        }
        if (input.equalsIgnoreCase("quit")) {
          System.exit(0);
        }
        apolloConfigDemo.getConfig(input);
      } catch (Throwable ex) {
        logger.error("some error occurred", ex);
      }
    }
  }
}
