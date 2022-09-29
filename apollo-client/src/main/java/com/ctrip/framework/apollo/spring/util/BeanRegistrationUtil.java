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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.MethodMetadata;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class BeanRegistrationUtil {
  // reserved bean definitions, we should consider drop this if we will upgrade Spring version
  private static final Map<String, String> RESERVED_BEAN_DEFINITIONS = new ConcurrentHashMap<>();

  static {
    RESERVED_BEAN_DEFINITIONS.put(
        "org.springframework.context.support.PropertySourcesPlaceholderConfigurer",
        "org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration#propertySourcesPlaceholderConfigurer"
    );
  }

  public static boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, Class<?> beanClass) {
    return registerBeanDefinitionIfNotExists(registry, beanClass, null);
  }

  public static boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, Class<?> beanClass,
                                                          Map<String, Object> extraPropertyValues) {
    return registerBeanDefinitionIfNotExists(registry, beanClass.getName(), beanClass, extraPropertyValues);
  }

  public static boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, String beanName,
                                                          Class<?> beanClass) {
    return registerBeanDefinitionIfNotExists(registry, beanName, beanClass, null);
  }

  public static boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, String beanName,
                                                          Class<?> beanClass, Map<String, Object> extraPropertyValues) {
    if (registry.containsBeanDefinition(beanName)) {
      return false;
    }

    String[] candidates = registry.getBeanDefinitionNames();

    String reservedBeanDefinition = RESERVED_BEAN_DEFINITIONS.get(beanClass.getName());
    for (String candidate : candidates) {
      BeanDefinition beanDefinition = registry.getBeanDefinition(candidate);
      if (Objects.equals(beanDefinition.getBeanClassName(), beanClass.getName())) {
        return false;
      }

      if (reservedBeanDefinition != null && beanDefinition.getSource() != null && beanDefinition.getSource() instanceof MethodMetadata) {
        MethodMetadata metadata = (MethodMetadata) beanDefinition.getSource();
        if (Objects.equals(reservedBeanDefinition, String.format("%s#%s", metadata.getDeclaringClassName(), metadata.getMethodName()))) {
          return false;
        }
      }
    }

    BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(beanClass).getBeanDefinition();

    if (extraPropertyValues != null) {
      for (Map.Entry<String, Object> entry : extraPropertyValues.entrySet()) {
        beanDefinition.getPropertyValues().add(entry.getKey(), entry.getValue());
      }
    }

    registry.registerBeanDefinition(beanName, beanDefinition);

    return true;
  }

}