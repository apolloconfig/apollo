package com.ctrip.framework.apollo.spi;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigFactoryManagerTest {
  private DefaultConfigFactoryManager defaultConfigFactoryManager;

  @Before
  public void setUp() throws Exception {
    MockInjector.setInstance(ConfigRegistry.class, new MockConfigRegistry());
    defaultConfigFactoryManager = new DefaultConfigFactoryManager();
  }

  @After
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testGetFactoryFromRegistry() throws Exception {
    ConfigFactory result =
        defaultConfigFactoryManager.getFactory(MockConfigRegistry.NAMESPACE_REGISTERED);

    assertEquals(MockConfigRegistry.REGISTERED_CONFIGFACTORY, result);
  }

  @Test
  public void testGetFactoryFromNamespace() throws Exception {
    String someNamespace = "someName";
    MockInjector.setInstance(ConfigFactory.class, someNamespace, new SomeConfigFactory());

    ConfigFactory result = defaultConfigFactoryManager.getFactory(someNamespace);

    assertThat("When namespace is registered, should return the registerd config factory", result,
        instanceOf(SomeConfigFactory.class));
  }

  @Test
  public void testGetFactoryFromNamespaceMultipleTimes() throws Exception {
    String someNamespace = "someName";
    MockInjector.setInstance(ConfigFactory.class, someNamespace, new SomeConfigFactory());

    ConfigFactory result = defaultConfigFactoryManager.getFactory(someNamespace);
    ConfigFactory anotherResult = defaultConfigFactoryManager.getFactory(someNamespace);

    assertThat(
        "Get config factory with the same namespace multiple times should returnt the same instance",
        anotherResult, equalTo(result));
  }

  @Test
  public void testGetFactoryFromDefault() throws Exception {
    String someNamespace = "someName";
    MockInjector.setInstance(ConfigFactory.class, new AnotherConfigFactory());

    ConfigFactory result = defaultConfigFactoryManager.getFactory(someNamespace);

    assertThat("When namespace is not registered, should return the default config factory", result,
        instanceOf(AnotherConfigFactory.class));
  }

  public static class MockConfigRegistry implements ConfigRegistry {
    public static String NAMESPACE_REGISTERED = "some-namespace-registered";
    public static ConfigFactory REGISTERED_CONFIGFACTORY = new ConfigFactory() {
      @Override
      public Config create(String namespace) {
        return null;
      }

      @Override
      public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
        return null;
      }

    };

    @Override
    public void register(String namespace, ConfigFactory factory) {
      //do nothing
    }

    @Override
    public ConfigFactory getFactory(String namespace) {
      if (namespace.equals(NAMESPACE_REGISTERED)) {
        return REGISTERED_CONFIGFACTORY;
      }
      return null;
    }
  }

  public static class SomeConfigFactory implements ConfigFactory {
    @Override
    public Config create(String namespace) {
      return null;
    }

    @Override
    public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
      return null;
    }
  }

  public static class AnotherConfigFactory implements ConfigFactory {
    @Override
    public Config create(String namespace) {
      return null;
    }

    @Override
    public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
      return null;
    }
  }

}
