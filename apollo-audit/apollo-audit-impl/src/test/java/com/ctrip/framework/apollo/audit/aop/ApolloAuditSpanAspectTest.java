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
package com.ctrip.framework.apollo.audit.aop;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = ApolloAuditSpanAspect.class)
public class ApolloAuditSpanAspectTest {

  final OpType opType = OpType.CREATE;
  final String opName = "create";
  final String description = "xxx";

  @SpyBean
  ApolloAuditSpanAspect aspect;

  @MockBean
  ApolloAuditLogApi api;

  @Test
  public void testOpenScopeAndClose() throws Throwable {
    ApolloAuditLog anno = mock(ApolloAuditLog.class);
    ApolloAuditScope scope = mock(ApolloAuditScope.class);
    ProceedingJoinPoint mockPJP = mock(ProceedingJoinPoint.class);
    {
      when(anno.name()).thenReturn(opName);
      when(anno.type()).thenReturn(opType);
      when(anno.description()).thenReturn(description);
      when(api.appendAuditLog(eq(opType), eq(opName), eq(description)))
          .thenReturn(scope);
    }
    aspect.around(mockPJP, anno);

    verify(api).appendAuditLog(eq(opType), eq(opName), eq(description));
    verify(mockPJP).proceed();
    verify(scope).close();
  }
}
