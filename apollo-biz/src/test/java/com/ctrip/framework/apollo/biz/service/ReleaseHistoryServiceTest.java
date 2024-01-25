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
package com.ctrip.framework.apollo.biz.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.biz.BizTestConfiguration;
import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.entity.ReleaseHistory;
import com.ctrip.framework.apollo.biz.repository.ReleaseHistoryRepository;
import com.ctrip.framework.apollo.biz.repository.ReleaseRepository;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.lang.reflect.Method;
import java.sql.SQLException;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/24
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = BizTestConfiguration.class,
    webEnvironment = WebEnvironment.RANDOM_PORT
)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ReleaseHistoryServiceTest {

  @Mock
  private BizConfig bizConfig;
  @Mock
  private ReleaseRepository mockReleaseRepository;

  private ReleaseHistory mockReleaseHistory;
  private static final String APP_ID = "kl-app";
  private static final String CLUSTER_NAME = "default";
  private static final String NAMESPACE_NAME = "application";
  private static final String BRANCH_NAME = "default";

  @Autowired
  private ReleaseHistoryService releaseHistoryService;
  @Autowired
  private ReleaseHistoryRepository releaseHistoryRepository;
  @Autowired
  private ReleaseRepository releaseRepository;

  @Before
  public void setUp() throws Exception {
    ReflectionTestUtils.setField(releaseHistoryService, "bizConfig", bizConfig);
    mockReleaseHistory = spy(ReleaseHistory.class);
    mockReleaseHistory.setBranchName(BRANCH_NAME);
    mockReleaseHistory.setNamespaceName(NAMESPACE_NAME);
    mockReleaseHistory.setClusterName(CLUSTER_NAME);
    mockReleaseHistory.setAppId(APP_ID);
  }

  @Test
  @Sql(scripts = "/sql/release-history-test.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testCleanReleaseHistory() {
    ReleaseHistoryService service = (ReleaseHistoryService) AopProxyUtils.getSingletonTarget(releaseHistoryService);
    assert service != null;
    Method method = ReflectionUtils.findMethod(service.getClass(), "cleanReleaseHistory", ReleaseHistory.class);
    assert method != null;
    ReflectionUtils.makeAccessible(method);

    when(bizConfig.releaseHistoryRetentionSize()).thenReturn(-1);
    when(bizConfig.releaseHistoryRetentionSizeOverride()).thenReturn(Maps.newHashMap());
    ReflectionUtils.invokeMethod(method, service, mockReleaseHistory);
    Assert.assertEquals(6, releaseHistoryRepository.count());
    Assert.assertEquals(6, releaseRepository.count());

    when(bizConfig.releaseHistoryRetentionSize()).thenReturn(2);
    when(bizConfig.releaseHistoryRetentionSizeOverride()).thenReturn(Maps.newHashMap());
    ReflectionUtils.invokeMethod(method, service, mockReleaseHistory);
    Assert.assertEquals(2, releaseHistoryRepository.count());
    Assert.assertEquals(2, releaseRepository.count());

    when(bizConfig.releaseHistoryRetentionSize()).thenReturn(2);
    when(bizConfig.releaseHistoryRetentionSizeOverride()).thenReturn(
        ImmutableMap.of("kl-app+default+application+default", 1));
    ReflectionUtils.invokeMethod(method, service, mockReleaseHistory);
    Assert.assertEquals(1, releaseHistoryRepository.count());
    Assert.assertEquals(1, releaseRepository.count());

    Iterable<ReleaseHistory> historyList = releaseHistoryRepository.findAll();
    historyList.forEach(history -> Assert.assertEquals(6, history.getId()));

    Iterable<Release> releaseList = releaseRepository.findAll();
    releaseList.forEach(release -> Assert.assertEquals(6, release.getId()));
  }

  @Test
  @Sql(scripts = "/sql/release-history-test.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testCleanReleaseHistoryTransactionalRollBack() {
    ReleaseHistoryService service = (ReleaseHistoryService) AopProxyUtils.getSingletonTarget(releaseHistoryService);
    assert service != null;
    Method method = ReflectionUtils.findMethod(service.getClass(), "cleanReleaseHistory", ReleaseHistory.class);
    assert method != null;
    ReflectionUtils.makeAccessible(method);

    when(bizConfig.releaseHistoryRetentionSize()).thenReturn(1);
    when(bizConfig.releaseHistoryRetentionSizeOverride()).thenReturn(Maps.newHashMap());
    ReflectionTestUtils.setField(releaseHistoryService, "releaseRepository", mockReleaseRepository);
    doThrow(new JDBCConnectionException("error", new SQLException("sql"))).when(mockReleaseRepository).deleteAllById(any());
    Assert.assertThrows(JDBCConnectionException.class, () ->
        ReflectionUtils.invokeMethod(method, service, mockReleaseHistory));

    Assert.assertEquals(6, releaseHistoryRepository.count());

    ReflectionTestUtils.setField(releaseHistoryService, "releaseRepository", releaseRepository);
    Assert.assertEquals(6, releaseRepository.count());

    ReflectionUtils.invokeMethod(method, service, mockReleaseHistory);
    Assert.assertEquals(1, releaseHistoryRepository.count());
    Assert.assertEquals(1, releaseRepository.count());
  }

}
