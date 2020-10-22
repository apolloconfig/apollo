package com.ctrip.framework.apollo.spring;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.boot.ApolloApplicationContextInitializer;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.google.common.collect.Sets;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(Enclosed.class)
public class BootstrapConfigTest {

  private static final String TEST_BEAN_CONDITIONAL_ON_KEY = "apollo.test.testBean";
  private static final String FX_APOLLO_NAMESPACE = "FX.apollo";

  @RunWith(SpringJUnit4ClassRunner.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  public static class TestWithBootstrapEnabledAndDefaultNamespacesAndConditionalOn extends
      AbstractSpringIntegrationTest {
    private static final String someProperty = "someProperty";
    private static final String someValue = "someValue";

    @Autowired(required = false)
    private TestBean testBean;

    @ApolloConfig
    private Config config;

    @Value("${" + someProperty + "}")
    private String someInjectedValue;

    private static Config mockedConfig;


    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");

      mockedConfig = mock(Config.class);

      when(mockedConfig.getPropertyNames()).thenReturn(Sets.newHashSet(TEST_BEAN_CONDITIONAL_ON_KEY, someProperty));

      when(mockedConfig.getProperty(eq(TEST_BEAN_CONDITIONAL_ON_KEY), anyString())).thenReturn(Boolean.TRUE.toString());
      when(mockedConfig.getProperty(eq(someProperty), anyString())).thenReturn(someValue);

      mockConfig(ConfigConsts.NAMESPACE_APPLICATION, mockedConfig);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNotNull(testBean);
      Assert.assertTrue(testBean.execute());

      Assert.assertEquals(mockedConfig, config);

      Assert.assertEquals(someValue, someInjectedValue);
    }
  }

  @RunWith(SpringJUnit4ClassRunner.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  public static class TestWithBootstrapEnabledAndNamespacesAndConditionalOn extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES,
          String.format("%s, %s", ConfigConsts.NAMESPACE_APPLICATION, FX_APOLLO_NAMESPACE));

      Config config = mock(Config.class);
      Config anotherConfig = mock(Config.class);

      when(config.getPropertyNames()).thenReturn(Sets.newHashSet(TEST_BEAN_CONDITIONAL_ON_KEY));
      when(config.getProperty(eq(TEST_BEAN_CONDITIONAL_ON_KEY), anyString())).thenReturn(Boolean.TRUE.toString());

      mockConfig(ConfigConsts.NAMESPACE_APPLICATION, anotherConfig);
      mockConfig(FX_APOLLO_NAMESPACE, config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNotNull(testBean);
      Assert.assertTrue(testBean.execute());
    }
  }

  @RunWith(SpringJUnit4ClassRunner.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  public static class TestWithBootstrapEnabledAndNamespacesAndConditionalOnWithYamlFile extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES,
          String.format("%s, %s", "application.yml", FX_APOLLO_NAMESPACE));

      prepareYamlConfigFile("application.yml", readYamlContentAsConfigFileProperties("case6.yml"));
      Config anotherConfig = mock(Config.class);

      mockConfig(ConfigConsts.NAMESPACE_APPLICATION, anotherConfig);
      mockConfig(FX_APOLLO_NAMESPACE, anotherConfig);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNotNull(testBean);
      Assert.assertTrue(testBean.execute());
    }
  }

  @RunWith(SpringJUnit4ClassRunner.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  public static class TestWithBootstrapEnabledAndDefaultNamespacesAndConditionalOnFailed extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");

      Config config = mock(Config.class);

      when(config.getPropertyNames()).thenReturn(Sets.newHashSet(TEST_BEAN_CONDITIONAL_ON_KEY));
      when(config.getProperty(eq(TEST_BEAN_CONDITIONAL_ON_KEY), anyString())).thenReturn(Boolean.FALSE.toString());

      mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNull(testBean);
    }
  }

  @RunWith(SpringJUnit4ClassRunner.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  public static class TestWithBootstrapEnabledAndDefaultNamespacesAndConditionalOnFailedWithYamlFile extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, "application.yml");

      prepareYamlConfigFile("application.yml", readYamlContentAsConfigFileProperties("case7.yml"));
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNull(testBean);
    }
  }

  @RunWith(SpringJUnit4ClassRunner.class)
  @SpringBootTest(classes = ConfigurationWithoutConditionalOnProperty.class)
  @DirtiesContext
  public static class TestWithBootstrapEnabledAndDefaultNamespacesAndConditionalOff extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");

      Config config = mock(Config.class);

      mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNotNull(testBean);
      Assert.assertTrue(testBean.execute());
    }
  }

  @RunWith(SpringJUnit4ClassRunner.class)
  @SpringBootTest(classes = ConfigurationWithoutConditionalOnProperty.class)
  @DirtiesContext
  public static class TestWithBootstrapEnabledAndDefaultNamespacesAndConditionalOffWithYamlFile extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, "application.yml");

      prepareYamlConfigFile("application.yml", readYamlContentAsConfigFileProperties("case8.yml"));
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNotNull(testBean);
      Assert.assertTrue(testBean.execute());
    }
  }

  @RunWith(SpringJUnit4ClassRunner.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  public static class TestWithBootstrapDisabledAndDefaultNamespacesAndConditionalOn extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      Config config = mock(Config.class);

      when(config.getPropertyNames()).thenReturn(Sets.newHashSet(TEST_BEAN_CONDITIONAL_ON_KEY));
      when(config.getProperty(eq(TEST_BEAN_CONDITIONAL_ON_KEY), anyString())).thenReturn(Boolean.FALSE.toString());

      mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNull(testBean);
    }
  }

  @RunWith(SpringJUnit4ClassRunner.class)
  @SpringBootTest(classes = ConfigurationWithoutConditionalOnProperty.class)
  @DirtiesContext
  public static class TestWithBootstrapDisabledAndDefaultNamespacesAndConditionalOff extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      Config config = mock(Config.class);

      mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNotNull(testBean);
      Assert.assertTrue(testBean.execute());
    }
  }

  @RunWith(SpringJUnit4ClassRunner.class)
  @SpringBootTest(classes = ConfigurationWithoutConditionalOnProperty.class)
  @DirtiesContext
  public static class TestWithBootstrapEnabledAndEagerLoadEnabled extends
          AbstractSpringIntegrationTest {

    @BeforeClass
    public static void beforeClass() {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED, "true");

      Config config = mock(Config.class);

      mockConfig(ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    @AfterClass
    public static void afterClass() {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED);

      doTearDown();
    }

    @Test
    public void test() {
      List<EnvironmentPostProcessor> processorList =  SpringFactoriesLoader.loadFactories(EnvironmentPostProcessor.class, getClass().getClassLoader());
      boolean containsApollo = false;
      for (EnvironmentPostProcessor postProcessor : processorList) {
        if (postProcessor instanceof ApolloApplicationContextInitializer) {
          containsApollo = true;
          break;
        }
      }
      Assert.assertTrue(containsApollo);
    }
  }

  @EnableAutoConfiguration
  @Configuration
  static class ConfigurationWithoutConditionalOnProperty {

    @Bean
    public TestBean testBean() {
      return new TestBean();
    }
  }

  @ConditionalOnProperty(TEST_BEAN_CONDITIONAL_ON_KEY)
  @EnableAutoConfiguration
  @Configuration
  static class ConfigurationWithConditionalOnProperty {

    @Bean
    public TestBean testBean() {
      return new TestBean();
    }
  }

  static class TestBean {

    public boolean execute() {
      return true;
    }
  }
}
