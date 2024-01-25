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
package com.ctrip.framework.apollo.biz.repository;


import static org.assertj.core.api.Assertions.assertThat;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.AccessKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;


public class AccessKeyRepositoryTest extends AbstractIntegrationTest {

  @Autowired
  private AccessKeyRepository accessKeyRepository;

  @Test
  public void testSave() {
    String appId = "someAppId";
    String secret = "someSecret";
    AccessKey entity = new AccessKey();
    entity.setAppId(appId);
    entity.setSecret(secret);

    AccessKey accessKey = accessKeyRepository.save(entity);

    assertThat(accessKey).isNotNull();
    assertThat(accessKey.getAppId()).isEqualTo(appId);
    assertThat(accessKey.getSecret()).isEqualTo(secret);
  }

  @Test
  @Sql(scripts = "/sql/accesskey-test.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindByAppId() {
    String appId = "someAppId";

    List<AccessKey> accessKeyList = accessKeyRepository.findByAppId(appId);

    assertThat(accessKeyList).hasSize(1);
    assertThat(accessKeyList.get(0).getAppId()).isEqualTo(appId);
    assertThat(accessKeyList.get(0).getSecret()).isEqualTo("someSecret");
  }

  @Test
  @Sql(scripts = "/sql/accesskey-test.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindFirst500ByDataChangeLastModifiedTimeGreaterThanOrderByDataChangeLastModifiedTime() {
    Instant instant = LocalDateTime.of(2019, 12, 19, 13, 44, 20)
        .atZone(ZoneId.systemDefault())
        .toInstant();
    Date date = Date.from(instant);

    List<AccessKey> accessKeyList = accessKeyRepository
        .findFirst500ByDataChangeLastModifiedTimeGreaterThanOrderByDataChangeLastModifiedTimeAsc(date);

    assertThat(accessKeyList).hasSize(2);
    assertThat(accessKeyList.get(0).getAppId()).isEqualTo("100004458");
    assertThat(accessKeyList.get(0).getSecret()).isEqualTo("4003c4d7783443dc9870932bebf3b7fe");
    assertThat(accessKeyList.get(1).getAppId()).isEqualTo("100004458");
    assertThat(accessKeyList.get(1).getSecret()).isEqualTo("c715cbc80fc44171b43732c3119c9456");
  }

}