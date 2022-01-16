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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.PropertiesCompatibleConfigFile;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import java.util.List;
import java.util.Properties;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class JavaConfigPlaceholderTest extends AbstractSpringIntegrationTest {
  private static final String TIMEOUT_PROPERTY = "timeout";
  private static final int DEFAULT_TIMEOUT = 100;
  private static final String BATCH_PROPERTY = "batch";
  private static final int DEFAULT_BATCH = 200;
  private static final String FX_APOLLO_NAMESPACE = "FX.apollo";
  private static final String JSON_PROPERTY = "jsonProperty";
  private static final String OTHER_JSON_PROPERTY = "otherJsonProperty";

  @Test
  public void testPropertySourceWithNoNamespace() throws Exception {
    int someTimeout = 1000;
    int someBatch = 2000;

    Config config = mock(Config.class);
    when(config.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someTimeout));
    when(config.getProperty(eq(BATCH_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someBatch));

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    check(someTimeout, someBatch, AppConfig1.class);
  }

  @Test
  public void testPropertySourceWithNoConfig() throws Exception {
    Config config = mock(Config.class);
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);
    check(DEFAULT_TIMEOUT, DEFAULT_BATCH, AppConfig1.class);
  }

  @Test
  public void testApplicationPropertySource() throws Exception {
    int someTimeout = 1000;
    int someBatch = 2000;

    Config config = mock(Config.class);
    when(config.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someTimeout));
    when(config.getProperty(eq(BATCH_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someBatch));

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    check(someTimeout, someBatch, AppConfig2.class);
  }

  @Test
  public void testPropertiesCompatiblePropertySource() throws Exception {
    int someTimeout = 1000;
    int someBatch = 2000;
    Properties properties = mock(Properties.class);

    when(properties.getProperty(TIMEOUT_PROPERTY)).thenReturn(String.valueOf(someTimeout));
    when(properties.getProperty(BATCH_PROPERTY)).thenReturn(String.valueOf(someBatch));
    PropertiesCompatibleConfigFile configFile = mock(PropertiesCompatibleConfigFile.class);
    when(configFile.asProperties()).thenReturn(properties);

    mockConfigFile("application.yaml", configFile);

    check(someTimeout, someBatch, AppConfig9.class);
  }

  @Test
  public void testPropertiesCompatiblePropertySourceWithNonNormalizedCase() throws Exception {
    int someTimeout = 1000;
    int someBatch = 2000;
    Properties properties = mock(Properties.class);

    when(properties.getProperty(TIMEOUT_PROPERTY)).thenReturn(String.valueOf(someTimeout));
    when(properties.getProperty(BATCH_PROPERTY)).thenReturn(String.valueOf(someBatch));
    PropertiesCompatibleConfigFile configFile = mock(PropertiesCompatibleConfigFile.class);
    when(configFile.asProperties()).thenReturn(properties);

    mockConfigFile("application.yaml", configFile);

    check(someTimeout, someBatch, AppConfig10.class);
  }

  @Test
  public void testMultiplePropertySources() throws Exception {
    int someTimeout = 1000;
    int someBatch = 2000;

    Config application = mock(Config.class);
    when(application.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someTimeout));
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, application);

    Config fxApollo = mock(Config.class);
    when(application.getProperty(eq(BATCH_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someBatch));
    mockConfig(FX_APOLLO_NAMESPACE, fxApollo);

    check(someTimeout, someBatch, AppConfig3.class);
  }

  @Test
  public void testMultiplePropertiesCompatiblePropertySourcesWithSameProperties() throws Exception {
    int someTimeout = 1000;
    int anotherTimeout = someTimeout + 1;
    int someBatch = 2000;

    Properties properties = mock(Properties.class);

    when(properties.getProperty(TIMEOUT_PROPERTY)).thenReturn(String.valueOf(someTimeout));
    when(properties.getProperty(BATCH_PROPERTY)).thenReturn(String.valueOf(someBatch));
    PropertiesCompatibleConfigFile configFile = mock(PropertiesCompatibleConfigFile.class);
    when(configFile.asProperties()).thenReturn(properties);

    mockConfigFile("application.yml", configFile);

    Config fxApollo = mock(Config.class);
    when(fxApollo.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(anotherTimeout));
    mockConfig(FX_APOLLO_NAMESPACE, fxApollo);

    check(someTimeout, someBatch, AppConfig11.class);
  }

  @Test
  public void testMultiplePropertySourcesCoverWithSameProperties() throws Exception {
    // Multimap does not maintain the strict input order of namespace.
    int someTimeout = 1000;
    int anotherTimeout = someTimeout + 1;
    int someBatch = 2000;

    Config fxApollo = mock(Config.class);
    when(fxApollo.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someTimeout));
    when(fxApollo.getProperty(eq(BATCH_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someBatch));
    mockConfig(FX_APOLLO_NAMESPACE, fxApollo);

    Config application = mock(Config.class);
    when(application.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(anotherTimeout));
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, application);

    check(someTimeout, someBatch, AppConfig6.class);
  }

  @Test
  public void
      testMultiplePropertySourcesCoverWithSamePropertiesWithPropertiesCompatiblePropertySource()
          throws Exception {
    // Multimap does not maintain the strict input order of namespace.
    int someTimeout = 1000;
    int anotherTimeout = someTimeout + 1;
    int someBatch = 2000;

    Config fxApollo = mock(Config.class);
    when(fxApollo.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someTimeout));
    when(fxApollo.getProperty(eq(BATCH_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someBatch));
    mockConfig(FX_APOLLO_NAMESPACE, fxApollo);

    Config application = mock(Config.class);
    when(application.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(anotherTimeout));
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, application);

    check(someTimeout, someBatch, AppConfig6.class);
  }

  @Test
  public void testMultiplePropertySourcesWithSamePropertiesWithWeight() throws Exception {
    int someTimeout = 1000;
    int anotherTimeout = someTimeout + 1;
    int someBatch = 2000;

    Config application = mock(Config.class);
    when(application.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someTimeout));
    when(application.getProperty(eq(BATCH_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someBatch));
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, application);

    Config fxApollo = mock(Config.class);
    when(fxApollo.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(anotherTimeout));
    mockConfig(FX_APOLLO_NAMESPACE, fxApollo);

    check(anotherTimeout, someBatch, AppConfig2.class, AppConfig4.class);
  }

  @Test
  public void testApplicationPropertySourceWithValueInjectedAsParameter() throws Exception {
    int someTimeout = 1000;
    int someBatch = 2000;

    Config config = mock(Config.class);
    when(config.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someTimeout));
    when(config.getProperty(eq(BATCH_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someBatch));

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(AppConfig5.class);

    TestJavaConfigBean2 bean = context.getBean(TestJavaConfigBean2.class);

    assertEquals(someTimeout, bean.getTimeout());
    assertEquals(someBatch, bean.getBatch());
  }

  @Test
  public void testApplicationPropertySourceWithValueInjectedAsConstructorArgs() throws Exception {
    int someTimeout = 1000;
    int someBatch = 2000;

    Config config = mock(Config.class);
    when(config.getProperty(eq(TIMEOUT_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someTimeout));
    when(config.getProperty(eq(BATCH_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someBatch));

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(AppConfig7.class);

    TestJavaConfigBean3 bean = context.getBean(TestJavaConfigBean3.class);

    assertEquals(someTimeout, bean.getTimeout());
    assertEquals(someBatch, bean.getBatch());
  }

  @Test
  public void testNestedProperty() throws Exception {
    String a = "a";
    String b = "b";
    int someValue = 1234;

    Config config = mock(Config.class);
    when(config.getProperty(eq(a), Mockito.nullable(String.class))).thenReturn(a);
    when(config.getProperty(eq(b), Mockito.nullable(String.class))).thenReturn(b);
    when(config.getProperty(eq(String.format("%s.%s", a, b)), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someValue));

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(NestedPropertyConfig1.class);

    TestNestedPropertyBean bean = context.getBean(TestNestedPropertyBean.class);

    assertEquals(someValue, bean.getNestedProperty());
  }

  @Test
  public void testNestedPropertyWithDefaultValue() throws Exception {
    String a = "a";
    String b = "b";
    String c = "c";
    int someValue = 1234;

    Config config = mock(Config.class);
    when(config.getProperty(eq(a), Mockito.nullable(String.class))).thenReturn(a);
    when(config.getProperty(eq(b), Mockito.nullable(String.class))).thenReturn(b);
    when(config.getProperty(eq(c), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someValue));

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(NestedPropertyConfig1.class);

    TestNestedPropertyBean bean = context.getBean(TestNestedPropertyBean.class);

    assertEquals(someValue, bean.getNestedProperty());
  }

  @Test
  public void testNestedPropertyWithNestedDefaultValue() throws Exception {
    String a = "a";
    String b = "b";

    Config config = mock(Config.class);
    when(config.getProperty(eq(a), Mockito.nullable(String.class))).thenReturn(a);
    when(config.getProperty(eq(b), Mockito.nullable(String.class))).thenReturn(b);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(NestedPropertyConfig1.class);

    TestNestedPropertyBean bean = context.getBean(TestNestedPropertyBean.class);

    assertEquals(100, bean.getNestedProperty());
  }

  @Test
  public void testMultipleNestedProperty() throws Exception {
    String a = "a";
    String b = "b";
    String nestedKey = "c.d";
    String nestedProperty = String.format("${%s}", nestedKey);
    int someValue = 1234;

    Config config = mock(Config.class);
    when(config.getProperty(eq(a), Mockito.nullable(String.class))).thenReturn(a);
    when(config.getProperty(eq(b), Mockito.nullable(String.class))).thenReturn(b);
    when(config.getProperty(eq(String.format("%s.%s", a, b)), Mockito.nullable(String.class)))
        .thenReturn(nestedProperty);
    when(config.getProperty(eq(nestedKey), Mockito.nullable(String.class)))
        .thenReturn(String.valueOf(someValue));

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(NestedPropertyConfig1.class);

    TestNestedPropertyBean bean = context.getBean(TestNestedPropertyBean.class);

    assertEquals(someValue, bean.getNestedProperty());
  }

  @Test
  public void testMultipleNestedPropertyWithDefaultValue() throws Exception {
    String a = "a";
    String b = "b";
    String nestedKey = "c.d";
    int someValue = 1234;
    String nestedProperty = String.format("${%s:%d}", nestedKey, someValue);

    Config config = mock(Config.class);
    when(config.getProperty(eq(a), Mockito.nullable(String.class))).thenReturn(a);
    when(config.getProperty(eq(b), Mockito.nullable(String.class))).thenReturn(b);
    when(config.getProperty(eq(String.format("%s.%s", a, b)), Mockito.nullable(String.class)))
        .thenReturn(nestedProperty);

    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(NestedPropertyConfig1.class);

    TestNestedPropertyBean bean = context.getBean(TestNestedPropertyBean.class);

    assertEquals(someValue, bean.getNestedProperty());
  }

  @Test
  public void testApolloJsonValue() {
    String someJson = "[{\"a\":\"astring\", \"b\":10},{\"a\":\"astring2\", \"b\":20}]";
    String otherJson = "[{\"a\":\"otherString\", \"b\":10},{\"a\":\"astring2\", \"b\":20}]";

    Config config = mock(Config.class);
    when(config.getProperty(eq(JSON_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(someJson);
    when(config.getProperty(eq(OTHER_JSON_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(otherJson);
    when(config.getProperty(eq("a"), Mockito.nullable(String.class))).thenReturn(JSON_PROPERTY);
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(AppConfig8.class);

    TestJsonPropertyBean testJsonPropertyBean = context.getBean(TestJsonPropertyBean.class);
    assertEquals(2, testJsonPropertyBean.getJsonBeanList().size());
    assertEquals("astring", testJsonPropertyBean.getJsonBeanList().get(0).getA());
    assertEquals(10, testJsonPropertyBean.getJsonBeanList().get(0).getB());
    assertEquals("astring2", testJsonPropertyBean.getJsonBeanList().get(1).getA());
    assertEquals(20, testJsonPropertyBean.getJsonBeanList().get(1).getB());
    assertEquals(
        testJsonPropertyBean.getJsonBeanList(), testJsonPropertyBean.getEmbeddedJsonBeanList());
    assertEquals("otherString", testJsonPropertyBean.getOtherJsonBeanList().get(0).getA());
    assertEquals(10, testJsonPropertyBean.getOtherJsonBeanList().get(0).getB());
    assertEquals("astring2", testJsonPropertyBean.getOtherJsonBeanList().get(1).getA());
    assertEquals(20, testJsonPropertyBean.getOtherJsonBeanList().get(1).getB());
  }

  @Test(expected = BeanCreationException.class)
  public void testApolloJsonValueWithInvalidJson() throws Exception {
    String someInvalidJson = "someInvalidJson";

    Config config = mock(Config.class);
    when(config.getProperty(eq(JSON_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(someInvalidJson);
    when(config.getProperty(eq(OTHER_JSON_PROPERTY), Mockito.nullable(String.class)))
        .thenReturn(someInvalidJson);
    when(config.getProperty(eq("a"), Mockito.nullable(String.class))).thenReturn(JSON_PROPERTY);
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    new AnnotationConfigApplicationContext(AppConfig8.class).getBean(TestJsonPropertyBean.class);
  }

  @Test(expected = BeanCreationException.class)
  public void testApolloJsonValueWithNoPropertyValue() throws Exception {
    Config config = mock(Config.class);
    mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);

    new AnnotationConfigApplicationContext(AppConfig8.class);
  }

  private void check(int expectedTimeout, int expectedBatch, Class<?>... annotatedClasses) {
    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(annotatedClasses);

    TestJavaConfigBean bean = context.getBean(TestJavaConfigBean.class);

    assertEquals(expectedTimeout, bean.getTimeout());
    assertEquals(expectedBatch, bean.getBatch());
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig1 {
    @Bean
    TestJavaConfigBean testJavaConfigBean() {
      return new TestJavaConfigBean();
    }
  }

  @Configuration
  @EnableApolloConfig("application")
  static class AppConfig2 {
    @Bean
    TestJavaConfigBean testJavaConfigBean() {
      return new TestJavaConfigBean();
    }
  }

  @Configuration
  @EnableApolloConfig({"application", "FX.apollo"})
  static class AppConfig3 {
    @Bean
    TestJavaConfigBean testJavaConfigBean() {
      return new TestJavaConfigBean();
    }
  }

  @Configuration
  @EnableApolloConfig(value = "FX.apollo", order = 10)
  static class AppConfig4 {}

  @Configuration
  @EnableApolloConfig
  static class AppConfig5 {
    @Bean
    TestJavaConfigBean2 testJavaConfigBean2(
        @Value("${timeout:100}") int timeout, @Value("${batch:200}") int batch) {
      TestJavaConfigBean2 bean = new TestJavaConfigBean2();

      bean.setTimeout(timeout);
      bean.setBatch(batch);

      return bean;
    }
  }

  @Configuration
  @EnableApolloConfig({"FX.apollo", "application"})
  static class AppConfig6 {
    @Bean
    TestJavaConfigBean testJavaConfigBean() {
      return new TestJavaConfigBean();
    }
  }

  @Configuration
  @ComponentScan(
      includeFilters = {
        @Filter(
            type = FilterType.ANNOTATION,
            value = {Component.class})
      },
      excludeFilters = {
        @Filter(
            type = FilterType.ANNOTATION,
            value = {Configuration.class})
      })
  @EnableApolloConfig
  static class AppConfig7 {}

  @Configuration
  @EnableApolloConfig
  static class NestedPropertyConfig1 {
    @Bean
    TestNestedPropertyBean testNestedPropertyBean() {
      return new TestNestedPropertyBean();
    }
  }

  @Configuration
  @EnableApolloConfig
  static class AppConfig8 {

    @Bean
    TestJsonPropertyBean testJavaConfigBean() {
      return new TestJsonPropertyBean();
    }
  }

  @Configuration
  @EnableApolloConfig("application.yaml")
  static class AppConfig9 {
    @Bean
    TestJavaConfigBean testJavaConfigBean() {
      return new TestJavaConfigBean();
    }
  }

  @Configuration
  @EnableApolloConfig("application.yaMl")
  static class AppConfig10 {
    @Bean
    TestJavaConfigBean testJavaConfigBean() {
      return new TestJavaConfigBean();
    }
  }

  @Configuration
  @EnableApolloConfig({"application.yml", "FX.apollo"})
  static class AppConfig11 {
    @Bean
    TestJavaConfigBean testJavaConfigBean() {
      return new TestJavaConfigBean();
    }
  }

  @Component
  static class TestJavaConfigBean {
    @Value("${timeout:100}")
    private int timeout;

    private int batch;

    @Value("${batch:200}")
    public void setBatch(int batch) {
      this.batch = batch;
    }

    public int getTimeout() {
      return timeout;
    }

    public int getBatch() {
      return batch;
    }
  }

  static class TestJavaConfigBean2 {
    private int timeout;
    private int batch;

    public int getTimeout() {
      return timeout;
    }

    public void setTimeout(int timeout) {
      this.timeout = timeout;
    }

    public int getBatch() {
      return batch;
    }

    public void setBatch(int batch) {
      this.batch = batch;
    }
  }

  @Component
  static class TestJavaConfigBean3 {
    private final int timeout;
    private final int batch;

    @Autowired
    public TestJavaConfigBean3(
        @Value("${timeout:100}") int timeout, @Value("${batch:200}") int batch) {
      this.timeout = timeout;
      this.batch = batch;
    }

    public int getTimeout() {
      return timeout;
    }

    public int getBatch() {
      return batch;
    }
  }

  static class TestNestedPropertyBean {

    @Value("${${a}.${b}:${c:100}}")
    private int nestedProperty;

    public int getNestedProperty() {
      return nestedProperty;
    }
  }

  static class TestJsonPropertyBean {

    @ApolloJsonValue("${jsonProperty}")
    private List<JsonBean> jsonBeanList;

    private List<JsonBean> otherJsonBeanList;

    @ApolloJsonValue("${${a}}")
    private List<JsonBean> embeddedJsonBeanList;

    public List<JsonBean> getJsonBeanList() {
      return jsonBeanList;
    }

    @ApolloJsonValue("${otherJsonProperty}")
    public void setOtherJsonBeanList(List<JsonBean> otherJsonBeanList) {
      this.otherJsonBeanList = otherJsonBeanList;
    }

    public List<JsonBean> getOtherJsonBeanList() {
      return otherJsonBeanList;
    }

    public List<JsonBean> getEmbeddedJsonBeanList() {
      return embeddedJsonBeanList;
    }
  }

  static class JsonBean {

    private String a;
    private int b;

    String getA() {
      return a;
    }

    public void setA(String a) {
      this.a = a;
    }

    int getB() {
      return b;
    }

    public void setB(int b) {
      this.b = b;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      JsonBean jsonBean = (JsonBean) o;

      if (b != jsonBean.b) {
        return false;
      }
      return a != null ? a.equals(jsonBean.a) : jsonBean.a == null;
    }

    @Override
    public int hashCode() {
      int result = a != null ? a.hashCode() : 0;
      result = 31 * result + b;
      return result;
    }
  }
}
