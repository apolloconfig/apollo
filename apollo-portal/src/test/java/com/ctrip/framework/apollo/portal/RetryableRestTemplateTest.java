package com.ctrip.framework.apollo.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.portal.component.AdminServiceAddressLocator;
import com.ctrip.framework.apollo.portal.component.RetryableRestTemplate;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.environment.PortalMetaDomainService;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class RetryableRestTemplateTest extends AbstractUnitTest {

  @Mock
  private AdminServiceAddressLocator serviceAddressLocator;
  @Mock
  private RestTemplate restTemplate;
  @Mock
  private PortalMetaDomainService portalMetaDomainService;
  @Mock
  private PortalConfig portalConfig;
  @InjectMocks
  private RetryableRestTemplate retryableRestTemplate;

  private static final Gson GSON = new Gson();

  private String path = "app";
  private String serviceOne = "http://10.0.0.1";
  private String serviceTwo = "http://10.0.0.2";
  private String serviceThree = "http://10.0.0.3";
  private ResourceAccessException socketTimeoutException = new ResourceAccessException("");
  private ResourceAccessException httpHostConnectException = new ResourceAccessException("");
  private ResourceAccessException connectTimeoutException = new ResourceAccessException("");
  private Object request = new Object();
  private Object result = new Object();
  private Class<?> requestType = request.getClass();

  @Before
  public void init() {
    socketTimeoutException.initCause(new SocketTimeoutException());

    httpHostConnectException
        .initCause(new HttpHostConnectException(new ConnectTimeoutException(),
            new HttpHost(serviceOne, 80)));
    connectTimeoutException.initCause(new ConnectTimeoutException());
  }

  @Test(expected = ServiceException.class)
  public void testNoAdminServer() {

    when(serviceAddressLocator.getServiceList(any())).thenReturn(Collections.emptyList());

    retryableRestTemplate.get(Env.DEV, path, Object.class);
  }

  @Test(expected = ServiceException.class)
  public void testAllServerDown() {

    when(serviceAddressLocator.getServiceList(any()))
        .thenReturn(Arrays
            .asList(mockService(serviceOne), mockService(serviceTwo), mockService(serviceThree)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class))).thenThrow(socketTimeoutException);
    when(restTemplate
        .exchange(eq(serviceTwo + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class))).thenThrow(httpHostConnectException);
    when(restTemplate
        .exchange(eq(serviceThree + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class))).thenThrow(connectTimeoutException);

    retryableRestTemplate.get(Env.DEV, path, Object.class);

    verify(restTemplate, times(1))
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class));
    verify(restTemplate, times(1))
        .exchange(eq(serviceTwo + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class));
    verify(restTemplate, times(1))
        .exchange(eq(serviceThree + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class));
  }

  @Test
  public void testOneServerDown() {
    ResponseEntity someEntity = mock(ResponseEntity.class);
    when(someEntity.getBody()).thenReturn(result);

    when(serviceAddressLocator.getServiceList(any()))
        .thenReturn(Arrays
            .asList(mockService(serviceOne), mockService(serviceTwo), mockService(serviceThree)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class))).thenThrow(socketTimeoutException);
    when(restTemplate
        .exchange(eq(serviceTwo + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class))).thenReturn(someEntity);
    when(restTemplate
        .exchange(eq(serviceThree + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class))).thenThrow(connectTimeoutException);

    Object actualResult = retryableRestTemplate.get(Env.DEV, path, Object.class);

    verify(restTemplate, times(1))
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class));
    verify(restTemplate, times(1))
        .exchange(eq(serviceTwo + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class));
    verify(restTemplate, never())
        .exchange(eq(serviceThree + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Object.class));
    assertEquals(result, actualResult);
  }

  @Test
  public void testPostSocketTimeoutNotRetry() {
    ResponseEntity someEntity = mock(ResponseEntity.class);
    when(someEntity.getBody()).thenReturn(result);

    when(serviceAddressLocator.getServiceList(any()))
        .thenReturn(Arrays
            .asList(mockService(serviceOne), mockService(serviceTwo), mockService(serviceThree)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.POST), any(HttpEntity.class),
            eq(Object.class))).thenThrow(socketTimeoutException);
    when(restTemplate
        .exchange(eq(serviceTwo + "/" + path), eq(HttpMethod.POST), any(HttpEntity.class),
            eq(Object.class))).thenReturn(someEntity);

    Throwable exception = null;
    Object actualResult = null;
    try {
      actualResult = retryableRestTemplate.post(Env.DEV, path, request, Object.class);
    } catch (Throwable ex) {
      exception = ex;
    }

    assertNull(actualResult);
    assertSame(socketTimeoutException, exception);
    verify(restTemplate, times(1))
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.POST), any(HttpEntity.class),
            eq(Object.class));
    verify(restTemplate, never())
        .exchange(eq(serviceTwo + "/" + path), eq(HttpMethod.POST), any(HttpEntity.class),
            eq(Object.class));
  }

  @Test
  public void testDelete() {
    ResponseEntity someEntity = mock(ResponseEntity.class);

    when(serviceAddressLocator.getServiceList(any()))
        .thenReturn(Arrays
            .asList(mockService(serviceOne), mockService(serviceTwo), mockService(serviceThree)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.DELETE), any(HttpEntity.class),
            (Class<Object>) isNull())).thenReturn(someEntity);

    retryableRestTemplate.delete(Env.DEV, path);

    verify(restTemplate)
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.DELETE), any(HttpEntity.class),
            (Class<Object>) isNull());
  }

  @Test
  public void testPut() {
    ResponseEntity someEntity = mock(ResponseEntity.class);

    when(serviceAddressLocator.getServiceList(any()))
        .thenReturn(Arrays
            .asList(mockService(serviceOne), mockService(serviceTwo), mockService(serviceThree)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.PUT), any(HttpEntity.class),
            (Class<Object>) isNull())).thenReturn(someEntity);

    retryableRestTemplate.put(Env.DEV, path, request);

    ArgumentCaptor<HttpEntity> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.PUT), argumentCaptor.capture(),
            (Class<Object>) isNull());

    assertEquals(request, argumentCaptor.getValue().getBody());
  }

  @Test
  public void testPostObjectWithNoAccessToken() {
    Env someEnv = Env.DEV;
    ResponseEntity someEntity = mock(ResponseEntity.class);

    when(serviceAddressLocator.getServiceList(someEnv))
        .thenReturn(Collections.singletonList(mockService(serviceOne)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.POST), any(HttpEntity.class),
            eq(requestType))).thenReturn(someEntity);
    when(someEntity.getBody()).thenReturn(result);

    Object actualResult = retryableRestTemplate.post(someEnv, path, request, requestType);

    assertEquals(result, actualResult);

    ArgumentCaptor<HttpEntity> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate, times(1))
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.POST), argumentCaptor.capture(),
            eq(requestType));

    HttpEntity entity = argumentCaptor.getValue();
    HttpHeaders headers = entity.getHeaders();

    assertSame(request, entity.getBody());
    assertTrue(headers.isEmpty());
  }

  @Test
  public void testPostObjectWithAccessToken() {
    Env someEnv = Env.DEV;
    String someToken = "someToken";
    ResponseEntity someEntity = mock(ResponseEntity.class);

    when(portalConfig.getAdminServiceAccessTokens())
        .thenReturn(mockAdminServiceTokens(someEnv, someToken));
    when(serviceAddressLocator.getServiceList(someEnv))
        .thenReturn(Collections.singletonList(mockService(serviceOne)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.POST), any(HttpEntity.class),
            eq(requestType))).thenReturn(someEntity);
    when(someEntity.getBody()).thenReturn(result);

    Object actualResult = retryableRestTemplate.post(someEnv, path, request, requestType);

    assertEquals(result, actualResult);

    ArgumentCaptor<HttpEntity> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate, times(1))
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.POST), argumentCaptor.capture(),
            eq(requestType));

    HttpEntity entity = argumentCaptor.getValue();
    HttpHeaders headers = entity.getHeaders();
    List<String> headerValue = headers.get(HttpHeaders.AUTHORIZATION);

    assertSame(request, entity.getBody());
    assertEquals(1, headers.size());
    assertEquals(1, headerValue.size());
    assertEquals(someToken, headerValue.get(0));
  }

  @Test
  public void testPostObjectWithNoAccessTokenForEnv() {
    Env someEnv = Env.DEV;
    Env anotherEnv = Env.PRO;
    String someToken = "someToken";
    ResponseEntity someEntity = mock(ResponseEntity.class);

    when(portalConfig.getAdminServiceAccessTokens())
        .thenReturn(mockAdminServiceTokens(someEnv, someToken));
    when(serviceAddressLocator.getServiceList(someEnv))
        .thenReturn(Collections.singletonList(mockService(serviceOne)));
    when(serviceAddressLocator.getServiceList(anotherEnv))
        .thenReturn(Collections.singletonList(mockService(serviceTwo)));
    when(restTemplate
        .exchange(eq(serviceTwo + "/" + path), eq(HttpMethod.POST), any(HttpEntity.class),
            eq(requestType))).thenReturn(someEntity);
    when(someEntity.getBody()).thenReturn(result);

    Object actualResult = retryableRestTemplate.post(anotherEnv, path, request, requestType);

    assertEquals(result, actualResult);

    ArgumentCaptor<HttpEntity> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate, times(1))
        .exchange(eq(serviceTwo + "/" + path), eq(HttpMethod.POST), argumentCaptor.capture(),
            eq(requestType));

    HttpEntity entity = argumentCaptor.getValue();
    HttpHeaders headers = entity.getHeaders();

    assertSame(request, entity.getBody());
    assertTrue(headers.isEmpty());
  }

  @Test
  public void testPostEntityWithNoAccessToken() {
    Env someEnv = Env.DEV;
    String originalHeader = "someHeader";
    String originalValue = "someValue";
    HttpHeaders originalHeaders = new HttpHeaders();
    originalHeaders.add(originalHeader, originalValue);
    HttpEntity<Object> requestEntity = new HttpEntity<>(request, originalHeaders);
    ResponseEntity someEntity = mock(ResponseEntity.class);

    when(serviceAddressLocator.getServiceList(someEnv))
        .thenReturn(Collections.singletonList(mockService(serviceOne)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.POST), any(HttpEntity.class),
            eq(requestType))).thenReturn(someEntity);
    when(someEntity.getBody()).thenReturn(result);

    Object actualResult = retryableRestTemplate.post(someEnv, path, requestEntity, requestType);

    assertEquals(result, actualResult);

    ArgumentCaptor<HttpEntity> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate, times(1))
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.POST), argumentCaptor.capture(),
            eq(requestType));

    HttpEntity entity = argumentCaptor.getValue();

    assertSame(requestEntity, entity);
    assertSame(request, entity.getBody());
    assertEquals(originalHeaders, entity.getHeaders());
  }

  @Test
  public void testPostEntityWithAccessToken() {
    Env someEnv = Env.DEV;
    String someToken = "someToken";
    String originalHeader = "someHeader";
    String originalValue = "someValue";
    HttpHeaders originalHeaders = new HttpHeaders();
    originalHeaders.add(originalHeader, originalValue);
    HttpEntity<Object> requestEntity = new HttpEntity<>(request, originalHeaders);
    ResponseEntity someEntity = mock(ResponseEntity.class);

    when(portalConfig.getAdminServiceAccessTokens())
        .thenReturn(mockAdminServiceTokens(someEnv, someToken));
    when(serviceAddressLocator.getServiceList(someEnv))
        .thenReturn(Collections.singletonList(mockService(serviceOne)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.POST), any(HttpEntity.class),
            eq(requestType))).thenReturn(someEntity);
    when(someEntity.getBody()).thenReturn(result);

    Object actualResult = retryableRestTemplate.post(someEnv, path, requestEntity, requestType);

    assertEquals(result, actualResult);

    ArgumentCaptor<HttpEntity> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate, times(1))
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.POST), argumentCaptor.capture(),
            eq(requestType));

    HttpEntity entity = argumentCaptor.getValue();
    HttpHeaders headers = entity.getHeaders();

    assertSame(request, entity.getBody());
    assertEquals(2, headers.size());
    assertEquals(originalValue, headers.get(originalHeader).get(0));
    assertEquals(someToken, headers.get(HttpHeaders.AUTHORIZATION).get(0));
  }

  @Test
  public void testGetEntityWithNoAccessToken() {
    Env someEnv = Env.DEV;
    ParameterizedTypeReference requestType = mock(ParameterizedTypeReference.class);
    ResponseEntity someEntity = mock(ResponseEntity.class);

    when(serviceAddressLocator.getServiceList(someEnv))
        .thenReturn(Collections.singletonList(mockService(serviceOne)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(requestType))).thenReturn(someEntity);

    ResponseEntity actualResult = retryableRestTemplate.get(someEnv, path, requestType);

    assertEquals(someEntity, actualResult);

    ArgumentCaptor<HttpEntity> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate, times(1))
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.GET), argumentCaptor.capture(),
            eq(requestType));

    HttpHeaders headers = argumentCaptor.getValue().getHeaders();

    assertTrue(headers.isEmpty());
  }

  @Test
  public void testGetEntityWithAccessToken() {
    Env someEnv = Env.DEV;
    String someToken = "someToken";
    ParameterizedTypeReference requestType = mock(ParameterizedTypeReference.class);
    ResponseEntity someEntity = mock(ResponseEntity.class);

    when(portalConfig.getAdminServiceAccessTokens())
        .thenReturn(mockAdminServiceTokens(someEnv, someToken));
    when(serviceAddressLocator.getServiceList(someEnv))
        .thenReturn(Collections.singletonList(mockService(serviceOne)));
    when(restTemplate
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(requestType))).thenReturn(someEntity);

    ResponseEntity actualResult = retryableRestTemplate.get(someEnv, path, requestType);

    assertEquals(someEntity, actualResult);

    ArgumentCaptor<HttpEntity> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate, times(1))
        .exchange(eq(serviceOne + "/" + path), eq(HttpMethod.GET), argumentCaptor.capture(),
            eq(requestType));

    HttpHeaders headers = argumentCaptor.getValue().getHeaders();
    List<String> headerValue = headers.get(HttpHeaders.AUTHORIZATION);

    assertEquals(1, headers.size());
    assertEquals(1, headerValue.size());
    assertEquals(someToken, headerValue.get(0));
  }

  @Test
  public void testGetEntityWithNoAccessTokenForEnv() {
    Env someEnv = Env.DEV;
    Env anotherEnv = Env.PRO;
    String someToken = "someToken";
    ParameterizedTypeReference requestType = mock(ParameterizedTypeReference.class);
    ResponseEntity someEntity = mock(ResponseEntity.class);

    when(portalConfig.getAdminServiceAccessTokens())
        .thenReturn(mockAdminServiceTokens(someEnv, someToken));
    when(serviceAddressLocator.getServiceList(someEnv))
        .thenReturn(Collections.singletonList(mockService(serviceOne)));
    when(serviceAddressLocator.getServiceList(anotherEnv))
        .thenReturn(Collections.singletonList(mockService(serviceTwo)));
    when(restTemplate
        .exchange(eq(serviceTwo + "/" + path), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(requestType))).thenReturn(someEntity);

    ResponseEntity actualResult = retryableRestTemplate.get(anotherEnv, path, requestType);

    assertEquals(someEntity, actualResult);

    ArgumentCaptor<HttpEntity> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate, times(1))
        .exchange(eq(serviceTwo + "/" + path), eq(HttpMethod.GET), argumentCaptor.capture(),
            eq(requestType));

    HttpHeaders headers = argumentCaptor.getValue().getHeaders();

    assertTrue(headers.isEmpty());
  }

  private String mockAdminServiceTokens(Env env, String token) {
    Map<String, String> tokenMap = Maps.newHashMap();
    tokenMap.put(env.getName(), token);

    return GSON.toJson(tokenMap);
  }

  private ServiceDTO mockService(String homeUrl) {
    ServiceDTO serviceDTO = new ServiceDTO();
    serviceDTO.setHomepageUrl(homeUrl);
    return serviceDTO;
  }

}
