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
package com.ctrip.framework.apollo.audit.component;

import com.ctrip.framework.apollo.audit.context.ApolloAuditTraceContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = ApolloAuditHttpInterceptor.class)
public class ApolloAuditHttpInterceptorTest {

  @SpyBean
  ApolloAuditHttpInterceptor interceptor;

  @MockBean
  ApolloAuditTraceContext traceContext;

  @Test
  public void testInterceptor() throws IOException {
    ClientHttpRequestExecution execution = Mockito.mock(ClientHttpRequestExecution.class);
    HttpRequest request = Mockito.mock(HttpRequest.class);
    byte[] body = new byte[]{};
    ApolloAuditTracer tracer = Mockito.mock(ApolloAuditTracer.class);
    HttpRequest mockInjected = Mockito.mock(HttpRequest.class);

    Mockito.when(traceContext.tracer()).thenReturn(tracer);
    Mockito.when(tracer.inject(Mockito.eq(request)))
            .thenReturn(mockInjected);

    interceptor.intercept(request, body, execution);

    Mockito.verify(execution, Mockito.times(1))
        .execute(Mockito.eq(mockInjected), Mockito.eq(body));
  }

  @Test
  public void testInterceptorCaseNoTracer() throws IOException {
    ClientHttpRequestExecution execution = Mockito.mock(ClientHttpRequestExecution.class);
    HttpRequest request = Mockito.mock(HttpRequest.class);
    byte[] body = new byte[]{};
    Mockito.when(traceContext.tracer()).thenReturn(null);

    interceptor.intercept(request, body, execution);

    Mockito.verify(execution, Mockito.times(1))
        .execute(Mockito.eq(request), Mockito.eq(body));
  }

}
