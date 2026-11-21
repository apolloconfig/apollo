/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.adminservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ctrip.framework.apollo.AdminServiceTestConfiguration;
import com.ctrip.framework.apollo.biz.entity.GrayReleaseRule;
import com.ctrip.framework.apollo.biz.service.NamespaceBranchService;
import com.ctrip.framework.apollo.common.constants.NamespaceBranchStatus;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.common.utils.GrayReleaseRuleItemTransformer;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConverters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/** Verifies that gray rule updates persist only the resource identified by the request path. */
// The branch check resolves the branch through the Cluster hierarchy (GHSA-hv72), so the
// fixture must include the parent/child Cluster rows, not just the Namespaces.
@SpringBootTest(classes = AdminServiceTestConfiguration.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
@Sql(statements = {
    "INSERT INTO `Cluster` (Id, Name, AppId, ParentClusterId, IsDeleted, DataChange_CreatedBy, DataChange_LastModifiedBy) VALUES (9001, 'default', 'rule-owner', 0, 0, 'operator', 'operator')",
    "INSERT INTO `Cluster` (Id, Name, AppId, ParentClusterId, IsDeleted, DataChange_CreatedBy, DataChange_LastModifiedBy) VALUES (9002, 'owner-branch', 'rule-owner', 9001, 0, 'operator', 'operator')",
    "INSERT INTO `Namespace` (AppId, ClusterName, NamespaceName) VALUES ('rule-owner', 'default', 'application')",
    "INSERT INTO `Namespace` (AppId, ClusterName, NamespaceName) VALUES ('rule-owner', 'owner-branch', 'application')",
    "INSERT INTO `Release` (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations, IsAbandoned) VALUES (7001, 'gray-rule-test', 'branch release', '', 'rule-owner', 'owner-branch', 'application', '{}', false)",
    "INSERT INTO `GrayReleaseRule` (AppId, ClusterName, NamespaceName, BranchName, Rules, ReleaseId, BranchStatus, DataChange_CreatedBy, DataChange_CreatedTime) VALUES ('rule-victim', 'victim-cluster', 'victim-namespace', 'victim-branch', '[]', 8001, 1, 'victim-operator', CURRENT_TIMESTAMP)"})
@Sql(
    statements = {"DELETE FROM `GrayReleaseRule` WHERE AppId IN ('rule-owner', 'rule-victim')",
        "DELETE FROM `ReleaseHistory` WHERE AppId = 'rule-owner'",
        "DELETE FROM `ReleaseMessage` WHERE Message = 'rule-owner+default+application'",
        "DELETE FROM `Release` WHERE AppId = 'rule-owner'",
        "DELETE FROM `Namespace` WHERE AppId = 'rule-owner'",
        "DELETE FROM `Cluster` WHERE AppId = 'rule-owner'"},
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class NamespaceBranchControllerTest {

  private static final String APP_ID = "rule-owner";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";
  private static final String BRANCH = "owner-branch";

  @Autowired
  private HttpMessageConverters converters;
  @Autowired
  private NamespaceBranchService namespaceBranchService;
  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Value("${local.server.port}")
  private int port;

  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    restTemplate.setMessageConverters(converters.getConverters());
  }

  @ParameterizedTest
  @MethodSource("bodyIdentities")
  void shouldPersistPathIdentityAndLeaveOtherApplicationUnchanged(String appId, String clusterName,
      String namespaceName, String branchName) {
    GrayReleaseRuleDTO request =
        new GrayReleaseRuleDTO(appId, clusterName, namespaceName, branchName);
    request.addRuleItem(new GrayReleaseRuleItemDTO("consumer-app", Collections.singleton("*"),
        Collections.singleton("canary")));
    request.setReleaseId(8001L);
    request.setDataChangeCreatedBy("operator");
    request.setDataChangeLastModifiedBy("operator");

    restTemplate.put(rulesUrl(), request, APP_ID, CLUSTER, NAMESPACE, BRANCH);

    assertVictimUnchanged();
    GrayReleaseRule persisted =
        namespaceBranchService.findBranchGrayRules(APP_ID, CLUSTER, NAMESPACE, BRANCH);
    assertNotNull(persisted);
    assertEquals(APP_ID, persisted.getAppId());
    assertEquals(CLUSTER, persisted.getClusterName());
    assertEquals(NAMESPACE, persisted.getNamespaceName());
    assertEquals(BRANCH, persisted.getBranchName());
    assertEquals(7001L, persisted.getReleaseId());
    assertEquals(NamespaceBranchStatus.ACTIVE, persisted.getBranchStatus());
    assertEquals("operator", persisted.getDataChangeLastModifiedBy());
    assertEquals(GrayReleaseRuleItemTransformer.batchTransformToJSON(request.getRuleItems()),
        persisted.getRules());
    assertEquals(1, jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM `GrayReleaseRule` WHERE AppId = 'rule-owner'", Integer.class));
    assertEquals(1, jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM `ReleaseMessage` WHERE Message = 'rule-owner+default+application'",
        Integer.class));
  }

  @Test
  void shouldRejectMissingPathBranchWithoutWritingRules() {
    GrayReleaseRuleDTO request = new GrayReleaseRuleDTO(APP_ID, CLUSTER, NAMESPACE, BRANCH);
    HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
        () -> restTemplate.put(rulesUrl(), request, APP_ID, CLUSTER, NAMESPACE, "missing-branch"));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals(0, jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM `GrayReleaseRule` WHERE AppId = 'rule-owner'", Integer.class));
    assertVictimUnchanged();
  }

  private void assertVictimUnchanged() {
    assertEquals(1, jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM `GrayReleaseRule` WHERE AppId = 'rule-victim'", Integer.class));
    GrayReleaseRule victim = namespaceBranchService.findBranchGrayRules("rule-victim",
        "victim-cluster", "victim-namespace", "victim-branch");
    assertNotNull(victim);
    assertEquals(8001L, victim.getReleaseId());
    assertEquals("[]", victim.getRules());
  }

  private String rulesUrl() {
    return "http://localhost:" + port
        + "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/rules";
  }

  static Stream<Arguments> bodyIdentities() {
    return Stream.of(Arguments.of("rule-victim", CLUSTER, NAMESPACE, BRANCH),
        Arguments.of(APP_ID, "victim-cluster", NAMESPACE, BRANCH),
        Arguments.of(APP_ID, CLUSTER, "victim-namespace", BRANCH),
        Arguments.of(APP_ID, CLUSTER, NAMESPACE, "victim-branch"),
        Arguments.of("rule-victim", "victim-cluster", "victim-namespace", "victim-branch"),
        Arguments.of(null, null, null, null), Arguments.of(APP_ID, CLUSTER, NAMESPACE, BRANCH));
  }
}
