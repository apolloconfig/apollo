package com.ctrip.framework.apollo.spring;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;

import java.util.Set;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.google.common.collect.Lists;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class XMLConfigAnnotationTest extends AbstractSpringIntegrationTest {
  private static final String FX_APOLLO_NAMESPACE = "FX.apollo";

  @Test
  public void testApolloConfig() throws Exception {
    Config applicationConfig = mock(Config.class);
    Config fxApolloConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);
    mockConfig(FX_APOLLO_NAMESPACE, fxApolloConfig);

    TestApolloConfigBean1 bean = getBean("spring/XmlConfigAnnotationTest1.xml", TestApolloConfigBean1.class);

    assertEquals(applicationConfig, bean.getConfig());
    assertEquals(applicationConfig, bean.getAnotherConfig());
    assertEquals(fxApolloConfig, bean.getYetAnotherConfig());
  }

  @Test(expected = BeanCreationException.class)
  public void testApolloConfigWithWrongFieldType() throws Exception {
    Config applicationConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);

    getBean("spring/XmlConfigAnnotationTest2.xml", TestApolloConfigBean2.class);
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

    TestApolloConfigChangeListenerBean1 bean = getBean("spring/XmlConfigAnnotationTest3.xml",
        TestApolloConfigChangeListenerBean1.class);

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

    getBean("spring/XmlConfigAnnotationTest4.xml", TestApolloConfigChangeListenerBean2.class);
  }

  @Test(expected = BeanCreationException.class)
  public void testApolloConfigChangeListenerWithWrongParamCount() throws Exception {
    Config applicationConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);

    getBean("spring/XmlConfigAnnotationTest5.xml", TestApolloConfigChangeListenerBean3.class);
  }

  @Test
  public void testApolloConfigChangeListenerWithInterestedKeys() throws Exception {
    Config applicationConfig = mock(Config.class);
    Config fxApolloConfig = mock(Config.class);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, applicationConfig);
    mockConfig(FX_APOLLO_NAMESPACE, fxApolloConfig);

    TestApolloConfigChangeListenerWithInterestedKeysBean bean = getBean(
        "spring/XmlConfigAnnotationTest6.xml", TestApolloConfigChangeListenerWithInterestedKeysBean.class);

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

  private <T> T getBean(String xmlLocation, Class<T> beanClass) {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(xmlLocation);

    return context.getBean(beanClass);
  }

  public static class TestApolloConfigBean1 {
    @ApolloConfig
    private Config config;
    @ApolloConfig(ConfigConsts.NAMESPACE_APPLICATION)
    private Config anotherConfig;
    @ApolloConfig(FX_APOLLO_NAMESPACE)
    private Config yetAnotherConfig;

    public Config getConfig() {
      return config;
    }

    public Config getAnotherConfig() {
      return anotherConfig;
    }

    public Config getYetAnotherConfig() {
      return yetAnotherConfig;
    }
  }

  public static class TestApolloConfigBean2 {
    @ApolloConfig
    private String config;
  }

  public static class TestApolloConfigChangeListenerBean1 {
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

  public static class TestApolloConfigChangeListenerBean2 {
    @ApolloConfigChangeListener
    private void onChange(String event) {

    }
  }

  public static class TestApolloConfigChangeListenerBean3 {
    @ApolloConfigChangeListener
    private void onChange(ConfigChangeEvent event, String someParam) {

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
}
