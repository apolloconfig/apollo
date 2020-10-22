package com.ctrip.framework.apollo.configservice.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.configservice.util.AccessKeyUtil;
import com.ctrip.framework.apollo.core.signature.Signature;
import com.google.common.collect.Lists;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;

/**
 * @author nisiyong
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientAuthenticationFilterTest {

  private ClientAuthenticationFilter clientAuthenticationFilter;

  @Mock
  private AccessKeyUtil accessKeyUtil;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private FilterChain filterChain;

  @Before
  public void setUp() {
    clientAuthenticationFilter = new ClientAuthenticationFilter(accessKeyUtil);
  }

  @Test
  public void testInvalidAppId() throws Exception {
    when(accessKeyUtil.extractAppIdFromRequest(any())).thenReturn(null);

    clientAuthenticationFilter.doFilter(request, response, filterChain);

    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "InvalidAppId");
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  public void testRequestTimeTooSkewed() throws Exception {
    String appId = "someAppId";
    List<String> secrets = Lists.newArrayList("someSecret");
    String oneMinAgoTimestamp = Long.toString(System.currentTimeMillis() - 61 * 1000);

    when(accessKeyUtil.extractAppIdFromRequest(any())).thenReturn(appId);
    when(accessKeyUtil.findAvailableSecret(appId)).thenReturn(secrets);
    when(request.getHeader(Signature.HTTP_HEADER_TIMESTAMP)).thenReturn(oneMinAgoTimestamp);

    clientAuthenticationFilter.doFilter(request, response, filterChain);

    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "RequestTimeTooSkewed");
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  public void testRequestTimeOneMinFasterThenCurrentTime() throws Exception {
    String appId = "someAppId";
    List<String> secrets = Lists.newArrayList("someSecret");
    String oneMinAfterTimestamp = Long.toString(System.currentTimeMillis() + 61 * 1000);

    when(accessKeyUtil.extractAppIdFromRequest(any())).thenReturn(appId);
    when(accessKeyUtil.findAvailableSecret(appId)).thenReturn(secrets);
    when(request.getHeader(Signature.HTTP_HEADER_TIMESTAMP)).thenReturn(oneMinAfterTimestamp);

    clientAuthenticationFilter.doFilter(request, response, filterChain);

    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "RequestTimeTooSkewed");
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  public void testUnauthorized() throws Exception {
    String appId = "someAppId";
    String availableSignature = "someSignature";
    List<String> secrets = Lists.newArrayList("someSecret");
    String oneMinAgoTimestamp = Long.toString(System.currentTimeMillis());
    String errorAuthorization = "Apollo someAppId:wrongSignature";

    when(accessKeyUtil.extractAppIdFromRequest(any())).thenReturn(appId);
    when(accessKeyUtil.findAvailableSecret(appId)).thenReturn(secrets);
    when(accessKeyUtil.buildSignature(any(), any(), any(), any())).thenReturn(availableSignature);
    when(request.getHeader(Signature.HTTP_HEADER_TIMESTAMP)).thenReturn(oneMinAgoTimestamp);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(errorAuthorization);

    clientAuthenticationFilter.doFilter(request, response, filterChain);

    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  public void testAuthorizedSuccessfully() throws Exception {
    String appId = "someAppId";
    String availableSignature = "someSignature";
    List<String> secrets = Lists.newArrayList("someSecret");
    String oneMinAgoTimestamp = Long.toString(System.currentTimeMillis());
    String correctAuthorization = "Apollo someAppId:someSignature";

    when(accessKeyUtil.extractAppIdFromRequest(any())).thenReturn(appId);
    when(accessKeyUtil.findAvailableSecret(appId)).thenReturn(secrets);
    when(accessKeyUtil.buildSignature(any(), any(), any(), any())).thenReturn(availableSignature);
    when(request.getHeader(Signature.HTTP_HEADER_TIMESTAMP)).thenReturn(oneMinAgoTimestamp);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(correctAuthorization);

    clientAuthenticationFilter.doFilter(request, response, filterChain);

    verify(response, never()).sendError(HttpServletResponse.SC_BAD_REQUEST, "InvalidAppId");
    verify(response, never()).sendError(HttpServletResponse.SC_UNAUTHORIZED, "RequestTimeTooSkewed");
    verify(response, never()).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    verify(filterChain, times(1)).doFilter(request, response);
  }
}