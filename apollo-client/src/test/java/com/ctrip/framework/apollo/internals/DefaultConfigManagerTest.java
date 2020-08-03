package com.ctrip.framework.apollo.internals;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import com.ctrip.framework.apollo.enums.ConfigSourceType;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.ctrip.framework.apollo.util.ConfigUtil;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigManagerTest {
  private DefaultConfigManager defaultConfigManager;
  private static String someConfigContent;

  @Before
  public void setUp() throws Exception {
    MockInjector.setInstance(ConfigFactoryManager.class, new MockConfigFactoryManager());
    MockInjector.setInstance(ConfigUtil.class, new ConfigUtil());
    defaultConfigManager = new DefaultConfigManager();
    someConfigContent = "someContent";
  }

  @After
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testGetConfig() throws Exception {
    String someNamespace = "someName";
    String anotherNamespace = "anotherName";
    String someKey = "someKey";
    Config config = defaultConfigManager.getConfig(someNamespace);
    Config anotherConfig = defaultConfigManager.getConfig(anotherNamespace);

    assertEquals(someNamespace + ":" + someKey, config.getProperty(someKey, null));
    assertEquals(anotherNamespace + ":" + someKey, anotherConfig.getProperty(someKey, null));
  }

  @Test
  public void testGetConfigMultipleTimesWithSameNamespace() throws Exception {
    String someNamespace = "someName";
    Config config = defaultConfigManager.getConfig(someNamespace);
    Config anotherConfig = defaultConfigManager.getConfig(someNamespace);

    assertThat(
        "Get config multiple times with the same namespace should return the same config instance",
        config, equalTo(anotherConfig));
  }

  @Test
  public void testGetConfigFile() throws Exception {
    String someNamespace = "someName";
    ConfigFileFormat someConfigFileFormat = ConfigFileFormat.Properties;

    ConfigFile configFile =
        defaultConfigManager.getConfigFile(someNamespace, someConfigFileFormat);

    assertEquals(someConfigFileFormat, configFile.getConfigFileFormat());
    assertEquals(someConfigContent, configFile.getContent());
  }

  @Test
  public void testGetConfigFileMultipleTimesWithSameNamespace() throws Exception {
    String someNamespace = "someName";
    ConfigFileFormat someConfigFileFormat = ConfigFileFormat.Properties;

    ConfigFile someConfigFile =
        defaultConfigManager.getConfigFile(someNamespace, someConfigFileFormat);
    ConfigFile anotherConfigFile =
        defaultConfigManager.getConfigFile(someNamespace, someConfigFileFormat);

    assertThat(
        "Get config file multiple times with the same namespace should return the same config file instance",
        someConfigFile, equalTo(anotherConfigFile));

  }

  public static class MockConfigFactoryManager implements ConfigFactoryManager {

    @Override
    public ConfigFactory getFactory(String namespace) {
      return new ConfigFactory() {
        @Override
        public Config create(final String namespace) {
          return new AbstractConfig() {
            @Override
            public String getProperty(String key, String defaultValue) {
              return namespace + ":" + key;
            }

            @Override
            public Set<String> getPropertyNames() {
              return null;
            }

            @Override
            public ConfigSourceType getSourceType() {
              return null;
            }
          };
        }

        @Override
        public ConfigFile createConfigFile(String namespace, final ConfigFileFormat configFileFormat) {
          ConfigRepository someConfigRepository = mock(ConfigRepository.class);
          return new AbstractConfigFile(namespace, someConfigRepository) {

            @Override
            protected void update(Properties newProperties) {

            }

            @Override
            public String getContent() {
              return someConfigContent;
            }

            @Override
            public boolean hasContent() {
              return true;
            }

            @Override
            public ConfigFileFormat getConfigFileFormat() {
              return configFileFormat;
            }
          };
        }
      };
    }
  }
}
