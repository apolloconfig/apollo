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
package com.ctrip.framework.apollo.audit.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.constants.ApolloAuditConstants;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = ApolloAuditSpanAspect.class)
public class ApolloAuditSpanAspectTest {

  @SpyBean
  ApolloAuditSpanAspect aspect;

  @MockBean
  ApolloAuditLogApi api;

  @Test
  public void testAround() throws Throwable {
    final OpType opType = OpType.CREATE;
    final String opName = "App.create";
    final String description = "no description";

    ProceedingJoinPoint mockPJP = mock(ProceedingJoinPoint.class);
    ApolloAuditLog mockAnnotation = mock(ApolloAuditLog.class);
    AutoCloseable mockScope = mock(AutoCloseable.class);
    {
      when(mockAnnotation.type()).thenReturn(opType);
      when(mockAnnotation.name()).thenReturn(opName);
      when(mockAnnotation.description()).thenReturn(description);
      when(api.appendAuditLog(eq(opType), eq(opName), eq(description)))
          .thenReturn(mockScope);
      doNothing().when(aspect).auditDataInfluenceArg(mockPJP);
    }

    aspect.around(mockPJP, mockAnnotation);
    verify(api, times(1))
        .appendAuditLog(eq(opType), eq(opName), eq(description));
    verify(mockScope, times(1))
        .close();
    verify(aspect, times(1))
        .auditDataInfluenceArg(eq(mockPJP));
  }

  @Test
  public void testAuditDataInfluenceArg() throws NoSuchMethodException {
    ProceedingJoinPoint mockPJP = mock(ProceedingJoinPoint.class);
    Object[] args = new Object[]{new Object(), new Object()};
    Method method = MockAuditClass.class.getMethod("mockAuditMethod", Object.class, Object.class);
    {
      doReturn(method).when(aspect).findMethod(any());
      when(mockPJP.getArgs()).thenReturn(args);
    }
    aspect.auditDataInfluenceArg(mockPJP);
    verify(aspect, times(1))
        .parseArgAndAppend(eq("App"), eq("Name"), eq(args[0]));
  }

  @Test
  public void testAuditDataInfluenceArgCaseFindMethodReturnNull() throws NoSuchMethodException {
    ProceedingJoinPoint mockPJP = mock(ProceedingJoinPoint.class);
    Object[] args = new Object[]{new Object(), new Object()};
    {
      doReturn(null).when(aspect).findMethod(any());
      when(mockPJP.getArgs()).thenReturn(args);
    }
    aspect.auditDataInfluenceArg(mockPJP);
    verify(aspect, times(0))
        .parseArgAndAppend(eq("App"), eq("Name"), eq(args[0]));
  }

  @Test
  public void testFindMethod() throws NoSuchMethodException {
    ProceedingJoinPoint mockPJP = mock(ProceedingJoinPoint.class);
    MockAuditClass mockAuditClass = new MockAuditClass();
    MethodSignature signature = mock(MethodSignature.class);
    Method method = MockAuditClass.class.getMethod("mockAuditMethod", Object.class, Object.class);
    Method sameNameMethod = MockAuditClass.class.getMethod("mockAuditMethod", Object.class);
    {
      when(mockPJP.getTarget()).thenReturn(mockAuditClass);
      when(mockPJP.getSignature()).thenReturn(signature);
      when(signature.getName()).thenReturn("mockAuditMethod");
      when(signature.getParameterTypes()).thenReturn(new Class[]{Object.class, Object.class});
    }
    Method methodFounded = aspect.findMethod(mockPJP);

    assertEquals(method, methodFounded);
    assertNotEquals(sameNameMethod, methodFounded);
  }

  @Test
  public void testParseArgAndAppendCaseNullName() {
    Object somewhat = new Object();
    aspect.parseArgAndAppend(null, null, somewhat);
    verify(api, times(0))
        .appendDataInfluence(any(), any(), any(), any());
  }

  @Test
  public void testParseArgAndAppendCaseCollectionTypeArg() {
    final String entityName = "App";
    final String fieldName = "Name";
    List<Object> list = Arrays.asList(new Object(), new Object(), new Object());

    {
      doNothing().when(api).appendDataInfluence(any(), any(), any(), any());
    }
    aspect.parseArgAndAppend(entityName, fieldName, list);
    verify(api, times(list.size())).appendDataInfluence(eq(entityName),
        eq(ApolloAuditConstants.ANY_MATCHED_ID), eq(fieldName), any());
  }

  @Test
  public void testParseArgAndAppendCaseNormalTypeArg() {
    final String entityName = "App";
    final String fieldName = "Name";
    Object arg = new Object();

    {
      doNothing().when(api).appendDataInfluence(any(), any(), any(), any());
    }
    aspect.parseArgAndAppend(entityName, fieldName, arg);
    verify(api, times(1)).appendDataInfluence(eq(entityName),
        eq(ApolloAuditConstants.ANY_MATCHED_ID), eq(fieldName), any());
  }

  public class MockAuditClass {

    public void mockAuditMethod(
        @ApolloAuditLogDataInfluence
        @ApolloAuditLogDataInfluenceTable(tableName = "App")
        @ApolloAuditLogDataInfluenceTableField(fieldName = "Name") Object val1,
        Object val2) {
    }

    // same name method test
    public void mockAuditMethod(
        Object val2) {
    }
  }
}
