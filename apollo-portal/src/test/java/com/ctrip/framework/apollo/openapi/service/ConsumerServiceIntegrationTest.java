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
package com.ctrip.framework.apollo.openapi.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.entity.Consumer;
import com.ctrip.framework.apollo.portal.AbstractIntegrationTest;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

/**
 * @author wxq
 */
public class ConsumerServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private ConsumerService consumerService;

  @Test
  @Sql(scripts = "/sql/openapi/ConsumerServiceIntegrationTest.testFindAppIdsAuthorizedByConsumerId.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindAppIdsAuthorizedByConsumerId() {
    Set<String> appIds = this.consumerService.findAppIdsAuthorizedByConsumerId(1000L);
    assertEquals(Sets.newHashSet("consumer-test-app-id-0", "consumer-test-app-id-1"), appIds);
    assertFalse(appIds.contains("consumer-test-app-id-2"));
  }

  @Test
  @Sql(scripts = "/sql/openapi/ConsumerServiceIntegrationTest.commonData.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindAllConsumer() {
    List<Consumer> consumerList = consumerService.findAllConsumer(Pageable.ofSize(1));
    assertEquals(1, consumerList.size());
    consumerList = consumerService.findAllConsumer(Pageable.ofSize(4));
    assertEquals(4, consumerList.size());
  }

  @Test
  @Sql(scripts = "/sql/openapi/ConsumerServiceIntegrationTest.commonData.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testDeleteConsumer() {
    long consumerId = 1000;
    String appId = "consumer-test-app-role";

    Assertions.assertThatNoException()
        .isThrownBy(()-> consumerService.deleteConsumer(appId));

    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(()-> consumerService.deleteConsumer(appId))
        .withMessage("ConsumerApp not exist");

    Assertions.assertThat(consumerService.getConsumerByConsumerId(consumerId))
        .isNull();
  }
}
