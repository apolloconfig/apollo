package com.ctrip.framework.apollo.openapi.util;

import com.ctrip.framework.apollo.openapi.entity.ConsumerAudit;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsumerAuditUtilTest {
  private ConsumerAuditUtil consumerAuditUtil;
  @Mock
  private ConsumerService consumerService;
  @Mock
  private HttpServletRequest request;
  private long batchTimeout = 50;
  private TimeUnit batchTimeUnit = TimeUnit.MILLISECONDS;

  @Before
  public void setUp() throws Exception {
    consumerAuditUtil = new ConsumerAuditUtil(consumerService);
    ReflectionTestUtils.setField(consumerAuditUtil, "BATCH_TIMEOUT", batchTimeout);
    ReflectionTestUtils.setField(consumerAuditUtil, "BATCH_TIMEUNIT", batchTimeUnit);
    consumerAuditUtil.afterPropertiesSet();
  }

  @After
  public void tearDown() throws Exception {
    consumerAuditUtil.stopAudit();
  }

  @Test
  public void audit() throws Exception {
    long someConsumerId = 1;
    String someUri = "someUri";
    String someQuery = "someQuery";
    String someMethod = "someMethod";

    when(request.getRequestURI()).thenReturn(someUri);
    when(request.getQueryString()).thenReturn(someQuery);
    when(request.getMethod()).thenReturn(someMethod);

    SettableFuture<List<ConsumerAudit>> result = SettableFuture.create();

    doAnswer((Answer<Void>) invocation -> {
      Object[] args = invocation.getArguments();
      result.set((List<ConsumerAudit>) args[0]);

      return null;
    }).when(consumerService).createConsumerAudits(anyCollection());

    consumerAuditUtil.audit(request, someConsumerId);

    List<ConsumerAudit> audits = result.get(batchTimeout * 5, batchTimeUnit);

    assertEquals(1, audits.size());

    ConsumerAudit audit = audits.get(0);

    assertEquals(String.format("%s?%s", someUri, someQuery), audit.getUri());
    assertEquals(someMethod, audit.getMethod());
    assertEquals(someConsumerId, audit.getConsumerId());
  }

}
