package com.ctrip.framework.apollo.internals;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Created by Jason on 4/9/16.
 */
public class LocalFileConfigRepositoryTest {
  private File someBaseDir;
  private String someNamespace;
  private ConfigRepository upstreamRepo;
  private Properties someProperties;
  private static String someAppId = "someApp";
  private static String someCluster = "someCluster";
  private String defaultKey;
  private String defaultValue;
  private ConfigSourceType someSourceType;

  @Before
  public void setUp() throws Exception {
    someBaseDir = new File("src/test/resources/config-cache");
    someBaseDir.mkdir();

    someNamespace = "someName";
    someProperties = new Properties();
    defaultKey = "defaultKey";
    defaultValue = "defaultValue";
    someProperties.setProperty(defaultKey, defaultValue);
    someSourceType = ConfigSourceType.REMOTE;
    upstreamRepo = mock(ConfigRepository.class);
    when(upstreamRepo.getConfig()).thenReturn(someProperties);
    when(upstreamRepo.getSourceType()).thenReturn(someSourceType);

    MockInjector.setInstance(ConfigUtil.class, new MockConfigUtil());
    PropertiesFactory propertiesFactory = mock(PropertiesFactory.class);
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
    recursiveDelete(someBaseDir);
  }

  //helper method to clean created files
  private void recursiveDelete(File file) {
    if (!file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        recursiveDelete(f);
      }
    }
    file.delete();
  }

  private String assembleLocalCacheFileName() {
    return String.format("%s.properties", Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
        .join(someAppId, someCluster, someNamespace));
  }


  @Test
  public void testLoadConfigWithLocalFile() throws Exception {

    String someKey = "someKey";
    String someValue = "someValue\nxxx\nyyy";

    Properties someProperties = new Properties();
    someProperties.setProperty(someKey, someValue);
    createLocalCachePropertyFile(someProperties);

    LocalFileConfigRepository localRepo = new LocalFileConfigRepository(someNamespace);
    localRepo.setLocalCacheDir(someBaseDir, true);
    Properties properties = localRepo.getConfig();

    assertEquals(someValue, properties.getProperty(someKey));
    assertEquals(ConfigSourceType.LOCAL, localRepo.getSourceType());
  }

  @Test
  public void testLoadConfigWithLocalFileAndFallbackRepo() throws Exception {
    File file = new File(someBaseDir, assembleLocalCacheFileName());

    String someValue = "someValue";

    Files.write(defaultKey + "=" + someValue, file, Charsets.UTF_8);

    LocalFileConfigRepository localRepo = new LocalFileConfigRepository(someNamespace, upstreamRepo);
    localRepo.setLocalCacheDir(someBaseDir, true);

    Properties properties = localRepo.getConfig();

    assertEquals(defaultValue, properties.getProperty(defaultKey));
    assertEquals(someSourceType, localRepo.getSourceType());
  }

  @Test
  public void testLoadConfigWithNoLocalFile() throws Exception {
    LocalFileConfigRepository localFileConfigRepository =
        new LocalFileConfigRepository(someNamespace, upstreamRepo);
    localFileConfigRepository.setLocalCacheDir(someBaseDir, true);

    Properties result = localFileConfigRepository.getConfig();

    assertThat(
        "LocalFileConfigRepository's properties should be the same as fallback repo's when there is no local cache",
        result.entrySet(), equalTo(someProperties.entrySet()));
    assertEquals(someSourceType, localFileConfigRepository.getSourceType());
  }

  @Test
  public void testLoadConfigWithNoLocalFileMultipleTimes() throws Exception {
    LocalFileConfigRepository localRepo =
        new LocalFileConfigRepository(someNamespace, upstreamRepo);
    localRepo.setLocalCacheDir(someBaseDir, true);

    Properties someProperties = localRepo.getConfig();

    LocalFileConfigRepository
        anotherLocalRepoWithNoFallback =
        new LocalFileConfigRepository(someNamespace);
    anotherLocalRepoWithNoFallback.setLocalCacheDir(someBaseDir, true);

    Properties anotherProperties = anotherLocalRepoWithNoFallback.getConfig();

    assertThat(
        "LocalFileConfigRepository should persist local cache files and return that afterwards",
        someProperties.entrySet(), equalTo(anotherProperties.entrySet()));
    assertEquals(someSourceType, localRepo.getSourceType());
  }

  @Test
  public void testOnRepositoryChange() throws Exception {
    RepositoryChangeListener someListener = mock(RepositoryChangeListener.class);

    LocalFileConfigRepository localFileConfigRepository =
        new LocalFileConfigRepository(someNamespace, upstreamRepo);

    assertEquals(someSourceType, localFileConfigRepository.getSourceType());

    localFileConfigRepository.setLocalCacheDir(someBaseDir, true);
    localFileConfigRepository.addChangeListener(someListener);

    localFileConfigRepository.getConfig();

    Properties anotherProperties = new Properties();
    anotherProperties.put("anotherKey", "anotherValue");

    ConfigSourceType anotherSourceType = ConfigSourceType.NONE;
    when(upstreamRepo.getSourceType()).thenReturn(anotherSourceType);

    localFileConfigRepository.onRepositoryChange(someNamespace, anotherProperties);

    final ArgumentCaptor<Properties> captor = ArgumentCaptor.forClass(Properties.class);

    verify(someListener, times(1)).onRepositoryChange(eq(someNamespace), captor.capture());

    assertEquals(anotherProperties, captor.getValue());
    assertEquals(anotherSourceType, localFileConfigRepository.getSourceType());
  }

  public static class MockConfigUtil extends ConfigUtil {
    @Override
    public String getAppId() {
      return someAppId;
    }

    @Override
    public String getCluster() {
      return someCluster;
    }
  }

  private File createLocalCachePropertyFile(Properties properties) throws IOException {
    File file = new File(someBaseDir, assembleLocalCacheFileName());
    FileOutputStream in = null;
    try {
      in = new FileOutputStream(file);
      properties.store(in, "Persisted by LocalFileConfigRepositoryTest");
    } finally {
      if (in != null) {
        in.close();
      }
    }
    return file;
  }
}
