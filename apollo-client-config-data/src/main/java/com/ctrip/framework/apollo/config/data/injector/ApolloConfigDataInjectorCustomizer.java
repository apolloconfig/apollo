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
package com.ctrip.framework.apollo.config.data.injector;

import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.apollo.spi.ApolloInjectorCustomizer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloConfigDataInjectorCustomizer implements ApolloInjectorCustomizer {

  /**
   * the order of the injector customizer
   */
  public static final int ORDER = Ordered.LOWEST_PRECEDENCE - 200;

  private static final Map<Class<?>, Supplier<?>> INSTANCE_SUPPLIERS = new ConcurrentHashMap<>();

  private static final Map<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();

  /**
   * Register a specific type with the registry. If the specified type has already been registered,
   * it will be replaced.
   *
   * @param <T>              the instance type
   * @param type             the instance type
   * @param instanceSupplier the instance supplier
   */
  public static <T> void register(Class<T> type, Supplier<T> instanceSupplier) {
    INSTANCE_SUPPLIERS.put(type, instanceSupplier);
  }

  /**
   * Register a specific type with the registry if one is not already present.
   *
   * @param <T>              the instance type
   * @param type             the instance type
   * @param instanceSupplier the instance supplier
   */
  public static <T> void registerIfAbsent(Class<T> type, Supplier<T> instanceSupplier) {
    INSTANCE_SUPPLIERS.putIfAbsent(type, instanceSupplier);
  }

  /**
   * Return if a registration exists for the given type.
   *
   * @param <T>  the instance type
   * @param type the instance type
   * @return {@code true} if the type has already been registered
   */
  public static <T> boolean isRegistered(Class<T> type) {
    return INSTANCE_SUPPLIERS.containsKey(type);
  }

  @Override
  public <T> T getInstance(Class<T> clazz) {
    @SuppressWarnings("unchecked")
    Supplier<T> instanceSupplier = (Supplier<T>) INSTANCE_SUPPLIERS.get(clazz);
    if (instanceSupplier == null) {
      return null;
    }
    return this.getInstance(clazz, instanceSupplier);
  }

  @SuppressWarnings("unchecked")
  private <T> T getInstance(Class<T> type, Supplier<T> instanceSupplier) {
    T instance = (T) INSTANCES.get(type);
    if (instance != null) {
      return instance;
    }
    // prebuild an newInstance to prevent dead lock when recursive call computeIfAbsent
    // https://bugs.openjdk.java.net/browse/JDK-8062841
    T newInstance = instanceSupplier.get();
    return (T) INSTANCES.computeIfAbsent(type, key -> newInstance);
  }

  @Override
  public <T> T getInstance(Class<T> clazz, String name) {
    return null;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }
}
