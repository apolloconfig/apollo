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
package com.ctrip.framework.apollo.spi;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigRegistry implements ConfigRegistry {
  private static final Logger s_logger = LoggerFactory.getLogger(DefaultConfigRegistry.class);
  private final Map<String, ConfigFactory> m_instances = Maps.newConcurrentMap();

  @Override
  public void register(String namespace, ConfigFactory factory) {
    if (m_instances.containsKey(namespace)) {
      s_logger.warn("ConfigFactory({}) is overridden by {}!", namespace, factory.getClass());
    }

    m_instances.put(namespace, factory);
  }

  @Override
  public ConfigFactory getFactory(String namespace) {
    return m_instances.get(namespace);
  }
}
