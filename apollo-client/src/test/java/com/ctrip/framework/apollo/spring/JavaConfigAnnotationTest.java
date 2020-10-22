package com.ctrip.framework.apollo.spring;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.internals.YamlConfigFile;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class JavaConfigAnnotationTest extends AbstractSpringIntegrationTest {
  private static final String FX_APOLLO_NAMESPACE = "FX.apollo";
  private static final String APPLICATION_YAML_NAMESPACE = "application.yaml";

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
  public void testApolloConfigChangeListener() throws Exception {
    Config applicationConfig = mock(Config.class);
    Config fxApolloConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);
    mockConfig(FX_APOLLO_NAMESPACE, fxApolloConfig);

    final List<ConfigChangeListener> applicationListeners = Lists.newArrayList();
    final List<ConfigChangeListener> fxApolloListeners = Lists.newArrayList();

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        applicationListeners.add(invocation.getArgumentAt(0, ConfigChangeListener.class));

        return Void.class;
      }
    }).when(applicationConfig).addChangeListener(any(ConfigChangeListener.class));

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        fxApolloListeners.add(invocation.getArgumentAt(0, ConfigChangeListener.class));

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

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        applicationListeners.add(invocation.getArgumentAt(0, ConfigChangeListener.class));

        return Void.class;
      }
    }).when(applicationConfig).addChangeListener(any(ConfigChangeListener.class));

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        fxApolloListeners.add(invocation.getArgumentAt(0, ConfigChangeListener.class));

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
        .addChangeListener(any(ConfigChangeListener.class), applicationConfigInterestedKeys.capture(), anySetOf(String.class));

    verify(fxApolloConfig, times(1))
        .addChangeListener(any(ConfigChangeListener.class), fxApolloConfigInterestedKeys.capture(), anySetOf(String.class));

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

  private <T> T getBean(Class<T> beanClass, Class<?>... annotatedClasses) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(annotatedClasses);

    return context.getBean(beanClass);
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
