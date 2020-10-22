package com.ctrip.framework.apollo.openapi.filter;

import com.ctrip.framework.apollo.openapi.util.ConsumerAuditUtil;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsumerAuthenticationFilterTest {
  private ConsumerAuthenticationFilter authenticationFilter;
  @Mock
  private ConsumerAuthUtil consumerAuthUtil;
  @Mock
  private ConsumerAuditUtil consumerAuditUtil;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private FilterChain filterChain;

  @Before
  public void setUp() throws Exception {
    authenticationFilter = new ConsumerAuthenticationFilter(consumerAuthUtil, consumerAuditUtil);
  }

  @Test
  public void testAuthSuccessfully() throws Exception {
    String someToken = "someToken";
    Long someConsumerId = 1L;

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someToken);
    when(consumerAuthUtil.getConsumerId(someToken)).thenReturn(someConsumerId);

    authenticationFilter.doFilter(request, response, filterChain);

    verify(consumerAuthUtil, times(1)).storeConsumerId(request, someConsumerId);
    verify(consumerAuditUtil, times(1)).audit(request, someConsumerId);
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void testAuthFailed() throws Exception {
    String someInvalidToken = "someInvalidToken";

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someInvalidToken);
    when(consumerAuthUtil.getConsumerId(someInvalidToken)).thenReturn(null);

    authenticationFilter.doFilter(request, response, filterChain);

    verify(response, times(1)).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
    verify(consumerAuthUtil, never()).storeConsumerId(eq(request), anyLong());
    verify(consumerAuditUtil, never()).audit(eq(request), anyLong());
    verify(filterChain, never()).doFilter(request, response);
  }
}
