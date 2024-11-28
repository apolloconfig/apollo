/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.openapi.filter;

import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuditUtil;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
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
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsumerAuthenticationFilterTest {

  private static final int TOO_MANY_REQUESTS = 429;

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

    ConsumerToken someConsumerToken = new ConsumerToken();
    someConsumerToken.setConsumerId(someConsumerId);

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someToken);
    when(consumerAuthUtil.getConsumerToken(someToken)).thenReturn(someConsumerToken);

    authenticationFilter.doFilter(request, response, filterChain);

    verify(consumerAuthUtil, times(1)).storeConsumerId(request, someConsumerId);
    verify(consumerAuditUtil, times(1)).audit(request, someConsumerId);
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void testAuthFailed() throws Exception {
    String someInvalidToken = "someInvalidToken";

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someInvalidToken);
    when(consumerAuthUtil.getConsumerToken(someInvalidToken)).thenReturn(null);

    authenticationFilter.doFilter(request, response, filterChain);

    verify(response, times(1)).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
    verify(consumerAuthUtil, never()).storeConsumerId(eq(request), anyLong());
    verify(consumerAuditUtil, never()).audit(eq(request), anyLong());
    verify(filterChain, never()).doFilter(request, response);
  }


  @Test
  public void testRateLimitSuccessfully() throws Exception {
    String someToken = "some-ratelimit-success-token";
    Long someConsumerId = 1L;
    int qps = 5;
    int durationInSeconds = 3;

    setupRateLimitMocks(someToken, someConsumerId, qps);

    Runnable task = () -> {
      try {
        authenticationFilter.doFilter(request, response, filterChain);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (ServletException e) {
        throw new RuntimeException(e);
      }
    };

    int realQps = qps - 1;
    executeWithQps(realQps, task, durationInSeconds);

    int total = realQps * durationInSeconds;

    verify(consumerAuthUtil, times(total)).storeConsumerId(request, someConsumerId);
    verify(consumerAuditUtil, times(total)).audit(request, someConsumerId);
    verify(filterChain, times(total)).doFilter(request, response);

  }


  @Test
  public void testRateLimitPartFailure() throws Exception {
     String someToken = "some-ratelimit-fail-token";
    Long someConsumerId = 1L;
    int qps = 5;
    int durationInSeconds = 3;

    setupRateLimitMocks(someToken, someConsumerId, qps);

    Runnable task = () -> {
      try {
        authenticationFilter.doFilter(request, response, filterChain);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (ServletException e) {
        throw new RuntimeException(e);
      }
    };

    int realQps = qps + 3;
    executeWithQps(realQps, task, durationInSeconds);

    int leastTimes = qps * durationInSeconds;
    int mostTimes = realQps * durationInSeconds;

    verify(response, atLeastOnce()).sendError(eq(TOO_MANY_REQUESTS), anyString());

    verify(consumerAuthUtil, atLeast(leastTimes)).storeConsumerId(request, someConsumerId);
    verify(consumerAuthUtil, atMost(mostTimes)).storeConsumerId(request, someConsumerId);
    verify(consumerAuditUtil, atLeast(leastTimes)).audit(request, someConsumerId);
    verify(consumerAuditUtil, atMost(mostTimes)).audit(request, someConsumerId);
    verify(filterChain, atLeast(leastTimes)).doFilter(request, response);
    verify(filterChain, atMost(mostTimes)).doFilter(request, response);

  }


  private void setupRateLimitMocks(String someToken, Long someConsumerId, int qps) {
    ConsumerToken someConsumerToken = new ConsumerToken();
    someConsumerToken.setConsumerId(someConsumerId);
    someConsumerToken.setRateLimit(qps);
    someConsumerToken.setToken(someToken);

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(someToken);
    when(consumerAuthUtil.getConsumerToken(someToken)).thenReturn(someConsumerToken);
  }


  public static void executeWithQps(int qps, Runnable task, int durationInSeconds) {
    ExecutorService executor = Executors.newFixedThreadPool(qps);
    long totalTasks = qps * durationInSeconds;

    for (int i = 0; i < totalTasks; i++) {
      executor.submit(task);
      try {
        TimeUnit.MILLISECONDS.sleep(1000 / qps); // Control QPS
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    executor.shutdown();
  }

}
