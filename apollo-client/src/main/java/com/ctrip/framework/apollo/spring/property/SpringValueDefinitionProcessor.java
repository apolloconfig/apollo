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
package com.ctrip.framework.apollo.spring.property;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * To process xml config placeholders, e.g.
 *
 * <pre>
 *  &lt;bean class=&quot;com.ctrip.framework.apollo.demo.spring.xmlConfigDemo.bean.XmlBean&quot;&gt;
 *    &lt;property name=&quot;timeout&quot; value=&quot;${timeout:200}&quot;/&gt;
 *    &lt;property name=&quot;batch&quot; value=&quot;${batch:100}&quot;/&gt;
 *  &lt;/bean&gt;
 * </pre>
 */
public class SpringValueDefinitionProcessor implements BeanDefinitionRegistryPostProcessor {
  private static final Map<BeanDefinitionRegistry, Multimap<String, SpringValueDefinition>>
      beanName2SpringValueDefinitions = Maps.newConcurrentMap();
  private static final Set<BeanDefinitionRegistry> PROPERTY_VALUES_PROCESSED_BEAN_FACTORIES =
      Sets.newConcurrentHashSet();

  private final ConfigUtil configUtil;
  private final PlaceholderHelper placeholderHelper;

  public SpringValueDefinitionProcessor() {
    configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    placeholderHelper = SpringInjector.getInstance(PlaceholderHelper.class);
  }

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
      throws BeansException {
    if (configUtil.isAutoUpdateInjectedSpringPropertiesEnabled()) {
      processPropertyValues(registry);
    }
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {}

  public static Multimap<String, SpringValueDefinition> getBeanName2SpringValueDefinitions(
      BeanDefinitionRegistry registry) {
    Multimap<String, SpringValueDefinition> springValueDefinitions =
        beanName2SpringValueDefinitions.get(registry);
    if (springValueDefinitions == null) {
      springValueDefinitions = LinkedListMultimap.create();
    }

    return springValueDefinitions;
  }

  private void processPropertyValues(BeanDefinitionRegistry beanRegistry) {
    if (!PROPERTY_VALUES_PROCESSED_BEAN_FACTORIES.add(beanRegistry)) {
      // already initialized
      return;
    }

    if (!beanName2SpringValueDefinitions.containsKey(beanRegistry)) {
      beanName2SpringValueDefinitions.put(
          beanRegistry, LinkedListMultimap.<String, SpringValueDefinition>create());
    }

    Multimap<String, SpringValueDefinition> springValueDefinitions =
        beanName2SpringValueDefinitions.get(beanRegistry);

    String[] beanNames = beanRegistry.getBeanDefinitionNames();
    for (String beanName : beanNames) {
      BeanDefinition beanDefinition = beanRegistry.getBeanDefinition(beanName);
      MutablePropertyValues mutablePropertyValues = beanDefinition.getPropertyValues();
      List<PropertyValue> propertyValues = mutablePropertyValues.getPropertyValueList();
      for (PropertyValue propertyValue : propertyValues) {
        Object value = propertyValue.getValue();
        if (!(value instanceof TypedStringValue)) {
          continue;
        }
        String placeholder = ((TypedStringValue) value).getValue();
        Set<String> keys = placeholderHelper.extractPlaceholderKeys(placeholder);

        if (keys.isEmpty()) {
          continue;
        }

        for (String key : keys) {
          springValueDefinitions.put(
              beanName, new SpringValueDefinition(key, placeholder, propertyValue.getName()));
        }
      }
    }
  }
}
