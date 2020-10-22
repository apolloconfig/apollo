package com.ctrip.framework.apollo.internals;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;
import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.core.signature.Signature;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.exceptions.ApolloConfigStatusCodeException;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.OrderedProperties;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import com.ctrip.framework.apollo.util.http.HttpRequest;
import com.ctrip.framework.apollo.util.http.HttpResponse;
import com.ctrip.framework.apollo.util.http.HttpUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;
import com.google.common.net.UrlEscapers;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * Created by Jason on 4/9/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class RemoteConfigRepositoryTest {

  @Mock
  private ConfigServiceLocator configServiceLocator;
  private String someNamespace;
  private String someServerUrl;
  private ConfigUtil configUtil;
  private HttpUtil httpUtil;
  @Mock
  private static HttpResponse<ApolloConfig> someResponse;
  @Mock
  private static HttpResponse<List<ApolloConfigNotification>> pollResponse;
  private RemoteConfigLongPollService remoteConfigLongPollService;
  @Mock
  private PropertiesFactory propertiesFactory;

  private static String someAppId;
  private static String someCluster;
  private static String someSecret;

  @Before
  public void setUp() throws Exception {
    someNamespace = "someName";

    when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_NOT_MODIFIED);

    configUtil = new MockConfigUtil();
    MockInjector.setInstance(ConfigUtil.class, configUtil);

    someServerUrl = "http://someServer";

    ServiceDTO serviceDTO = mock(ServiceDTO.class);

    when(serviceDTO.getHomepageUrl()).thenReturn(someServerUrl);
    when(configServiceLocator.getConfigServices()).thenReturn(Lists.newArrayList(serviceDTO));
    MockInjector.setInstance(ConfigServiceLocator.class, configServiceLocator);

    httpUtil = spy(new MockHttpUtil());
    MockInjector.setInstance(HttpUtil.class, httpUtil);

    remoteConfigLongPollService = new RemoteConfigLongPollService();

    MockInjector.setInstance(RemoteConfigLongPollService.class, remoteConfigLongPollService);

    when(propertiesFactory.getPropertiesInstance()).thenAnswer(new Answer<Properties>() {
      @Override
      public Properties answer(InvocationOnMock invocation) {
        return new Properties();
      }
    });
    MockInjector.setInstance(PropertiesFactory.class, propertiesFactory);

    someAppId = "someAppId";
    someCluster = "someCluster";
  }

  @After
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testLoadConfig() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    Map<String, String> configurations = Maps.newHashMap();
    configurations.put(someKey, someValue);
    ApolloConfig someApolloConfig = assembleApolloConfig(configurations);

    when(someResponse.getStatusCode()).thenReturn(200);
    when(someResponse.getBody()).thenReturn(someApolloConfig);

    RemoteConfigRepository remoteConfigRepository = new RemoteConfigRepository(someNamespace);

    Properties config = remoteConfigRepository.getConfig();

    assertEquals(configurations, config);
    assertEquals(ConfigSourceType.REMOTE, remoteConfigRepository.getSourceType());
    remoteConfigLongPollService.stopLongPollingRefresh();
  }

  @Test
  public void testLoadConfigWithOrderedProperties() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    Map<String, String> configurations = Maps.newLinkedHashMap();
    configurations.put(someKey, someValue);
    configurations.put("someKey2", "someValue2");
    ApolloConfig someApolloConfig = assembleApolloConfig(configurations);

    when(someResponse.getStatusCode()).thenReturn(200);
    when(someResponse.getBody()).thenReturn(someApolloConfig);
    when(propertiesFactory.getPropertiesInstance()).thenAnswer(new Answer<Properties>() {
      @Override
      public Properties answer(InvocationOnMock invocation) {
        return new OrderedProperties();
      }
    });

    RemoteConfigRepository remoteConfigRepository = new RemoteConfigRepository(someNamespace);

    Properties config = remoteConfigRepository.getConfig();

    assertTrue(config instanceof OrderedProperties);
    assertEquals(configurations, config);
    assertEquals(ConfigSourceType.REMOTE, remoteConfigRepository.getSourceType());
    remoteConfigLongPollService.stopLongPollingRefresh();

    String[] actualArrays = config.keySet().toArray(new String[]{});
    String[] expectedArrays = {"someKey", "someKey2"};
    assertArrayEquals(expectedArrays, actualArrays);
  }

  @Test
  public void testLoadConfigWithAccessKeySecret() throws Exception {
    someSecret = "someSecret";
    String someKey = "someKey";
    String someValue = "someValue";
    Map<String, String> configurations = Maps.newHashMap();
    configurations.put(someKey, someValue);
    ApolloConfig someApolloConfig = assembleApolloConfig(configurations);

    when(someResponse.getStatusCode()).thenReturn(200);
    when(someResponse.getBody()).thenReturn(someApolloConfig);
    doAnswer(new Answer<HttpResponse<ApolloConfig>>() {
      @Override
      public HttpResponse<ApolloConfig> answer(InvocationOnMock invocation) throws Throwable {
        HttpRequest request = invocation.getArgumentAt(0, HttpRequest.class);
        Map<String, String> headers = request.getHeaders();
        assertNotNull(headers);
        assertTrue(headers.containsKey(Signature.HTTP_HEADER_TIMESTAMP));
        assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));

        return someResponse;
      }
    }).when(httpUtil).doGet(any(HttpRequest.class), any(Class.class));

    RemoteConfigRepository remoteConfigRepository = new RemoteConfigRepository(someNamespace);

    Properties config = remoteConfigRepository.getConfig();

    assertEquals(configurations, config);
    assertEquals(ConfigSourceType.REMOTE, remoteConfigRepository.getSourceType());
    remoteConfigLongPollService.stopLongPollingRefresh();
  }

  @Test(expected = ApolloConfigException.class)
  public void testGetRemoteConfigWithServerError() throws Exception {

    when(someResponse.getStatusCode()).thenReturn(500);

    RemoteConfigRepository remoteConfigRepository = new RemoteConfigRepository(someNamespace);

    //must stop the long polling before exception occurred
    remoteConfigLongPollService.stopLongPollingRefresh();

    remoteConfigRepository.getConfig();
  }

  @Test(expected = ApolloConfigException.class)
  public void testGetRemoteConfigWithNotFount() throws Exception {

    when(someResponse.getStatusCode()).thenReturn(404);

    RemoteConfigRepository remoteConfigRepository = new RemoteConfigRepository(someNamespace);

    //must stop the long polling before exception occurred
    remoteConfigLongPollService.stopLongPollingRefresh();

    remoteConfigRepository.getConfig();
  }

  @Test
  public void testRepositoryChangeListener() throws Exception {
    Map<String, String> configurations = ImmutableMap.of("someKey", "someValue");
    ApolloConfig someApolloConfig = assembleApolloConfig(configurations);

    when(someResponse.getStatusCode()).thenReturn(200);
    when(someResponse.getBody()).thenReturn(someApolloConfig);

    RepositoryChangeListener someListener = mock(RepositoryChangeListener.class);
    RemoteConfigRepository remoteConfigRepository = new RemoteConfigRepository(someNamespace);
    remoteConfigRepository.addChangeListener(someListener);
    final ArgumentCaptor<Properties> captor = ArgumentCaptor.forClass(Properties.class);

    Map<String, String> newConfigurations = ImmutableMap.of("someKey", "anotherValue");
    ApolloConfig newApolloConfig = assembleApolloConfig(newConfigurations);

    when(someResponse.getBody()).thenReturn(newApolloConfig);

    remoteConfigRepository.sync();

    verify(someListener, times(1)).onRepositoryChange(eq(someNamespace), captor.capture());

    assertEquals(newConfigurations, captor.getValue());

    remoteConfigLongPollService.stopLongPollingRefresh();
  }

  @Test
  public void testLongPollingRefresh() throws Exception {
    Map<String, String> configurations = ImmutableMap.of("someKey", "someValue");
    ApolloConfig someApolloConfig = assembleApolloConfig(configurations);

    when(someResponse.getStatusCode()).thenReturn(200);
    when(someResponse.getBody()).thenReturn(someApolloConfig);

    final SettableFuture<Boolean> longPollFinished = SettableFuture.create();
    RepositoryChangeListener someListener = mock(RepositoryChangeListener.class);
    doAnswer(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        longPollFinished.set(true);
        return null;
      }

    }).when(someListener).onRepositoryChange(any(String.class), any(Properties.class));

    RemoteConfigRepository remoteConfigRepository = new RemoteConfigRepository(someNamespace);
    remoteConfigRepository.addChangeListener(someListener);
    final ArgumentCaptor<Properties> captor = ArgumentCaptor.forClass(Properties.class);

    Map<String, String> newConfigurations = ImmutableMap.of("someKey", "anotherValue");
    ApolloConfig newApolloConfig = assembleApolloConfig(newConfigurations);

    ApolloNotificationMessages notificationMessages = new ApolloNotificationMessages();
    String someKey = "someKey";
    long someNotificationId = 1;
    notificationMessages.put(someKey, someNotificationId);

    ApolloConfigNotification someNotification = mock(ApolloConfigNotification.class);
    when(someNotification.getNamespaceName()).thenReturn(someNamespace);
    when(someNotification.getMessages()).thenReturn(notificationMessages);

    when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);
    when(pollResponse.getBody()).thenReturn(Lists.newArrayList(someNotification));
    when(someResponse.getBody()).thenReturn(newApolloConfig);

    longPollFinished.get(5000, TimeUnit.MILLISECONDS);

    remoteConfigLongPollService.stopLongPollingRefresh();

    verify(someListener, times(1)).onRepositoryChange(eq(someNamespace), captor.capture());
    assertEquals(newConfigurations, captor.getValue());

    final ArgumentCaptor<HttpRequest> httpRequestArgumentCaptor = ArgumentCaptor
        .forClass(HttpRequest.class);
    verify(httpUtil, atLeast(2)).doGet(httpRequestArgumentCaptor.capture(), eq(ApolloConfig.class));

    HttpRequest request = httpRequestArgumentCaptor.getValue();

    assertTrue(request.getUrl().contains("messages=%7B%22details%22%3A%7B%22someKey%22%3A1%7D%7D"));
  }

  @Test
  public void testAssembleQueryConfigUrl() throws Exception {
    Gson gson = new Gson();
    String someUri = "http://someServer";
    String someAppId = "someAppId";
    String someCluster = "someCluster+ &.-_someSign";
    String someReleaseKey = "20160705193346-583078ef5716c055+20160705193308-31c471ddf9087c3f";

    ApolloNotificationMessages notificationMessages = new ApolloNotificationMessages();
    String someKey = "someKey";
    long someNotificationId = 1;
    String anotherKey = "anotherKey";
    long anotherNotificationId = 2;
    notificationMessages.put(someKey, someNotificationId);
    notificationMessages.put(anotherKey, anotherNotificationId);

    RemoteConfigRepository remoteConfigRepository = new RemoteConfigRepository(someNamespace);
    ApolloConfig someApolloConfig = mock(ApolloConfig.class);
    when(someApolloConfig.getReleaseKey()).thenReturn(someReleaseKey);

    String queryConfigUrl = remoteConfigRepository
        .assembleQueryConfigUrl(someUri, someAppId, someCluster, someNamespace, null,
            notificationMessages,
            someApolloConfig);

    remoteConfigLongPollService.stopLongPollingRefresh();
    assertTrue(queryConfigUrl
        .contains(
            "http://someServer/configs/someAppId/someCluster+%20&.-_someSign/" + someNamespace));
    assertTrue(queryConfigUrl
        .contains("releaseKey=20160705193346-583078ef5716c055%2B20160705193308-31c471ddf9087c3f"));
    assertTrue(queryConfigUrl
        .contains("messages=" + UrlEscapers.urlFormParameterEscaper()
            .escape(gson.toJson(notificationMessages))));
  }

  private ApolloConfig assembleApolloConfig(Map<String, String> configurations) {
    String someAppId = "appId";
    String someClusterName = "cluster";
    String someReleaseKey = "1";
    ApolloConfig apolloConfig =
        new ApolloConfig(someAppId, someClusterName, someNamespace, someReleaseKey);

    apolloConfig.setConfigurations(configurations);

    return apolloConfig;
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

    @Override
    public String getAccessKeySecret() {
      return someSecret;
    }

    @Override
    public String getDataCenter() {
      return null;
    }

    @Override
    public int getLoadConfigQPS() {
      return 200;
    }

    @Override
    public int getLongPollQPS() {
      return 200;
    }

    @Override
    public long getOnErrorRetryInterval() {
      return 10;
    }

    @Override
    public TimeUnit getOnErrorRetryIntervalTimeUnit() {
      return TimeUnit.MILLISECONDS;
    }

    @Override
    public long getLongPollingInitialDelayInMills() {
      return 0;
    }
  }

  public static class MockHttpUtil extends HttpUtil {

    @Override
    public <T> HttpResponse<T> doGet(HttpRequest httpRequest, Class<T> responseType) {
      if (someResponse.getStatusCode() == 200 || someResponse.getStatusCode() == 304) {
        return (HttpResponse<T>) someResponse;
      }
      throw new ApolloConfigStatusCodeException(someResponse.getStatusCode(),
              String.format("Http request failed due to status code: %d",
          someResponse.getStatusCode()));
    }

    @Override
    public <T> HttpResponse<T> doGet(HttpRequest httpRequest, Type responseType) {
      try {
        TimeUnit.MILLISECONDS.sleep(50);
      } catch (InterruptedException e) {
      }
      return (HttpResponse<T>) pollResponse;
    }
  }

}
