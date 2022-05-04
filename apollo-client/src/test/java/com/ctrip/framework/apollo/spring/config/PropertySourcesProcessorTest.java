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
package com.ctrip.framework.apollo.spring.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.AbstractSpringIntegrationTest;
import com.ctrip.framework.apollo.spring.events.ApolloConfigChangeEvent;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

public class PropertySourcesProcessorTest extends AbstractSpringIntegrationTest {

  private ConfigurableEnvironment environment;
  private ConfigurableListableBeanFactory beanFactory;
  private PropertySourcesProcessor processor;
  private MutablePropertySources propertySources;
  private ApplicationEventPublisher applicationEventPublisher;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    propertySources = mock(MutablePropertySources.class);
    environment = mock(ConfigurableEnvironment.class);
    when(environment.getPropertySources()).thenReturn(propertySources);
    beanFactory = mock(ConfigurableListableBeanFactory.class);
    applicationEventPublisher = mock(ApplicationEventPublisher.class);
    processor = new PropertySourcesProcessor();
    processor.setEnvironment(environment);
    processor.setApplicationEventPublisher(applicationEventPublisher);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    PropertySourcesProcessor.reset();
  }

  @Test
  public void testInitializePropertySources() {
    String namespaceName = "someNamespace";
    String anotherNamespaceName = "anotherNamespace";
    Config config = mock(Config.class);
    Config anotherConfig = mock(Config.class);
    mockConfig(namespaceName, config);
    mockConfig(anotherNamespaceName, anotherConfig);
    PropertySourcesProcessor.addNamespaces(Lists.newArrayList(namespaceName, anotherNamespaceName),
        0);

    processor.postProcessBeanFactory(beanFactory);

    ArgumentCaptor<CompositePropertySource> argumentCaptor = ArgumentCaptor.forClass(
        CompositePropertySource.class);
    verify(propertySources).addFirst(argumentCaptor.capture());

    CompositePropertySource compositePropertySource = argumentCaptor.getValue();
    assertEquals(2, compositePropertySource.getPropertySources().size());

    ConfigPropertySource propertySource = (ConfigPropertySource) Lists.newArrayList(
        compositePropertySource.getPropertySources()).get(0);
    ConfigPropertySource anotherPropertySource = (ConfigPropertySource) Lists.newArrayList(
        compositePropertySource.getPropertySources()).get(1);

    assertEquals(namespaceName, propertySource.getName());
    assertSame(config, propertySource.getSource());
    assertEquals(anotherNamespaceName, anotherPropertySource.getName());
    assertSame(anotherConfig, anotherPropertySource.getSource());
  }

  @Test
  public void testApplicationEvent() {
    String namespaceName = "someNamespace";
    Config config = mock(Config.class);
    mockConfig(namespaceName, config);
    PropertySourcesProcessor.addNamespaces(Lists.newArrayList(namespaceName), 0);
    ConfigChangeEvent someConfigChangeEvent = mock(ConfigChangeEvent.class);

    processor.postProcessBeanFactory(beanFactory);

    ArgumentCaptor<ConfigChangeListener> argumentCaptor = ArgumentCaptor.forClass(
        ConfigChangeListener.class);
    verify(config).addChangeListener(argumentCaptor.capture());

    ConfigChangeListener listener = argumentCaptor.getValue();
    listener.onChange(someConfigChangeEvent);

    ArgumentCaptor<ApolloConfigChangeEvent> eventCaptor = ArgumentCaptor.forClass(
        ApolloConfigChangeEvent.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

    ApolloConfigChangeEvent event = eventCaptor.getValue();
    assertSame(someConfigChangeEvent, event.getConfigChangeEvent());
  }
}
