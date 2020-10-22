package com.ctrip.framework.apollo.internals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;
import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.core.signature.Signature;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.http.HttpRequest;
import com.ctrip.framework.apollo.util.http.HttpResponse;
import com.ctrip.framework.apollo.util.http.HttpUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.SettableFuture;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class RemoteConfigLongPollServiceTest {
  private RemoteConfigLongPollService remoteConfigLongPollService;
  @Mock
  private HttpResponse<List<ApolloConfigNotification>> pollResponse;
  @Mock
  private HttpUtil httpUtil;
  @Mock
  private ConfigServiceLocator configServiceLocator;
  private Type responseType;

  private static String someServerUrl;
  private static String someAppId;
  private static String someCluster;
  private static String someSecret;

  @Before
  public void setUp() throws Exception {
    MockInjector.setInstance(HttpUtil.class, httpUtil);

    someServerUrl = "http://someServer";
    ServiceDTO serviceDTO = mock(ServiceDTO.class);
    when(serviceDTO.getHomepageUrl()).thenReturn(someServerUrl);
    when(configServiceLocator.getConfigServices()).thenReturn(Lists.newArrayList(serviceDTO));
    MockInjector.setInstance(ConfigServiceLocator.class, configServiceLocator);

    MockInjector.setInstance(ConfigUtil.class, new MockConfigUtil());

    remoteConfigLongPollService = new RemoteConfigLongPollService();

    responseType =
        (Type) ReflectionTestUtils.getField(remoteConfigLongPollService, "m_responseType");

    someAppId = "someAppId";
    someCluster = "someCluster";
  }

  @After
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testSubmitLongPollNamespaceWith304Response() throws Exception {
    RemoteConfigRepository someRepository = mock(RemoteConfigRepository.class);
    final String someNamespace = "someNamespace";

    when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_NOT_MODIFIED);
    final SettableFuture<Boolean> longPollFinished = SettableFuture.create();

    doAnswer(new Answer<HttpResponse<List<ApolloConfigNotification>>>() {
      @Override
      public HttpResponse<List<ApolloConfigNotification>> answer(InvocationOnMock invocation)
          throws Throwable {
        try {
          TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
        }
        HttpRequest request = invocation.getArgumentAt(0, HttpRequest.class);

        assertTrue(request.getUrl().contains(someServerUrl + "/notifications/v2?"));
        assertTrue(request.getUrl().contains("appId=" + someAppId));
        assertTrue(request.getUrl().contains("cluster=" + someCluster));
        assertTrue(request.getUrl().contains("notifications="));
        assertTrue(request.getUrl().contains(someNamespace));

        longPollFinished.set(true);
        return pollResponse;
      }
    }).when(httpUtil).doGet(any(HttpRequest.class), eq(responseType));

    remoteConfigLongPollService.submit(someNamespace, someRepository);

    longPollFinished.get(5000, TimeUnit.MILLISECONDS);

    remoteConfigLongPollService.stopLongPollingRefresh();

    verify(someRepository, never()).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));
  }

  @Test
  public void testSubmitLongPollNamespaceWith200Response() throws Exception {
    RemoteConfigRepository someRepository = mock(RemoteConfigRepository.class);
    final String someNamespace = "someNamespace";

    ApolloNotificationMessages notificationMessages = new ApolloNotificationMessages();
    String someKey = "someKey";
    long someNotificationId = 1;
    String anotherKey = "anotherKey";
    long anotherNotificationId = 2;
    notificationMessages.put(someKey, someNotificationId);
    notificationMessages.put(anotherKey, anotherNotificationId);

    ApolloConfigNotification someNotification = mock(ApolloConfigNotification.class);
    when(someNotification.getNamespaceName()).thenReturn(someNamespace);
    when(someNotification.getMessages()).thenReturn(notificationMessages);

    when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);
    when(pollResponse.getBody()).thenReturn(Lists.newArrayList(someNotification));

    doAnswer(new Answer<HttpResponse<List<ApolloConfigNotification>>>() {
      @Override
      public HttpResponse<List<ApolloConfigNotification>> answer(InvocationOnMock invocation)
          throws Throwable {
        try {
          TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
        }

        return pollResponse;
      }
    }).when(httpUtil).doGet(any(HttpRequest.class), eq(responseType));

    final SettableFuture<Boolean> onNotified = SettableFuture.create();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        onNotified.set(true);
        return null;
      }
    }).when(someRepository).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));

    remoteConfigLongPollService.submit(someNamespace, someRepository);

    onNotified.get(5000, TimeUnit.MILLISECONDS);

    remoteConfigLongPollService.stopLongPollingRefresh();

    final ArgumentCaptor<ApolloNotificationMessages> captor = ArgumentCaptor.forClass(ApolloNotificationMessages.class);
    verify(someRepository, times(1)).onLongPollNotified(any(ServiceDTO.class), captor.capture());

    ApolloNotificationMessages captured = captor.getValue();

    assertEquals(2, captured.getDetails().size());
    assertEquals(someNotificationId, captured.get(someKey).longValue());
    assertEquals(anotherNotificationId, captured.get(anotherKey).longValue());
  }

  @Test
  public void testSubmitLongPollNamespaceWithAccessKeySecret() throws Exception {
    someSecret = "someSecret";
    RemoteConfigRepository someRepository = mock(RemoteConfigRepository.class);
    final String someNamespace = "someNamespace";
    ApolloNotificationMessages notificationMessages = new ApolloNotificationMessages();
    String someKey = "someKey";
    long someNotificationId = 1;
    notificationMessages.put(someKey, someNotificationId);

    ApolloConfigNotification someNotification = mock(ApolloConfigNotification.class);
    when(someNotification.getNamespaceName()).thenReturn(someNamespace);
    when(someNotification.getMessages()).thenReturn(notificationMessages);

    when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);
    when(pollResponse.getBody()).thenReturn(Lists.newArrayList(someNotification));

    doAnswer(new Answer<HttpResponse<List<ApolloConfigNotification>>>() {
      @Override
      public HttpResponse<List<ApolloConfigNotification>> answer(InvocationOnMock invocation)
          throws Throwable {
        try {
          TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
        }

        HttpRequest request = invocation.getArgumentAt(0, HttpRequest.class);

        Map<String, String> headers = request.getHeaders();
        assertNotNull(headers);
        assertTrue(headers.containsKey(Signature.HTTP_HEADER_TIMESTAMP));
        assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));

        return pollResponse;
      }
    }).when(httpUtil).doGet(any(HttpRequest.class), eq(responseType));

    final SettableFuture<Boolean> onNotified = SettableFuture.create();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        onNotified.set(true);
        return null;
      }
    }).when(someRepository).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));

    remoteConfigLongPollService.submit(someNamespace, someRepository);
    onNotified.get(5000, TimeUnit.MILLISECONDS);
    remoteConfigLongPollService.stopLongPollingRefresh();

    verify(someRepository, times(1)).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));
  }

  @Test
  public void testSubmitLongPollMultipleNamespaces() throws Exception {
    RemoteConfigRepository someRepository = mock(RemoteConfigRepository.class);
    RemoteConfigRepository anotherRepository = mock(RemoteConfigRepository.class);
    final String someNamespace = "someNamespace";
    final String anotherNamespace = "anotherNamespace";

    final ApolloConfigNotification someNotification = mock(ApolloConfigNotification.class);
    when(someNotification.getNamespaceName()).thenReturn(someNamespace);

    final ApolloConfigNotification anotherNotification = mock(ApolloConfigNotification.class);
    when(anotherNotification.getNamespaceName()).thenReturn(anotherNamespace);

    final SettableFuture<Boolean> submitAnotherNamespaceStart = SettableFuture.create();
    final SettableFuture<Boolean> submitAnotherNamespaceFinish = SettableFuture.create();

    doAnswer(new Answer<HttpResponse<List<ApolloConfigNotification>>>() {
      final AtomicInteger counter = new AtomicInteger();

      @Override
      public HttpResponse<List<ApolloConfigNotification>> answer(InvocationOnMock invocation)
          throws Throwable {
        try {
          TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
        }

        //the first time
        if (counter.incrementAndGet() == 1) {
          HttpRequest request = invocation.getArgumentAt(0, HttpRequest.class);

          assertTrue(request.getUrl().contains("notifications="));
          assertTrue(request.getUrl().contains(someNamespace));

          submitAnotherNamespaceStart.set(true);

          when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);
          when(pollResponse.getBody()).thenReturn(Lists.newArrayList(someNotification));
        } else if (submitAnotherNamespaceFinish.get()) {
          HttpRequest request = invocation.getArgumentAt(0, HttpRequest.class);
          assertTrue(request.getUrl().contains("notifications="));
          assertTrue(request.getUrl().contains(someNamespace));
          assertTrue(request.getUrl().contains(anotherNamespace));

          when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);
          when(pollResponse.getBody()).thenReturn(Lists.newArrayList(anotherNotification));
        } else {
          when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_NOT_MODIFIED);
          when(pollResponse.getBody()).thenReturn(null);
        }

        return pollResponse;
      }
    }).when(httpUtil).doGet(any(HttpRequest.class), eq(responseType));

    final SettableFuture<Boolean> onAnotherRepositoryNotified = SettableFuture.create();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        onAnotherRepositoryNotified.set(true);
        return null;
      }
    }).when(anotherRepository).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));

    remoteConfigLongPollService.submit(someNamespace, someRepository);

    submitAnotherNamespaceStart.get(5000, TimeUnit.MILLISECONDS);
    remoteConfigLongPollService.submit(anotherNamespace, anotherRepository);
    submitAnotherNamespaceFinish.set(true);

    onAnotherRepositoryNotified.get(5000, TimeUnit.MILLISECONDS);

    remoteConfigLongPollService.stopLongPollingRefresh();

    verify(someRepository, times(1)).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));
    verify(anotherRepository, times(1)).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));
  }

  @Test
  public void testSubmitLongPollMultipleNamespacesWithMultipleNotificationsReturned() throws Exception {
    RemoteConfigRepository someRepository = mock(RemoteConfigRepository.class);
    RemoteConfigRepository anotherRepository = mock(RemoteConfigRepository.class);
    final String someNamespace = "someNamespace";
    final String anotherNamespace = "anotherNamespace";

    ApolloNotificationMessages notificationMessages = new ApolloNotificationMessages();
    String someKey = "someKey";
    long someNotificationId = 1;
    notificationMessages.put(someKey, someNotificationId);
    ApolloNotificationMessages anotherNotificationMessages = new ApolloNotificationMessages();
    String anotherKey = "anotherKey";
    long anotherNotificationId = 2;
    anotherNotificationMessages.put(anotherKey, anotherNotificationId);

    final ApolloConfigNotification someNotification = mock(ApolloConfigNotification.class);
    when(someNotification.getNamespaceName()).thenReturn(someNamespace);
    when(someNotification.getMessages()).thenReturn(notificationMessages);

    final ApolloConfigNotification anotherNotification = mock(ApolloConfigNotification.class);
    when(anotherNotification.getNamespaceName()).thenReturn(anotherNamespace);
    when(anotherNotification.getMessages()).thenReturn(anotherNotificationMessages);

    when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);
    when(pollResponse.getBody()).thenReturn(Lists.newArrayList(someNotification, anotherNotification));

    doAnswer(new Answer<HttpResponse<List<ApolloConfigNotification>>>() {
      @Override
      public HttpResponse<List<ApolloConfigNotification>> answer(InvocationOnMock invocation)
          throws Throwable {
        try {
          TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
        }

        return pollResponse;
      }
    }).when(httpUtil).doGet(any(HttpRequest.class), eq(responseType));

    final SettableFuture<Boolean> someRepositoryNotified = SettableFuture.create();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        someRepositoryNotified.set(true);
        return null;
      }
    }).when(someRepository).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));
    final SettableFuture<Boolean> anotherRepositoryNotified = SettableFuture.create();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        anotherRepositoryNotified.set(true);
        return null;
      }
    }).when(anotherRepository).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));

    remoteConfigLongPollService.submit(someNamespace, someRepository);
    remoteConfigLongPollService.submit(anotherNamespace, anotherRepository);

    someRepositoryNotified.get(5000, TimeUnit.MILLISECONDS);
    anotherRepositoryNotified.get(5000, TimeUnit.MILLISECONDS);

    remoteConfigLongPollService.stopLongPollingRefresh();

    final ArgumentCaptor<ApolloNotificationMessages> captor = ArgumentCaptor.forClass(ApolloNotificationMessages.class);
    final ArgumentCaptor<ApolloNotificationMessages> anotherCaptor = ArgumentCaptor.forClass(ApolloNotificationMessages.class);
    verify(someRepository, times(1)).onLongPollNotified(any(ServiceDTO.class), captor.capture());
    verify(anotherRepository, times(1)).onLongPollNotified(any(ServiceDTO.class), anotherCaptor.capture());

    ApolloNotificationMessages result = captor.getValue();
    assertEquals(1, result.getDetails().size());
    assertEquals(someNotificationId, result.get(someKey).longValue());

    ApolloNotificationMessages anotherResult = anotherCaptor.getValue();
    assertEquals(1, anotherResult.getDetails().size());
    assertEquals(anotherNotificationId, anotherResult.get(anotherKey).longValue());
  }

  @Test
  public void testSubmitLongPollNamespaceWithMessagesUpdated() throws Exception {
    RemoteConfigRepository someRepository = mock(RemoteConfigRepository.class);
    final String someNamespace = "someNamespace";

    ApolloNotificationMessages notificationMessages = new ApolloNotificationMessages();
    String someKey = "someKey";
    long someNotificationId = 1;
    notificationMessages.put(someKey, someNotificationId);

    ApolloConfigNotification someNotification = mock(ApolloConfigNotification.class);
    when(someNotification.getNamespaceName()).thenReturn(someNamespace);
    when(someNotification.getMessages()).thenReturn(notificationMessages);

    when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);
    when(pollResponse.getBody()).thenReturn(Lists.newArrayList(someNotification));

    doAnswer(new Answer<HttpResponse<List<ApolloConfigNotification>>>() {
      @Override
      public HttpResponse<List<ApolloConfigNotification>> answer(InvocationOnMock invocation)
          throws Throwable {
        try {
          TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
        }

        return pollResponse;
      }
    }).when(httpUtil).doGet(any(HttpRequest.class), eq(responseType));

    final SettableFuture<Boolean> onNotified = SettableFuture.create();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        onNotified.set(true);
        return null;
      }
    }).when(someRepository).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));

    remoteConfigLongPollService.submit(someNamespace, someRepository);

    onNotified.get(5000, TimeUnit.MILLISECONDS);

    //reset to 304
    when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_NOT_MODIFIED);

    final ArgumentCaptor<ApolloNotificationMessages> captor = ArgumentCaptor.forClass(ApolloNotificationMessages.class);
    verify(someRepository, times(1)).onLongPollNotified(any(ServiceDTO.class), captor.capture());

    ApolloNotificationMessages captured = captor.getValue();

    assertEquals(1, captured.getDetails().size());
    assertEquals(someNotificationId, captured.get(someKey).longValue());

    final SettableFuture<Boolean> anotherOnNotified = SettableFuture.create();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        anotherOnNotified.set(true);
        return null;
      }
    }).when(someRepository).onLongPollNotified(any(ServiceDTO.class), any(ApolloNotificationMessages.class));

    String anotherKey = "anotherKey";
    long anotherNotificationId = 2;
    notificationMessages.put(anotherKey, anotherNotificationId);

    //send notifications
    when(pollResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);

    anotherOnNotified.get(5000, TimeUnit.MILLISECONDS);

    remoteConfigLongPollService.stopLongPollingRefresh();

    verify(someRepository, times(2)).onLongPollNotified(any(ServiceDTO.class), captor.capture());

    captured = captor.getValue();

    assertEquals(2, captured.getDetails().size());
    assertEquals(someNotificationId, captured.get(someKey).longValue());
    assertEquals(anotherNotificationId, captured.get(anotherKey).longValue());
  }

  @Test
  public void testAssembleLongPollRefreshUrl() throws Exception {
    String someUri = someServerUrl;
    String someAppId = "someAppId";
    String someCluster = "someCluster+ &.-_someSign";
    String someNamespace = "someName";
    long someNotificationId = 1;
    Map<String, Long> notificationsMap = ImmutableMap.of(someNamespace, someNotificationId);

    String longPollRefreshUrl =
        remoteConfigLongPollService
            .assembleLongPollRefreshUrl(someUri, someAppId, someCluster, null, notificationsMap);

    assertTrue(longPollRefreshUrl.contains(someServerUrl + "/notifications/v2?"));
    assertTrue(longPollRefreshUrl.contains("appId=" + someAppId));
    assertTrue(longPollRefreshUrl.contains("cluster=someCluster%2B+%26.-_someSign"));
    assertTrue(longPollRefreshUrl.contains(
        "notifications=%5B%7B%22namespaceName%22%3A%22" + someNamespace
            + "%22%2C%22notificationId%22%3A" + 1 + "%7D%5D"));
  }

  @Test
  public void testAssembleLongPollRefreshUrlWithMultipleNamespaces() throws Exception {
    String someUri = someServerUrl;
    String someAppId = "someAppId";
    String someCluster = "someCluster+ &.-_someSign";
    String someNamespace = "someName";
    String anotherNamespace = "anotherName";
    long someNotificationId = 1;
    long anotherNotificationId = 2;
    Map<String, Long> notificationsMap =
        ImmutableMap.of(someNamespace, someNotificationId, anotherNamespace, anotherNotificationId);

    String longPollRefreshUrl =
        remoteConfigLongPollService
            .assembleLongPollRefreshUrl(someUri, someAppId, someCluster, null, notificationsMap);

    assertTrue(longPollRefreshUrl.contains(someServerUrl + "/notifications/v2?"));
    assertTrue(longPollRefreshUrl.contains("appId=" + someAppId));
    assertTrue(longPollRefreshUrl.contains("cluster=someCluster%2B+%26.-_someSign"));
    assertTrue(
        longPollRefreshUrl.contains("notifications=%5B%7B%22namespaceName%22%3A%22" + someNamespace
            + "%22%2C%22notificationId%22%3A" + someNotificationId
            + "%7D%2C%7B%22namespaceName%22%3A%22" + anotherNamespace
            + "%22%2C%22notificationId%22%3A" + anotherNotificationId + "%7D%5D"));
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
    public long getLongPollingInitialDelayInMills() {
      return 0;
    }
  }

}
