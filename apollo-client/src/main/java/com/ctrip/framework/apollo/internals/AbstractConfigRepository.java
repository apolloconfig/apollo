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

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.collect.Lists;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public abstract class AbstractConfigRepository implements ConfigRepository {
  private static final Logger logger = LoggerFactory.getLogger(AbstractConfigRepository.class);
  private List<RepositoryChangeListener> m_listeners = Lists.newCopyOnWriteArrayList();
  protected PropertiesFactory propertiesFactory = ApolloInjector.getInstance(PropertiesFactory.class);

  protected boolean trySync() {
    try {
      sync();
      return true;
    } catch (Throwable ex) {
      Tracer.logEvent("ApolloConfigException", ExceptionUtil.getDetailMessage(ex));
      logger
          .warn("Sync config failed, will retry. Repository {}, reason: {}", this.getClass(), ExceptionUtil
              .getDetailMessage(ex));
    }
    return false;
  }

  protected abstract void sync();

  @Override
  public void addChangeListener(RepositoryChangeListener listener) {
    if (!m_listeners.contains(listener)) {
      m_listeners.add(listener);
    }
  }

  @Override
  public void removeChangeListener(RepositoryChangeListener listener) {
    m_listeners.remove(listener);
  }

  protected void fireRepositoryChange(String namespace, Properties newProperties) {
    for (RepositoryChangeListener listener : m_listeners) {
      try {
        listener.onRepositoryChange(namespace, newProperties);
      } catch (Throwable ex) {
        Tracer.logError(ex);
        logger.error("Failed to invoke repository change listener {}", listener.getClass(), ex);
      }
    }
  }
}
