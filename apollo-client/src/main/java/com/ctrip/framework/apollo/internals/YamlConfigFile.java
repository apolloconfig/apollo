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
package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.util.ExceptionUtil;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.apollo.PropertiesCompatibleConfigFile;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.util.yaml.YamlParser;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class YamlConfigFile extends PlainTextConfigFile implements PropertiesCompatibleConfigFile {
  private static final Logger logger = LoggerFactory.getLogger(YamlConfigFile.class);
  private volatile Properties cachedProperties;

  public YamlConfigFile(String namespace, ConfigRepository configRepository) {
    super(namespace, configRepository);
    tryTransformToProperties();
  }

  @Override
  public ConfigFileFormat getConfigFileFormat() {
    return ConfigFileFormat.YAML;
  }

  @Override
  protected void update(Properties newProperties) {
    super.update(newProperties);
    tryTransformToProperties();
  }

  @Override
  public Properties asProperties() {
    if (cachedProperties == null) {
      transformToProperties();
    }
    return cachedProperties;
  }

  private boolean tryTransformToProperties() {
    try {
      transformToProperties();
      return true;
    } catch (Throwable ex) {
      Tracer.logEvent("ApolloConfigException", ExceptionUtil.getDetailMessage(ex));
      logger.warn("yaml to properties failed, reason: {}", ExceptionUtil.getDetailMessage(ex));
    }
    return false;
  }

  private synchronized void transformToProperties() {
    cachedProperties = toProperties();
  }

  private Properties toProperties() {
    if (!this.hasContent()) {
      return propertiesFactory.getPropertiesInstance();
    }

    try {
      return ApolloInjector.getInstance(YamlParser.class).yamlToProperties(getContent());
    } catch (Throwable ex) {
      ApolloConfigException exception = new ApolloConfigException(
          "Parse yaml file content failed for namespace: " + m_namespace, ex);
      Tracer.logError(exception);
      throw exception;
    }
  }
}
