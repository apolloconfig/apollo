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
package com.ctrip.framework.apollo.biz.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.ReleaseHistory;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/23
 */
public class ReleaseHistoryRepositoryTest extends AbstractIntegrationTest {

  @Autowired
  private ReleaseHistoryRepository releaseHistoryRepository;

  @Test
  @Sql(scripts = "/sql/release-history-test.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindReleaseHistoryRetentionMaxId() {
    Optional<Long> maxId = releaseHistoryRepository.findReleaseHistoryRetentionMaxId(APP_ID, CLUSTER_NAME, NAMESPACE_NAME, BRANCH_NAME, 1);
    assertEquals(5, maxId.orElse(0L).longValue());

    maxId = releaseHistoryRepository.findReleaseHistoryRetentionMaxId(APP_ID, CLUSTER_NAME, NAMESPACE_NAME, BRANCH_NAME, 2);
    assertEquals(4, maxId.orElse(0L).longValue());

    maxId = releaseHistoryRepository.findReleaseHistoryRetentionMaxId(APP_ID, CLUSTER_NAME, NAMESPACE_NAME, BRANCH_NAME, 5);
    assertEquals(1, maxId.orElse(0L).longValue());

    releaseHistoryRepository.deleteAll();
    maxId = releaseHistoryRepository.findReleaseHistoryRetentionMaxId(APP_ID, CLUSTER_NAME, NAMESPACE_NAME, BRANCH_NAME, 1);
    assertFalse(maxId.isPresent());
  }

  @Test
  @Sql(scripts = "/sql/release-history-test.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindFirst100ByAppIdAndClusterNameAndNamespaceNameAndBranchNameAndIdLessThanEqualOrderByIdAsc() {

    int releaseHistoryRetentionSize = 2;
    Optional<Long> maxId = releaseHistoryRepository.findReleaseHistoryRetentionMaxId(APP_ID, CLUSTER_NAME, NAMESPACE_NAME, BRANCH_NAME, releaseHistoryRetentionSize);
    List<ReleaseHistory> releaseHistories = releaseHistoryRepository.findFirst100ByAppIdAndClusterNameAndNamespaceNameAndBranchNameAndIdLessThanEqualOrderByIdAsc(
        APP_ID, CLUSTER_NAME, NAMESPACE_NAME, BRANCH_NAME, maxId.orElse(0L));
    assertEquals(4, releaseHistories.size());

    releaseHistoryRetentionSize = 1;
    maxId = releaseHistoryRepository.findReleaseHistoryRetentionMaxId(APP_ID, CLUSTER_NAME, NAMESPACE_NAME, BRANCH_NAME, releaseHistoryRetentionSize);
    releaseHistories = releaseHistoryRepository.findFirst100ByAppIdAndClusterNameAndNamespaceNameAndBranchNameAndIdLessThanEqualOrderByIdAsc(
        APP_ID, CLUSTER_NAME, NAMESPACE_NAME, BRANCH_NAME, maxId.orElse(0L));
    assertEquals(5, releaseHistories.size());
  }
}
