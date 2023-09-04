/*
 * Copyright 2023 Apollo Authors
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

import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class ApolloAuditHttpTracerInterceptor implements ClientHttpRequestInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(
      ApolloAuditHttpTracerInterceptor.class);

  //tracer might be and could be null
  private ApolloAuditTracer tracer;

  public ApolloAuditHttpTracerInterceptor() {
  }

  public ApolloAuditHttpTracerInterceptor(ApolloAuditTracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    // will set headers only when tracer is injected
    if (tracer != null && tracer.scopeManager() != null
        && tracer.scopeManager().activeSpanContext() != null) {
      HttpHeaders headers = request.getHeaders();
      Map map = tracer.extract();
      headers.putAll(map);
      logger.info("carried Audit-Log headers");
    }
    ClientHttpResponse response = execution.execute(request, body);
    return response;
  }
}