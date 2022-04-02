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
package com.ctrip.framework.apollo.spring;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigFileChangeListener;
import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.internals.SimpleConfig;
import com.ctrip.framework.apollo.internals.YamlConfigFile;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class JavaConfigAnnotationTest extends AbstractSpringIntegrationTest {
  private static final String FX_APOLLO_NAMESPACE = "FX.apollo";
  private static final String APPLICATION_YAML_NAMESPACE = "application.yaml";

  private static <T> T getBean(Class<T> beanClass, Class<?>... annotatedClasses) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(annotatedClasses);
    return context.getBean(beanClass);
  }

  private static <T> T getSimpleBean(Class<? extends T> clazz) {
    return getBean(clazz, clazz);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    // clear the system properties
    System.clearProperty(SystemPropertyKeyConstants.SIMPLE_NAMESPACE);
    System.clearProperty(SystemPropertyKeyConstants.REDIS_NAMESPACE);
    System.clearProperty(SystemPropertyKeyConstants.FROM_SYSTEM_NAMESPACE);
    System.clearProperty(SystemPropertyKeyConstants.FROM_SYSTEM_YAML_NAMESPACE);
    System.clearProperty(SystemPropertyKeyConstants.FROM_NAMESPACE_APPLICATION_KEY);
    System.clearProperty(SystemPropertyKeyConstants.FROM_NAMESPACE_APPLICATION_KEY_YAML);
    System.clearProperty(ApolloClientSystemConsts.APOLLO_PROPERTY_NAMES_CACHE_ENABLE);
    super.tearDown();
  }

  @Test
  public void testApolloConfig() throws Exception {
    Config applicationConfig = mock(Config.class);
    Config fxApolloConfig = mock(Config.class);
    String someKey = "someKey";
    String someValue = "someValue";

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);
    mockConfig(FX_APOLLO_NAMESPACE, fxApolloConfig);

    prepareYamlConfigFile(APPLICATION_YAML_NAMESPACE, readYamlContentAsConfigFileProperties("case9.yml"));

    TestApolloConfigBean1 bean = getBean(TestApolloConfigBean1.class, AppConfig1.class);

    assertEquals(applicationConfig, bean.getConfig());
    assertEquals(applicationConfig, bean.getAnotherConfig());
    assertEquals(fxApolloConfig, bean.getYetAnotherConfig());

    Config yamlConfig = bean.getYamlConfig();
    assertEquals(someValue, yamlConfig.getProperty(someKey, null));
  }

  @Test(expected = BeanCreationException.class)
  public void testApolloConfigWithWrongFieldType() throws Exception {
    Config applicationConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);

    getBean(TestApolloConfigBean2.class, AppConfig2.class);
  }

  @Test
  public void testApolloConfigWithInheritance() throws Exception {
    Config applicationConfig = mock(Config.class);
    Config fxApolloConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);
    mockConfig(FX_APOLLO_NAMESPACE, fxApolloConfig);
    prepareYamlConfigFile(APPLICATION_YAML_NAMESPACE, readYamlContentAsConfigFileProperties("case9.yml"));

    TestApolloChildConfigBean bean = getBean(TestApolloChildConfigBean.class, AppConfig6.class);

    assertEquals(applicationConfig, bean.getConfig());
    assertEquals(applicationConfig, bean.getAnotherConfig());
    assertEquals(fxApolloConfig, bean.getYetAnotherConfig());
    assertEquals(applicationConfig, bean.getSomeConfig());
  }

  @Test
  public void testEnableApolloConfigResolveExpressionSimple() {
    String someKey = "someKey-2020-11-14-1750";
    String someValue = UUID.randomUUID().toString();
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, mock(Config.class));
    Config xxxConfig = mock(Config.class);
    when(xxxConfig.getProperty(eq(someKey), Mockito.nullable(String.class))).thenReturn(someValue);
    mockConfig("xxx", xxxConfig);

    TestEnableApolloConfigResolveExpressionWithDefaultValueConfiguration configuration =
        getSimpleBean(TestEnableApolloConfigResolveExpressionWithDefaultValueConfiguration.class);

    // check
    assertEquals(someValue, configuration.getSomeKey());
    verify(xxxConfig, times(1)).getProperty(eq(someKey), Mockito.nullable(String.class));
  }

  @Test
  public void testEnableApolloConfigResolveExpressionFromSystemProperty() {
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, mock(Config.class));
    final String someKey = "someKey-2020-11-14-1750";
    final String someValue = UUID.randomUUID().toString();

    final String resolvedNamespaceName = "yyy";
    System.setProperty(SystemPropertyKeyConstants.SIMPLE_NAMESPACE, resolvedNamespaceName);

    Config yyyConfig = mock(Config.class);
    when(yyyConfig.getProperty(eq(someKey), Mockito.nullable(String.class))).thenReturn(someValue);
    mockConfig(resolvedNamespaceName, yyyConfig);

    TestEnableApolloConfigResolveExpressionWithDefaultValueConfiguration configuration =
        getSimpleBean(TestEnableApolloConfigResolveExpressionWithDefaultValueConfiguration.class);

    // check
    assertEquals(someValue, configuration.getSomeKey());
    verify(yyyConfig, times(1)).getProperty(eq(someKey), Mockito.nullable(String.class));
  }

  @Test(expected = BeanCreationException.class)
  public void testEnableApolloConfigUnresolvedValueInField() {
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, mock(Config.class));
    mockConfig("xxx", mock(Config.class));
    getSimpleBean(TestEnableApolloConfigResolveExpressionWithDefaultValueConfiguration.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEnableApolloConfigUnresolvable() {
    getSimpleBean(TestEnableApolloConfigUnresolvableConfiguration.class);
  }

  @Test
  public void testApolloConfigChangeListener() throws Exception {
    Config applicationConfig = mock(Config.class);
    Config fxApolloConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);
    mockConfig(FX_APOLLO_NAMESPACE, fxApolloConfig);

    final List<ConfigChangeListener> applicationListeners = Lists.newArrayList();
    final List<ConfigChangeListener> fxApolloListeners = Lists.newArrayList();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        applicationListeners.add(invocation.getArgument(0, ConfigChangeListener.class));

        return Void.class;
      }
    }).when(applicationConfig).addChangeListener(any(ConfigChangeListener.class));

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        fxApolloListeners.add(invocation.getArgument(0, ConfigChangeListener.class));

        return Void.class;
      }
    }).when(fxApolloConfig).addChangeListener(any(ConfigChangeListener.class));

    ConfigChangeEvent someEvent = mock(ConfigChangeEvent.class);
    ConfigChangeEvent anotherEvent = mock(ConfigChangeEvent.class);

    TestApolloConfigChangeListenerBean1 bean = getBean(TestApolloConfigChangeListenerBean1.class, AppConfig3.class);

    //PropertySourcesProcessor add listeners to listen config changed of all namespace
    assertEquals(4, applicationListeners.size());
    assertEquals(1, fxApolloListeners.size());

    for (ConfigChangeListener listener : applicationListeners) {
      listener.onChange(someEvent);
    }

    assertEquals(someEvent, bean.getChangeEvent1());
    assertEquals(someEvent, bean.getChangeEvent2());
    assertEquals(someEvent, bean.getChangeEvent3());

    for (ConfigChangeListener listener : fxApolloListeners) {
      listener.onChange(anotherEvent);
    }

    assertEquals(someEvent, bean.getChangeEvent1());
    assertEquals(someEvent, bean.getChangeEvent2());
    assertEquals(anotherEvent, bean.getChangeEvent3());
  }

  @Test(expected = BeanCreationException.class)
  public void testApolloConfigChangeListenerWithWrongParamType() throws Exception {
    Config applicationConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);

    getBean(TestApolloConfigChangeListenerBean2.class, AppConfig4.class);
  }

  @Test(expected = BeanCreationException.class)
  public void testApolloConfigChangeListenerWithWrongParamCount() throws Exception {
    Config applicationConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);

    getBean(TestApolloConfigChangeListenerBean3.class, AppConfig5.class);
  }

  @Test
  public void testApolloConfigChangeListenerWithInheritance() throws Exception {
    Config applicationConfig = mock(Config.class);
    Config fxApolloConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);
    mockConfig(FX_APOLLO_NAMESPACE, fxApolloConfig);

    final List<ConfigChangeListener> applicationListeners = Lists.newArrayList();
    final List<ConfigChangeListener> fxApolloListeners = Lists.newArrayList();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        applicationListeners.add(invocation.getArgument(0, ConfigChangeListener.class));

        return Void.class;
      }
    }).when(applicationConfig).addChangeListener(any(ConfigChangeListener.class));

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        fxApolloListeners.add(invocation.getArgument(0, ConfigChangeListener.class));

        return Void.class;
      }
    }).when(fxApolloConfig).addChangeListener(any(ConfigChangeListener.class));

    ConfigChangeEvent someEvent = mock(ConfigChangeEvent.class);
    ConfigChangeEvent anotherEvent = mock(ConfigChangeEvent.class);

    TestApolloChildConfigChangeListener bean = getBean(TestApolloChildConfigChangeListener.class, AppConfig7.class);

    //PropertySourcesProcessor add listeners to listen config changed of all namespace
    assertEquals(5, applicationListeners.size());
    assertEquals(1, fxApolloListeners.size());

    for (ConfigChangeListener listener : applicationListeners) {
      listener.onChange(someEvent);
    }

    assertEquals(someEvent, bean.getChangeEvent1());
    assertEquals(someEvent, bean.getChangeEvent2());
    assertEquals(someEvent, bean.getChangeEvent3());
    assertEquals(someEvent, bean.getSomeChangeEvent());

    for (ConfigChangeListener listener : fxApolloListeners) {
      listener.onChange(anotherEvent);
    }

    assertEquals(someEvent, bean.getChangeEvent1());
    assertEquals(someEvent, bean.getChangeEvent2());
    assertEquals(anotherEvent, bean.getChangeEvent3());
    assertEquals(someEvent, bean.getSomeChangeEvent());
  }

  @Test
  public void testApolloConfigChangeListenerWithInterestedKeys() throws Exception {
    Config applicationConfig = mock(Config.class);
    Config fxApolloConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);
    mockConfig(FX_APOLLO_NAMESPACE, fxApolloConfig);

    TestApolloConfigChangeListenerWithInterestedKeysBean bean = getBean(
        TestApolloConfigChangeListenerWithInterestedKeysBean.class, AppConfig8.class);

    final ArgumentCaptor<Set> applicationConfigInterestedKeys = ArgumentCaptor.forClass(Set.class);
    final ArgumentCaptor<Set> fxApolloConfigInterestedKeys = ArgumentCaptor.forClass(Set.class);

    verify(applicationConfig, times(2))
        .addChangeListener(any(ConfigChangeListener.class), applicationConfigInterestedKeys.capture(), Mockito.nullable(Set.class));

    verify(fxApolloConfig, times(1))
        .addChangeListener(any(ConfigChangeListener.class), fxApolloConfigInterestedKeys.capture(), Mockito.nullable(Set.class));

    assertEquals(2, applicationConfigInterestedKeys.getAllValues().size());

    Set<String> result = Sets.newHashSet();
    for (Set interestedKeys : applicationConfigInterestedKeys.getAllValues()) {
      result.addAll(interestedKeys);
    }
    assertEquals(Sets.newHashSet("someKey", "anotherKey"), result);

    assertEquals(1, fxApolloConfigInterestedKeys.getAllValues().size());

    assertEquals(Collections.singletonList(Sets.newHashSet("anotherKey")), fxApolloConfigInterestedKeys.getAllValues());
  }

  @Test
  public void testApolloConfigChangeListenerWithInterestedKeyPrefixes() {
    Config applicationConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);

    TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean bean = getBean(
        TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean.class, AppConfig10.class);

    final ArgumentCaptor<Set> interestedKeyPrefixesArgumentCaptor = ArgumentCaptor
        .forClass(Set.class);

    verify(applicationConfig, times(1))
        .addChangeListener(any(ConfigChangeListener.class), Mockito.nullable(Set.class),
            interestedKeyPrefixesArgumentCaptor.capture());

    assertEquals(1, interestedKeyPrefixesArgumentCaptor.getAllValues().size());

    Set<String> result = Sets.newHashSet();
    for (Set<String> interestedKeyPrefixes : interestedKeyPrefixesArgumentCaptor.getAllValues()) {
      result.addAll(interestedKeyPrefixes);
    }
    assertEquals(Sets.newHashSet("logging.level", "number"), result);
  }

  @Test
  public void testApolloConfigChangeListenerWithInterestedKeyPrefixes_fire()
      throws InterruptedException {
    // default mock, useless here
    // just for speed up test without waiting
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, mock(Config.class));

    SimpleConfig simpleConfig = spy(
        this.prepareConfig(
            TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean1.SPECIAL_NAMESPACE,
            new Properties()));

    mockConfig(TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean1.SPECIAL_NAMESPACE,
        simpleConfig);

    TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean1 bean = getBean(
        TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean1.class, AppConfig11.class);

    verify(simpleConfig, atLeastOnce())
        .addChangeListener(any(ConfigChangeListener.class), Mockito.nullable(Set.class),
            anySet());

    Properties properties = new Properties();
    properties.put("logging.level.com", "debug");
    properties.put("logging.level.root", "warn");
    properties.put("number.value", "333");

    // publish config change
    simpleConfig.onRepositoryChange(
        TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean1.SPECIAL_NAMESPACE, properties);

    // get event from bean
    ConfigChangeEvent configChangeEvent = bean.getConfigChangeEvent();
    Set<String> interestedChangedKeys = configChangeEvent.interestedChangedKeys();
    assertEquals(Sets.newHashSet("logging.level.com", "logging.level.root", "number.value"),
        interestedChangedKeys);
  }

  @Test
  public void testApolloConfigChangeListenerWithYamlFile() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    String anotherValue = "anotherValue";

    YamlConfigFile configFile = prepareYamlConfigFile(APPLICATION_YAML_NAMESPACE,
        readYamlContentAsConfigFileProperties("case9.yml"));

    TestApolloConfigChangeListenerWithYamlFile bean = getBean(TestApolloConfigChangeListenerWithYamlFile.class, AppConfig9.class);

    Config yamlConfig = bean.getYamlConfig();
    SettableFuture<ConfigChangeEvent> future = bean.getConfigChangeEventFuture();

    assertEquals(someValue, yamlConfig.getProperty(someKey, null));
    assertFalse(future.isDone());

    configFile.onRepositoryChange(APPLICATION_YAML_NAMESPACE, readYamlContentAsConfigFileProperties("case9-new.yml"));

    ConfigChangeEvent configChangeEvent = future.get(100, TimeUnit.MILLISECONDS);
    ConfigChange change = configChangeEvent.getChange(someKey);
    assertEquals(someValue, change.getOldValue());
    assertEquals(anotherValue, change.getNewValue());

    assertEquals(anotherValue, yamlConfig.getProperty(someKey, null));
  }

  @Test
  public void testApolloConfigChangeListenerResolveExpressionSimple() {
    // for ignore, no listener use it
    Config ignoreConfig = mock(Config.class);
    mockConfig("ignore.for.listener", ignoreConfig);

    Config applicationConfig = mock(Config.class);
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);

    System.setProperty(ApolloClientSystemConsts.APOLLO_PROPERTY_NAMES_CACHE_ENABLE, "true");

    getSimpleBean(TestApolloConfigChangeListenerResolveExpressionSimpleConfiguration.class);

    // no using
    verify(ignoreConfig, never()).addChangeListener(any(ConfigChangeListener.class));

    // one invocation for spring value auto update
    // one invocation for the @ApolloConfigChangeListener annotation
    // one invocation for CachedCompositePropertySource clear cache listener
    verify(applicationConfig, times(3)).addChangeListener(any(ConfigChangeListener.class));
  }

  /**
   * resolve namespace's name from system property.
   */
  @Test
  public void testApolloConfigChangeListenerResolveExpressionFromSystemProperty() {
    Config applicationConfig = mock(Config.class);
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);

    final String namespaceName = "magicRedis";
    System.setProperty(SystemPropertyKeyConstants.REDIS_NAMESPACE, namespaceName);
    Config redisConfig = mock(Config.class);
    mockConfig(namespaceName, redisConfig);
    getSimpleBean(
        TestApolloConfigChangeListenerResolveExpressionFromSystemPropertyConfiguration.class);

    // if config was used, it must be invoked on method addChangeListener 1 time
    verify(redisConfig, times(1)).addChangeListener(any(ConfigChangeListener.class));
  }

  /**
   * resolve namespace from config. ${mysql.namespace} will be resolved by config from namespace
   * application.
   */
  @Test
  public void testApolloConfigChangeListenerResolveExpressionFromApplicationNamespace() {
    final String namespaceKey = "mysql.namespace";
    final String namespaceName = "magicMysqlNamespaceApplication";

    Properties properties = new Properties();
    properties.setProperty(namespaceKey, namespaceName);
    this.prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, properties);

    Config mysqlConfig = mock(Config.class);
    mockConfig(namespaceName, mysqlConfig);

    getSimpleBean(
        TestApolloConfigChangeListenerResolveExpressionFromApplicationNamespaceConfiguration.class);

    // if config was used, it must be invoked on method addChangeListener 1 time
    verify(mysqlConfig, times(1)).addChangeListener(any(ConfigChangeListener.class));
  }

  @Test(expected = BeanCreationException.class)
  public void testApolloConfigChangeListenerUnresolvedPlaceholder() {
    Config applicationConfig = mock(Config.class);
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);
    getSimpleBean(TestApolloConfigChangeListenerUnresolvedPlaceholderConfiguration.class);
  }

  @Test
  public void testApolloConfigChangeListenerResolveExpressionFromSelfYaml() throws IOException {
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, mock(Config.class));

    final String resolvedValue = "resolve.from.self.yml";
    YamlConfigFile yamlConfigFile = prepareYamlConfigFile(resolvedValue, readYamlContentAsConfigFileProperties(resolvedValue));
    getSimpleBean(TestApolloConfigChangeListenerResolveExpressionFromSelfYamlConfiguration.class);
    verify(yamlConfigFile, times(1)).addChangeListener(any(ConfigFileChangeListener.class));
  }

  @Test
  public void testApolloConfigResolveExpressionDefault() {
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, mock(Config.class));
    Config defaultConfig = mock(Config.class);
    Config yamlConfig = mock(Config.class);
    mockConfig("default-2020-11-14-1733", defaultConfig);
    mockConfig(APPLICATION_YAML_NAMESPACE, yamlConfig);
    TestApolloConfigResolveExpressionDefaultConfiguration configuration = getSimpleBean(
        TestApolloConfigResolveExpressionDefaultConfiguration.class);
    assertSame(defaultConfig, configuration.getDefaultConfig());
    assertSame(yamlConfig, configuration.getYamlConfig());
  }

  @Test
  public void testApolloConfigResolveExpressionFromSystemProperty() {
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, mock(Config.class));
    final String namespaceName = "xxx6";
    final String yamlNamespaceName = "yyy8.yml";

    System.setProperty(SystemPropertyKeyConstants.FROM_SYSTEM_NAMESPACE, namespaceName);
    System.setProperty(SystemPropertyKeyConstants.FROM_SYSTEM_YAML_NAMESPACE, yamlNamespaceName);
    Config config = mock(Config.class);
    Config yamlConfig = mock(Config.class);
    mockConfig(namespaceName, config);
    mockConfig(yamlNamespaceName, yamlConfig);
    TestApolloConfigResolveExpressionFromSystemPropertyConfiguration configuration = getSimpleBean(
        TestApolloConfigResolveExpressionFromSystemPropertyConfiguration.class);
    assertSame(config, configuration.getConfig());
    assertSame(yamlConfig, configuration.getYamlConfig());
  }

  @Test(expected = BeanCreationException.class)
  public void testApolloConfigUnresolvedExpression() {
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, mock(Config.class));
    getSimpleBean(TestApolloConfigUnresolvedExpressionConfiguration.class);
  }

  @Test
  public void testApolloConfigResolveExpressionFromApolloConfigNamespaceApplication() {

    final String namespaceName = "xxx6";
    final String yamlNamespaceName = "yyy8.yml";
    {
      // hide variable scope
      Properties properties = new Properties();
      properties.setProperty(SystemPropertyKeyConstants.FROM_NAMESPACE_APPLICATION_KEY, namespaceName);
      properties.setProperty(SystemPropertyKeyConstants.FROM_NAMESPACE_APPLICATION_KEY_YAML, yamlNamespaceName);
      this.prepareConfig(ConfigConsts.NAMESPACE_APPLICATION, properties);
    }
    final Config config = mock(Config.class);
    final Config yamlConfig = mock(Config.class);
    mockConfig(namespaceName, config);
    mockConfig(yamlNamespaceName, yamlConfig);
    TestApolloConfigResolveExpressionFromApolloConfigNamespaceApplication configuration = getSimpleBean(
        TestApolloConfigResolveExpressionFromApolloConfigNamespaceApplication.class);
    assertSame(config, configuration.getConfig());
    assertSame(yamlConfig, configuration.getYamlConfig());
  }

  private static class SystemPropertyKeyConstants {

    static final String SIMPLE_NAMESPACE = "simple.namespace";
    static final String REDIS_NAMESPACE = "redis.namespace";
    static final String FROM_SYSTEM_NAMESPACE = "from.system.namespace";
    static final String FROM_SYSTEM_YAML_NAMESPACE = "from.system.yaml.namespace";
    static final String FROM_NAMESPACE_APPLICATION_KEY = "from.namespace.application.key";
    static final String FROM_NAMESPACE_APPLICATION_KEY_YAML = "from.namespace.application.key.yaml";
  }

  @EnableApolloConfig
  protected static class TestApolloConfigResolveExpressionDefaultConfiguration {

    @ApolloConfig(value = "${simple.namespace:default-2020-11-14-1733}")
    private Config defaultConfig;

    @ApolloConfig(value = "${simple.yaml.namespace:" + APPLICATION_YAML_NAMESPACE + "}")
    private Config yamlConfig;

    public Config getDefaultConfig() {
      return defaultConfig;
    }

    public Config getYamlConfig() {
      return yamlConfig;
    }
  }

  @EnableApolloConfig
  protected static class TestApolloConfigResolveExpressionFromSystemPropertyConfiguration {

    @ApolloConfig(value = "${from.system.namespace}")
    private Config config;

    @ApolloConfig(value = "${from.system.yaml.namespace}")
    private Config yamlConfig;

    public Config getConfig() {
      return config;
    }

    public Config getYamlConfig() {
      return yamlConfig;
    }
  }

  @EnableApolloConfig
  protected static class TestApolloConfigUnresolvedExpressionConfiguration {

    @ApolloConfig(value = "${so.complex.to.resolve}")
    private Config config;
  }

  @EnableApolloConfig
  protected static class TestApolloConfigResolveExpressionFromApolloConfigNamespaceApplication {

    @ApolloConfig(value = "${from.namespace.application.key}")
    private Config config;

    @ApolloConfig(value = "${from.namespace.application.key.yaml}")
    private Config yamlConfig;

    public Config getConfig() {
      return config;
    }

    public Config getYamlConfig() {
      return yamlConfig;
    }
  }


  @Configuration
  @EnableApolloConfig
  static class TestApolloConfigChangeListenerResolveExpressionSimpleConfiguration {

    @ApolloConfigChangeListener("${simple.application:application}")
    private void onChange(ConfigChangeEvent event) {
    }
  }

  @Configuration
  @EnableApolloConfig
  static class TestApolloConfigChangeListenerResolveExpressionFromSystemPropertyConfiguration {

    @ApolloConfigChangeListener("${redis.namespace}")
    private void onChange(ConfigChangeEvent event) {
    }
  }

  @Configuration
  @EnableApolloConfig
  static class TestApolloConfigChangeListenerResolveExpressionFromApplicationNamespaceConfiguration {

    @ApolloConfigChangeListener(value = {ConfigConsts.NAMESPACE_APPLICATION,
        "${mysql.namespace}"})
    private void onChange(ConfigChangeEvent event) {
    }
  }

  @Configuration
  @EnableApolloConfig
  static class TestApolloConfigChangeListenerUnresolvedPlaceholderConfiguration {
    @ApolloConfigChangeListener(value = {ConfigConsts.NAMESPACE_APPLICATION,
        "${i.can.not.be.resolved}"})
    private void onChange(ConfigChangeEvent event) {
    }
  }

  @Configuration
  @EnableApolloConfig("resolve.from.self.yml")
  static class TestApolloConfigChangeListenerResolveExpressionFromSelfYamlConfiguration {

    /**
     * value in file src/test/resources/spring/yaml/resolve.from.self.yml
     */
    @ApolloConfigChangeListener("${i.can.resolve.from.self}")
    private void onChange(ConfigChangeEvent event) {
    }
  }

  @Configuration
  @EnableApolloConfig(value = {ConfigConsts.NAMESPACE_APPLICATION, "${simple.namespace:xxx}"})
  static class TestEnableApolloConfigResolveExpressionWithDefaultValueConfiguration {

    @Value("${someKey-2020-11-14-1750}")
    private String someKey;

    public String getSomeKey() {
      return this.someKey;
    }
  }

  @Configuration
  @EnableApolloConfig(value = "${unresolvable.property}")
  static class TestEnableApolloConfigUnresolvableConfiguration {

  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig1 {
    @Bean
    public TestApolloConfigBean1 bean() {
      return new TestApolloConfigBean1();
    }
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig2 {
    @Bean
    public TestApolloConfigBean2 bean() {
      return new TestApolloConfigBean2();
    }
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig3 {
    @Bean
    public TestApolloConfigChangeListenerBean1 bean() {
      return new TestApolloConfigChangeListenerBean1();
    }
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig4 {
    @Bean
    public TestApolloConfigChangeListenerBean2 bean() {
      return new TestApolloConfigChangeListenerBean2();
    }
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig5 {
    @Bean
    public TestApolloConfigChangeListenerBean3 bean() {
      return new TestApolloConfigChangeListenerBean3();
    }
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig6 {
    @Bean
    public TestApolloChildConfigBean bean() {
      return new TestApolloChildConfigBean();
    }
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig7 {
    @Bean
    public TestApolloChildConfigChangeListener bean() {
      return new TestApolloChildConfigChangeListener();
    }
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig8 {
    @Bean
    public TestApolloConfigChangeListenerWithInterestedKeysBean bean() {
      return new TestApolloConfigChangeListenerWithInterestedKeysBean();
    }
  }

  @Configuration
  @EnableApolloConfig(APPLICATION_YAML_NAMESPACE)
  static class AppConfig9 {
    @Bean
    public TestApolloConfigChangeListenerWithYamlFile bean() {
      return new TestApolloConfigChangeListenerWithYamlFile();
    }
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig10 {
    @Bean
    public TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean bean() {
      return new TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean();
    }
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig11 {
    @Bean
    public TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean1 bean() {
      return spy(new TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean1());
    }
  }

  static class TestApolloConfigBean1 {
    @ApolloConfig
    private Config config;
    @ApolloConfig(ConfigConsts.NAMESPACE_APPLICATION)
    private Config anotherConfig;
    @ApolloConfig(FX_APOLLO_NAMESPACE)
    private Config yetAnotherConfig;
    @ApolloConfig(APPLICATION_YAML_NAMESPACE)
    private Config yamlConfig;

    public Config getConfig() {
      return config;
    }

    public Config getAnotherConfig() {
      return anotherConfig;
    }

    public Config getYetAnotherConfig() {
      return yetAnotherConfig;
    }

    public Config getYamlConfig() {
      return yamlConfig;
    }
  }

  static class TestApolloConfigBean2 {
    @ApolloConfig
    private String config;
  }

  static class TestApolloChildConfigBean extends TestApolloConfigBean1 {

    @ApolloConfig
    private Config someConfig;

    public Config getSomeConfig() {
      return someConfig;
    }
  }

  static class TestApolloConfigChangeListenerBean1 {
    private ConfigChangeEvent changeEvent1;
    private ConfigChangeEvent changeEvent2;
    private ConfigChangeEvent changeEvent3;

    @ApolloConfigChangeListener
    private void onChange1(ConfigChangeEvent changeEvent) {
      this.changeEvent1 = changeEvent;
    }

    @ApolloConfigChangeListener(ConfigConsts.NAMESPACE_APPLICATION)
    private void onChange2(ConfigChangeEvent changeEvent) {
      this.changeEvent2 = changeEvent;
    }

    @ApolloConfigChangeListener({ConfigConsts.NAMESPACE_APPLICATION, FX_APOLLO_NAMESPACE})
    private void onChange3(ConfigChangeEvent changeEvent) {
      this.changeEvent3 = changeEvent;
    }

    public ConfigChangeEvent getChangeEvent1() {
      return changeEvent1;
    }

    public ConfigChangeEvent getChangeEvent2() {
      return changeEvent2;
    }

    public ConfigChangeEvent getChangeEvent3() {
      return changeEvent3;
    }
  }

  static class TestApolloConfigChangeListenerBean2 {
    @ApolloConfigChangeListener
    private void onChange(String event) {

    }
  }

  static class TestApolloConfigChangeListenerBean3 {
    @ApolloConfigChangeListener
    private void onChange(ConfigChangeEvent event, String someParam) {

    }
  }

  static class TestApolloChildConfigChangeListener extends TestApolloConfigChangeListenerBean1 {

    private ConfigChangeEvent someChangeEvent;

    @ApolloConfigChangeListener
    private void someOnChange(ConfigChangeEvent changeEvent) {
      this.someChangeEvent = changeEvent;
    }

    public ConfigChangeEvent getSomeChangeEvent() {
      return someChangeEvent;
    }
  }

  static class TestApolloConfigChangeListenerWithInterestedKeysBean {

    @ApolloConfigChangeListener(interestedKeys = {"someKey"})
    private void someOnChange(ConfigChangeEvent changeEvent) {}

    @ApolloConfigChangeListener(value = {ConfigConsts.NAMESPACE_APPLICATION, FX_APOLLO_NAMESPACE},
        interestedKeys = {"anotherKey"})
    private void anotherOnChange(ConfigChangeEvent changeEvent) {

    }
  }

  private static class TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean {

    @ApolloConfigChangeListener(interestedKeyPrefixes = {"number", "logging.level"})
    private void onChange(ConfigChangeEvent changeEvent) {
    }
  }

  private static class TestApolloConfigChangeListenerWithInterestedKeyPrefixesBean1 {

    static final String SPECIAL_NAMESPACE = "special-namespace-2021";

    private final BlockingQueue<ConfigChangeEvent> configChangeEventQueue = new ArrayBlockingQueue<>(100);

    @ApolloConfigChangeListener(value = SPECIAL_NAMESPACE, interestedKeyPrefixes = {"number",
        "logging.level"})
    private void onChange(ConfigChangeEvent changeEvent) {
      this.configChangeEventQueue.add(changeEvent);
    }

    public ConfigChangeEvent getConfigChangeEvent() throws InterruptedException {
      return this.configChangeEventQueue.poll(5, TimeUnit.SECONDS);
    }
  }

  static class TestApolloConfigChangeListenerWithYamlFile {

    private SettableFuture<ConfigChangeEvent> configChangeEventFuture = SettableFuture.create();

    @ApolloConfig(APPLICATION_YAML_NAMESPACE)
    private Config yamlConfig;

    @ApolloConfigChangeListener(APPLICATION_YAML_NAMESPACE)
    private void onChange(ConfigChangeEvent event) {
      configChangeEventFuture.set(event);
    }

    public SettableFuture<ConfigChangeEvent> getConfigChangeEventFuture() {
      return configChangeEventFuture;
    }

    public Config getYamlConfig() {
      return yamlConfig;
    }
  }
}
