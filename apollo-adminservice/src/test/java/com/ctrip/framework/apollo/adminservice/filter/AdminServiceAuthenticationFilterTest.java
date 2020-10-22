package com.ctrip.framework.apollo.adminservice.filter;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;

@RunWith(MockitoJUnitRunner.class)
public class AdminServiceAuthenticationFilterTest {

  @Mock
  private BizConfig bizConfig;
  private HttpServletRequest servletRequest;
  private HttpServletResponse servletResponse;
  private FilterChain filterChain;

  private AdminServiceAuthenticationFilter authenticationFilter;

  @Before
  public void setUp() throws Exception {
    authenticationFilter = new AdminServiceAuthenticationFilter(bizConfig);
    initVariables();
  }

  private void initVariables() {
    servletRequest = mock(HttpServletRequest.class);
    servletResponse = mock(HttpServletResponse.class);
    filterChain = mock(FilterChain.class);
  }

  @Test
  public void testWithAccessControlDisabled() throws Exception {
    when(bizConfig.isAdminServiceAccessControlEnabled()).thenReturn(false);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(bizConfig, times(1)).isAdminServiceAccessControlEnabled();
    verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    verify(bizConfig, never()).getAdminServiceAccessTokens();
    verify(servletRequest, never()).getHeader(HttpHeaders.AUTHORIZATION);
    verify(servletResponse, never()).sendError(anyInt(), anyString());
  }

  @Test
  public void testWithAccessControlEnabledWithTokenSpecifiedWithValidTokenPassed()
      throws Exception {
    String someValidToken = "someToken";

    when(bizConfig.isAdminServiceAccessControlEnabled()).thenReturn(true);
    when(bizConfig.getAdminServiceAccessTokens()).thenReturn(someValidToken);
    when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someValidToken);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(bizConfig, times(1)).isAdminServiceAccessControlEnabled();
    verify(bizConfig, times(1)).getAdminServiceAccessTokens();
    verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    verify(servletResponse, never()).sendError(anyInt(), anyString());
  }

  @Test
  public void testWithAccessControlEnabledWithTokenSpecifiedWithInvalidTokenPassed()
      throws Exception {
    String someValidToken = "someValidToken";
    String someInvalidToken = "someInvalidToken";

    when(bizConfig.isAdminServiceAccessControlEnabled()).thenReturn(true);
    when(bizConfig.getAdminServiceAccessTokens()).thenReturn(someValidToken);
    when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someInvalidToken);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(bizConfig, times(1)).isAdminServiceAccessControlEnabled();
    verify(bizConfig, times(1)).getAdminServiceAccessTokens();
    verify(servletResponse, times(1))
        .sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    verify(filterChain, never()).doFilter(servletRequest, servletResponse);
  }

  @Test
  public void testWithAccessControlEnabledWithTokenSpecifiedWithNoTokenPassed() throws Exception {
    String someValidToken = "someValidToken";

    when(bizConfig.isAdminServiceAccessControlEnabled()).thenReturn(true);
    when(bizConfig.getAdminServiceAccessTokens()).thenReturn(someValidToken);
    when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(bizConfig, times(1)).isAdminServiceAccessControlEnabled();
    verify(bizConfig, times(1)).getAdminServiceAccessTokens();
    verify(servletResponse, times(1))
        .sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    verify(filterChain, never()).doFilter(servletRequest, servletResponse);
  }


  @Test
  public void testWithAccessControlEnabledWithMultipleTokenSpecifiedWithValidTokenPassed()
      throws Exception {
    String someToken = "someToken";
    String anotherToken = "anotherToken";

    when(bizConfig.isAdminServiceAccessControlEnabled()).thenReturn(true);
    when(bizConfig.getAdminServiceAccessTokens())
        .thenReturn(String.format("%s,%s", someToken, anotherToken));
    when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someToken);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(bizConfig, times(1)).isAdminServiceAccessControlEnabled();
    verify(bizConfig, times(1)).getAdminServiceAccessTokens();
    verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    verify(servletResponse, never()).sendError(anyInt(), anyString());
  }

  @Test
  public void testWithAccessControlEnabledWithNoTokenSpecifiedWithTokenPassed() throws Exception {
    String someToken = "someToken";

    when(bizConfig.isAdminServiceAccessControlEnabled()).thenReturn(true);
    when(bizConfig.getAdminServiceAccessTokens()).thenReturn(null);
    when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someToken);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(bizConfig, times(1)).isAdminServiceAccessControlEnabled();
    verify(bizConfig, times(1)).getAdminServiceAccessTokens();
    verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    verify(servletResponse, never()).sendError(anyInt(), anyString());
  }

  @Test
  public void testWithAccessControlEnabledWithNoTokenSpecifiedWithNoTokenPassed() throws Exception {
    String someToken = "someToken";

    when(bizConfig.isAdminServiceAccessControlEnabled()).thenReturn(true);
    when(bizConfig.getAdminServiceAccessTokens()).thenReturn(null);
    when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(bizConfig, times(1)).isAdminServiceAccessControlEnabled();
    verify(bizConfig, times(1)).getAdminServiceAccessTokens();
    verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    verify(servletResponse, never()).sendError(anyInt(), anyString());
  }

  @Test
  public void testWithConfigChanged() throws Exception {
    String someToken = "someToken";
    String anotherToken = "anotherToken";
    String yetAnotherToken = "yetAnotherToken";

    // case 1: init state
    when(bizConfig.isAdminServiceAccessControlEnabled()).thenReturn(true);
    when(bizConfig.getAdminServiceAccessTokens()).thenReturn(someToken);

    when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someToken);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    verify(servletResponse, never()).sendError(anyInt(), anyString());

    // case 2: change access tokens specified
    initVariables();
    when(bizConfig.getAdminServiceAccessTokens())
        .thenReturn(String.format("%s,%s", anotherToken, yetAnotherToken));
    when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someToken);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(servletResponse, times(1))
        .sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    verify(filterChain, never()).doFilter(servletRequest, servletResponse);

    initVariables();
    when(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(anotherToken);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    verify(servletResponse, never()).sendError(anyInt(), anyString());

    // case 3: change access control flag
    initVariables();
    when(bizConfig.isAdminServiceAccessControlEnabled()).thenReturn(false);

    authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

    verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    verify(servletResponse, never()).sendError(anyInt(), anyString());
    verify(servletRequest, never()).getHeader(HttpHeaders.AUTHORIZATION);
  }
}