package com.ctrip.framework.apollo.internals;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.util.OrderedProperties;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import com.ctrip.framework.apollo.util.yaml.YamlParser;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class YamlConfigFileTest {

  private String someNamespace;
  @Mock
  private ConfigRepository configRepository;
  @Mock
  private YamlParser yamlParser;
  @Mock
  private PropertiesFactory propertiesFactory;

  private ConfigSourceType someSourceType;

  @Before
  public void setUp() throws Exception {
    someNamespace = "someName";

    MockInjector.setInstance(YamlParser.class, yamlParser);

    when(propertiesFactory.getPropertiesInstance()).thenAnswer(new Answer<Properties>() {
      @Override
      public Properties answer(InvocationOnMock invocation) {
        return new Properties();
      }
    });
    MockInjector.setInstance(PropertiesFactory.class, propertiesFactory);
  }

  @After
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testWhenHasContent() throws Exception {
    Properties someProperties = new Properties();
    String key = ConfigConsts.CONFIG_FILE_CONTENT_KEY;
    String someContent = "someKey: 'someValue'";
    someProperties.setProperty(key, someContent);
    someSourceType = ConfigSourceType.LOCAL;

    Properties yamlProperties = new Properties();
    yamlProperties.setProperty("someKey", "someValue");

    when(configRepository.getConfig()).thenReturn(someProperties);
    when(configRepository.getSourceType()).thenReturn(someSourceType);
    when(yamlParser.yamlToProperties(someContent)).thenReturn(yamlProperties);

    YamlConfigFile configFile = new YamlConfigFile(someNamespace, configRepository);

    assertSame(someContent, configFile.getContent());
    assertSame(yamlProperties, configFile.asProperties());
  }

  @Test
  public void testWhenHasContentWithOrder() throws Exception {
    when(propertiesFactory.getPropertiesInstance()).thenAnswer(new Answer<Properties>() {
      @Override
      public Properties answer(InvocationOnMock invocation) {
        return new OrderedProperties();
      }
    });
    Properties someProperties = new Properties();
    String key = ConfigConsts.CONFIG_FILE_CONTENT_KEY;
    String someContent = "someKey: 'someValue'\nsomeKey2: 'someValue2'";
    someProperties.setProperty(key, someContent);
    someSourceType = ConfigSourceType.LOCAL;

    Properties yamlProperties = new YamlParser().yamlToProperties(someContent);

    when(configRepository.getConfig()).thenReturn(someProperties);
    when(configRepository.getSourceType()).thenReturn(someSourceType);
    when(yamlParser.yamlToProperties(someContent)).thenReturn(yamlProperties);

    YamlConfigFile configFile = new YamlConfigFile(someNamespace, configRepository);

    assertSame(someContent, configFile.getContent());
    assertSame(yamlProperties, configFile.asProperties());

    String[] actualArrays = configFile.asProperties().keySet().toArray(new String[]{});
    String[] expectedArrays = {"someKey", "someKey2"};
    assertArrayEquals(expectedArrays, actualArrays);
  }

  @Test
  public void testWhenHasNoContent() throws Exception {
    when(configRepository.getConfig()).thenReturn(null);

    YamlConfigFile configFile = new YamlConfigFile(someNamespace, configRepository);

    assertFalse(configFile.hasContent());
    assertNull(configFile.getContent());

    Properties properties = configFile.asProperties();

    assertTrue(properties.isEmpty());
  }

  @Test
  public void testWhenInvalidYamlContent() throws Exception {
    Properties someProperties = new Properties();
    String key = ConfigConsts.CONFIG_FILE_CONTENT_KEY;
    String someInvalidContent = ",";
    someProperties.setProperty(key, someInvalidContent);
    someSourceType = ConfigSourceType.LOCAL;

    when(configRepository.getConfig()).thenReturn(someProperties);
    when(configRepository.getSourceType()).thenReturn(someSourceType);
    when(yamlParser.yamlToProperties(someInvalidContent))
        .thenThrow(new RuntimeException("some exception"));

    YamlConfigFile configFile = new YamlConfigFile(someNamespace, configRepository);

    assertSame(someInvalidContent, configFile.getContent());

    Throwable exceptionThrown = null;
    try {
      configFile.asProperties();
    } catch (Throwable ex) {
      exceptionThrown = ex;
    }

    assertTrue(exceptionThrown instanceof ApolloConfigException);
    assertNotNull(exceptionThrown.getCause());
  }

  @Test
  public void testWhenConfigRepositoryHasError() throws Exception {
    when(configRepository.getConfig()).thenThrow(new RuntimeException("someError"));

    YamlConfigFile configFile = new YamlConfigFile(someNamespace, configRepository);

    assertFalse(configFile.hasContent());
    assertNull(configFile.getContent());
    assertEquals(ConfigSourceType.NONE, configFile.getSourceType());

    Properties properties = configFile.asProperties();

    assertTrue(properties.isEmpty());
  }

  @Test
  public void testOnRepositoryChange() throws Exception {
    Properties someProperties = new Properties();
    String key = ConfigConsts.CONFIG_FILE_CONTENT_KEY;
    String someValue = "someKey: 'someValue'";
    String anotherValue = "anotherKey: 'anotherValue'";
    someProperties.setProperty(key, someValue);

    someSourceType = ConfigSourceType.LOCAL;

    Properties someYamlProperties = new Properties();
    someYamlProperties.setProperty("someKey", "someValue");

    Properties anotherYamlProperties = new Properties();
    anotherYamlProperties.setProperty("anotherKey", "anotherValue");

    when(configRepository.getConfig()).thenReturn(someProperties);
    when(configRepository.getSourceType()).thenReturn(someSourceType);
    when(yamlParser.yamlToProperties(someValue)).thenReturn(someYamlProperties);
    when(yamlParser.yamlToProperties(anotherValue)).thenReturn(anotherYamlProperties);

    YamlConfigFile configFile = new YamlConfigFile(someNamespace, configRepository);

    assertEquals(someValue, configFile.getContent());
    assertEquals(someSourceType, configFile.getSourceType());
    assertSame(someYamlProperties, configFile.asProperties());

    Properties anotherProperties = new Properties();
    anotherProperties.setProperty(key, anotherValue);

    ConfigSourceType anotherSourceType = ConfigSourceType.REMOTE;
    when(configRepository.getSourceType()).thenReturn(anotherSourceType);

    configFile.onRepositoryChange(someNamespace, anotherProperties);

    assertEquals(anotherValue, configFile.getContent());
    assertEquals(anotherSourceType, configFile.getSourceType());
    assertSame(anotherYamlProperties, configFile.asProperties());
  }

  @Test
  public void testWhenConfigRepositoryHasErrorAndThenRecovered() throws Exception {
    Properties someProperties = new Properties();
    String key = ConfigConsts.CONFIG_FILE_CONTENT_KEY;
    String someValue = "someKey: 'someValue'";
    someProperties.setProperty(key, someValue);

    someSourceType = ConfigSourceType.LOCAL;

    Properties someYamlProperties = new Properties();
    someYamlProperties.setProperty("someKey", "someValue");

    when(configRepository.getConfig()).thenThrow(new RuntimeException("someError"));
    when(configRepository.getSourceType()).thenReturn(someSourceType);
    when(yamlParser.yamlToProperties(someValue)).thenReturn(someYamlProperties);

    YamlConfigFile configFile = new YamlConfigFile(someNamespace, configRepository);

    assertFalse(configFile.hasContent());
    assertNull(configFile.getContent());
    assertEquals(ConfigSourceType.NONE, configFile.getSourceType());
    assertTrue(configFile.asProperties().isEmpty());

    configFile.onRepositoryChange(someNamespace, someProperties);

    assertTrue(configFile.hasContent());
    assertEquals(someValue, configFile.getContent());
    assertEquals(someSourceType, configFile.getSourceType());
    assertSame(someYamlProperties, configFile.asProperties());
  }
}
