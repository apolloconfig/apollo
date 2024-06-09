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
package com.ctrip.framework.apollo.audit.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ctrip.framework.apollo.audit.constants.ApolloAuditConstants;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.spi.defaultimpl.ApolloAuditOperatorDefaultSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@SpringBootTest
@ContextConfiguration(classes = ApolloAuditOperatorSupplier.class)
public class ApolloAuditOperatorSupplierTest {

  @SpyBean
  ApolloAuditOperatorDefaultSupplier defaultSupplier;

  @MockBean
  RequestAttributes requestAttributes;
  @MockBean
  ApolloAuditTracer tracer;

  @BeforeEach
  public void setUp() {
    Mockito.when(requestAttributes.getAttribute(
        Mockito.eq(ApolloAuditConstants.TRACER),
        Mockito.eq(RequestAttributes.SCOPE_REQUEST))
    ).thenReturn(tracer);
    RequestContextHolder.setRequestAttributes(requestAttributes);
  }

  @Test
  public void testGetOperatorCaseActiveSpanExist() {
    final String operator = "test";
    {
      ApolloAuditSpan activeSpan = new ApolloAuditSpan();
      activeSpan.setContext(new ApolloAuditSpanContext(null, null, operator, null, null));
      Mockito.when(tracer.getActiveSpan()).thenReturn(activeSpan);
    }

    assertEquals(operator, defaultSupplier.getOperator());
  }

  @Test
  public void testGetOperatorCaseActiveSpanNotExist() {
    assertEquals("anonymous", defaultSupplier.getOperator());
  }

}
