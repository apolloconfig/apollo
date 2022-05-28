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
package com.ctrip.framework.apollo.spring.util;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySourceFactory;
import com.ctrip.framework.apollo.spring.property.PlaceholderHelper;
import com.ctrip.framework.apollo.spring.property.SpringValueRegistry;
import com.ctrip.framework.apollo.tracer.Tracer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpringInjector {
  private static volatile Map<Class<?>, Object> container;
  private static final Object lock = new Object();

  private static void getInjector() {
    if (container == null) {
      synchronized (lock) {
        if (container == null) {
          try {
            container = new ConcurrentHashMap<>();
            container.put(PlaceholderHelper.class, new PlaceholderHelper());
            container.put(ConfigPropertySourceFactory.class, new ConfigPropertySourceFactory());
            container.put(SpringValueRegistry.class, new SpringValueRegistry());
          } catch (Throwable ex) {
            ApolloConfigException exception = new ApolloConfigException("Unable to initialize Apollo Spring Injector!", ex);
            Tracer.logError(exception);
            throw exception;
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T getInstance(Class<T> clazz) {
    try {
      getInjector();
      return (T) container.get(clazz);
    } catch (Throwable ex) {
      Tracer.logError(ex);
      throw new ApolloConfigException(
          String.format("Unable to load instance for %s!", clazz.getName()), ex);
    }
  }
}
