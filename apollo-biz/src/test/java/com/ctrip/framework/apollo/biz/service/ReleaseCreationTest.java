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

import com.google.gson.Gson;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.GrayReleaseRule;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.entity.ReleaseHistory;
import com.ctrip.framework.apollo.common.constants.GsonType;
import com.ctrip.framework.apollo.common.constants.ReleaseOperation;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

public class ReleaseCreationTest extends AbstractIntegrationTest {

  private static final Gson GSON = new Gson();

  @Autowired
  private ReleaseService releaseService;
  @Autowired
  private NamespaceBranchService namespaceBranchService;
  @Autowired
  private ReleaseHistoryService releaseHistoryService;

  private String testApp = "test";
  private String testNamespace = "application";
  private String operator = "apollo";
  private Pageable pageable = PageRequest.of(0, 10);

  @Test
  @Sql(scripts = "/sql/release-creation-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPublishNormalNamespace() {
    long namespaceId = 100;
    String clusterName = "only-master";
    Namespace namespace = instanceNamespace(namespaceId, clusterName);

    releaseService.publish(namespace, "", "", operator, false);

    Release latestRelease = releaseService.findLatestActiveRelease(namespace);

    Assert.assertNotNull(latestRelease);

    Map<String, String> configuration = parseConfiguration(latestRelease.getConfigurations());
    Assert.assertEquals(3, configuration.size());
    Assert.assertEquals("v1", configuration.get("k1"));
    Assert.assertEquals("v2", configuration.get("k2"));
    Assert.assertEquals("v3", configuration.get("k3"));

    Page<ReleaseHistory> releaseHistories = releaseHistoryService.findReleaseHistoriesByNamespace
        (testApp, clusterName, testNamespace, pageable);

    ReleaseHistory releaseHistory = releaseHistories.getContent().get(0);

    Assert.assertEquals(1, releaseHistories.getTotalElements());
    Assert.assertEquals(ReleaseOperation.NORMAL_RELEASE, releaseHistory.getOperation());
    Assert.assertEquals(latestRelease.getId(), releaseHistory.getReleaseId());
    Assert.assertEquals(0, releaseHistory.getPreviousReleaseId());
  }


  /**
   *               Master     |      Branch
   *           ------------------------------                                      Master    |    Branch
   *     Items      k1=v1     |                                                 ----------------------------
   *                k2=v2     |                                                     k1=v1    |    k1=v1
   *                k3=v3                            publish master                 k2=v2    |    k2=v2
   *           ------------------------------        ===========>>      Result      k3=v3    |    k3=v3
   *    Release               |
   *                          |
   *                          |
   */
  @Test
  @Sql(scripts = "/sql/release-creation-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPublishMasterNamespaceAndBranchHasNotItems() {
    long parentNamespaceId = 101;
    String parentClusterName = "default1";
    long childNamespaceId = 102;
    String childClusterName = "child-cluster1";
    Namespace parentNamespace = instanceNamespace(parentNamespaceId, parentClusterName);

    releaseService.publish(parentNamespace, "", "", operator, false);

    Release latestParentNamespaceRelease = releaseService.findLatestActiveRelease(parentNamespace);

    //assert parent namespace
    Assert.assertNotNull(latestParentNamespaceRelease);

    Map<String, String> parentNamespaceConfiguration = parseConfiguration(latestParentNamespaceRelease.getConfigurations());
    Assert.assertEquals(3, parentNamespaceConfiguration.size());
    Assert.assertEquals("v1", parentNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2", parentNamespaceConfiguration.get("k2"));
    Assert.assertEquals("v3", parentNamespaceConfiguration.get("k3"));

    //assert child namespace
    Namespace childNamespace = instanceNamespace(childNamespaceId, childClusterName);
    Release latestChildNamespaceRelease = releaseService.findLatestActiveRelease(childNamespace);

    //assert parent namespace
    Assert.assertNotNull(latestChildNamespaceRelease);

    Map<String, String> childNamespaceConfiguration = parseConfiguration(latestChildNamespaceRelease.getConfigurations());
    Assert.assertEquals(3, childNamespaceConfiguration.size());
    Assert.assertEquals("v1", childNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2", childNamespaceConfiguration.get("k2"));
    Assert.assertEquals("v3", childNamespaceConfiguration.get("k3"));

    GrayReleaseRule rule= namespaceBranchService.findBranchGrayRules(testApp, parentClusterName,
                                                                     testNamespace, childClusterName);
    Assert.assertNotNull(rule);
    Assert.assertEquals(1, rule.getBranchStatus());
    Assert.assertEquals(Long.valueOf(latestChildNamespaceRelease.getId()), rule.getReleaseId());

    //assert release history
    Page<ReleaseHistory> releaseHistories = releaseHistoryService.findReleaseHistoriesByNamespace
        (testApp, parentClusterName, testNamespace, pageable);

    ReleaseHistory masterReleaseHistory = releaseHistories.getContent().get(1);
    ReleaseHistory branchReleaseHistory = releaseHistories.getContent().get(0);

    Assert.assertEquals(2, releaseHistories.getTotalElements());
    Assert.assertEquals(ReleaseOperation.NORMAL_RELEASE, masterReleaseHistory.getOperation());
    Assert.assertEquals(latestParentNamespaceRelease.getId(), masterReleaseHistory.getReleaseId());
    Assert.assertEquals(0, masterReleaseHistory.getPreviousReleaseId());
    Assert.assertEquals(ReleaseOperation.MASTER_NORMAL_RELEASE_MERGE_TO_GRAY,
        branchReleaseHistory.getOperation());
    Assert.assertEquals(latestChildNamespaceRelease.getId(), branchReleaseHistory.getReleaseId());
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains(String.format
        ("\"baseReleaseId\":%d", latestParentNamespaceRelease.getId())));
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains(rule.getRules()));

  }


  /**
   *               Master     |      Branch
   *           ------------------------------                                      Master    |    Branch
   *     Items      k1=v1     |      k1=v1-2                                      -------------------------
   *                k2=v2     |                                                     k1=v1    |    k1=v1
   *                k3=v3                            publish master                 k2=v2    |    k2=v2
   *           ------------------------------        ===========>>      Result      k3=v3    |    k3=v3
   *    Release               |
   *                          |
   *                          |
   */
  @Test
  @Sql(scripts = "/sql/release-creation-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPublishMasterNamespaceAndBranchHasItems() {
    long parentNamespaceId = 103;
    String parentClusterName = "default2";
    long childNamespaceId = 104;
    String childClusterName = "child-cluster2";
    Namespace parentNamespace = instanceNamespace(parentNamespaceId, parentClusterName);

    releaseService.publish(parentNamespace, "", "", operator, false);

    Release latestParentNamespaceRelease = releaseService.findLatestActiveRelease(parentNamespace);

    //assert parent namespace
    Assert.assertNotNull(latestParentNamespaceRelease);

    Map<String, String> parentNamespaceConfiguration = parseConfiguration(latestParentNamespaceRelease.getConfigurations());
    Assert.assertEquals(3, parentNamespaceConfiguration.size());
    Assert.assertEquals("v1", parentNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2", parentNamespaceConfiguration.get("k2"));
    Assert.assertEquals("v3", parentNamespaceConfiguration.get("k3"));

    //assert child namespace
    Namespace childNamespace = instanceNamespace(childNamespaceId, childClusterName);
    Release latestChildNamespaceRelease = releaseService.findLatestActiveRelease(childNamespace);

    Assert.assertNotNull(latestChildNamespaceRelease);

    Map<String, String> childNamespaceConfiguration = parseConfiguration(latestChildNamespaceRelease.getConfigurations());
    Assert.assertEquals(3, childNamespaceConfiguration.size());
    Assert.assertEquals("v1", childNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2", childNamespaceConfiguration.get("k2"));
    Assert.assertEquals("v3", childNamespaceConfiguration.get("k3"));

    GrayReleaseRule rule= namespaceBranchService.findBranchGrayRules(testApp, parentClusterName,
                                                                     testNamespace, childClusterName);
    Assert.assertNotNull(rule);
    Assert.assertEquals(1, rule.getBranchStatus());
    Assert.assertEquals(Long.valueOf(latestChildNamespaceRelease.getId()), rule.getReleaseId());

    //assert release history
    Page<ReleaseHistory> releaseHistories = releaseHistoryService.findReleaseHistoriesByNamespace
        (testApp, parentClusterName, testNamespace, pageable);

    ReleaseHistory masterReleaseHistory = releaseHistories.getContent().get(1);
    ReleaseHistory branchReleaseHistory = releaseHistories.getContent().get(0);

    Assert.assertEquals(2, releaseHistories.getTotalElements());
    Assert.assertEquals(ReleaseOperation.NORMAL_RELEASE, masterReleaseHistory.getOperation());
    Assert.assertEquals(latestParentNamespaceRelease.getId(), masterReleaseHistory.getReleaseId());
    Assert.assertEquals(0, masterReleaseHistory.getPreviousReleaseId());
    Assert.assertEquals(ReleaseOperation.MASTER_NORMAL_RELEASE_MERGE_TO_GRAY,
        branchReleaseHistory.getOperation());
    Assert.assertEquals(latestChildNamespaceRelease.getId(), branchReleaseHistory.getReleaseId());
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains(String.format
        ("\"baseReleaseId\":%d", latestParentNamespaceRelease.getId())));
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains(rule.getRules()));
  }


  /**
   *               Master     |      Branch
   *           ------------------------------                                      Master    |    Branch
   *     Items      k1=v1     |      k1=v1-2                                    ----------------------------
   *               k2=v2-2    |                      publish master                 k1=v1    |    k1=v1-1
   *           ------------------------------        ===========>>      Result      k2=v2-2  |    k2=v2-2
   *    Release     k1=v1     |      k1=v1-1                                                 |
   *                k2=v2     |      k2=v3
   *                k3=v3     |      k3=v3
   */
  @Test
  @Sql(scripts = "/sql/release-creation-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testModifyMasterNamespaceItemsAndBranchAlsoModify() {
    long parentNamespaceId = 105;
    String parentClusterName = "default3";
    long childNamespaceId = 106;
    String childClusterName = "child-cluster3";
    Namespace parentNamespace = instanceNamespace(parentNamespaceId, parentClusterName);

    releaseService.publish(parentNamespace, "", "", operator, false);

    Release latestParentNamespaceRelease = releaseService.findLatestActiveRelease(parentNamespace);

    //assert parent namespace
    Assert.assertNotNull(latestParentNamespaceRelease);

    Map<String, String> parentNamespaceConfiguration = parseConfiguration(latestParentNamespaceRelease.getConfigurations());
    Assert.assertEquals(2, parentNamespaceConfiguration.size());
    Assert.assertEquals("v1", parentNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2-2", parentNamespaceConfiguration.get("k2"));

    //assert child namespace
    Namespace childNamespace = instanceNamespace(childNamespaceId, childClusterName);
    Release latestChildNamespaceRelease = releaseService.findLatestActiveRelease(childNamespace);

    Assert.assertNotNull(latestChildNamespaceRelease);

    Map<String, String> childNamespaceConfiguration = parseConfiguration(latestChildNamespaceRelease.getConfigurations());
    Assert.assertEquals(2, childNamespaceConfiguration.size());
    Assert.assertEquals("v1-1", childNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2-2", childNamespaceConfiguration.get("k2"));

    GrayReleaseRule rule= namespaceBranchService.findBranchGrayRules(testApp, parentClusterName,
                                                                     testNamespace, childClusterName);
    Assert.assertNotNull(rule);
    Assert.assertEquals(1, rule.getBranchStatus());
    Assert.assertEquals(Long.valueOf(latestChildNamespaceRelease.getId()), rule.getReleaseId());

    //assert release history
    Page<ReleaseHistory> releaseHistories = releaseHistoryService.findReleaseHistoriesByNamespace
        (testApp, parentClusterName, testNamespace, pageable);

    ReleaseHistory masterReleaseHistory = releaseHistories.getContent().get(1);
    ReleaseHistory branchReleaseHistory = releaseHistories.getContent().get(0);

    Assert.assertEquals(2, releaseHistories.getTotalElements());
    Assert.assertEquals(ReleaseOperation.NORMAL_RELEASE, masterReleaseHistory.getOperation());
    Assert.assertEquals(latestParentNamespaceRelease.getId(), masterReleaseHistory.getReleaseId());
    Assert.assertEquals(1, masterReleaseHistory.getPreviousReleaseId());
    Assert.assertEquals(ReleaseOperation.MASTER_NORMAL_RELEASE_MERGE_TO_GRAY,
        branchReleaseHistory.getOperation());
    Assert.assertEquals(latestChildNamespaceRelease.getId(), branchReleaseHistory.getReleaseId());
    Assert.assertEquals(2, branchReleaseHistory.getPreviousReleaseId());
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains(String.format
        ("\"baseReleaseId\":%d", latestParentNamespaceRelease.getId())));
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains(rule.getRules()));
  }

  /**
   *               Master     |      Branch
   *           ------------------------------                                      Master    |    Branch
   *     Items      k1=v1     |      k1=v1-2                                    ----------------------------
   *               k2=v2-2    |      k4=v4           publish branch                 k1=v1    |    k1=v1-2
   *           ------------------------------        ===========>>      Result      k2=v2    |    k2=v2
   *    Release     k1=v1     |                                                     k3=v3    |    k3=v3
   *                k2=v2     |                                                              |    k4=v4
   *                k3=v3     |
   */
  @Test
  @Sql(scripts = "/sql/release-creation-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPublishBranchAtFirstTime() {
    long parentNamespaceId = 107;
    String parentClusterName = "default4";
    long childNamespaceId = 108;
    String childClusterName = "child-cluster4";
    //assert child namespace
    Namespace childNamespace = instanceNamespace(childNamespaceId, childClusterName);
    releaseService.publish(childNamespace, "", "", operator, false);

    Release latestChildNamespaceRelease = releaseService.findLatestActiveRelease(childNamespace);

    Assert.assertNotNull(latestChildNamespaceRelease);

    Map<String, String> childNamespaceConfiguration = parseConfiguration(latestChildNamespaceRelease.getConfigurations());
    Assert.assertEquals(4, childNamespaceConfiguration.size());
    Assert.assertEquals("v1-2", childNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2", childNamespaceConfiguration.get("k2"));
    Assert.assertEquals("v3", childNamespaceConfiguration.get("k3"));
    Assert.assertEquals("v4", childNamespaceConfiguration.get("k4"));

    Namespace parentNamespace = instanceNamespace(parentNamespaceId, parentClusterName);

    Release latestParentNamespaceRelease = releaseService.findLatestActiveRelease(parentNamespace);

    //assert parent namespace
    Assert.assertNotNull(latestParentNamespaceRelease);

    Map<String, String> parentNamespaceConfiguration = parseConfiguration(latestParentNamespaceRelease.getConfigurations());
    Assert.assertEquals(3, parentNamespaceConfiguration.size());
    Assert.assertEquals("v1", parentNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2", parentNamespaceConfiguration.get("k2"));
    Assert.assertEquals("v3", parentNamespaceConfiguration.get("k3"));

    GrayReleaseRule rule= namespaceBranchService.findBranchGrayRules(testApp, parentClusterName,
                                                                     testNamespace, childClusterName);
    Assert.assertNotNull(rule);
    Assert.assertEquals(1, rule.getBranchStatus());
    Assert.assertEquals(Long.valueOf(latestChildNamespaceRelease.getId()), rule.getReleaseId());

    //assert release history
    Page<ReleaseHistory> releaseHistories = releaseHistoryService.findReleaseHistoriesByNamespace
        (testApp, parentClusterName, testNamespace, pageable);

    ReleaseHistory branchReleaseHistory = releaseHistories.getContent().get(0);

    Assert.assertEquals(1, releaseHistories.getTotalElements());
    Assert.assertEquals(ReleaseOperation.GRAY_RELEASE,
        branchReleaseHistory.getOperation());
    Assert.assertEquals(latestChildNamespaceRelease.getId(), branchReleaseHistory.getReleaseId());
    Assert.assertEquals(0, branchReleaseHistory.getPreviousReleaseId());
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains("\"baseReleaseId\":3"));
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains(rule.getRules()));
  }


  /**
   *               Master     |      Branch
   *           ------------------------------                                      Master    |    Branch
   *     Items      k1=v1     |      k1=v1-2                                    ----------------------------
   *               k2=v2-2    |      k4=v4                                          k1=v1    |    k1=v1-2
   *                                 k6=v6           publish branch                 k2=v2    |    k2=v2
   *           ------------------------------        ===========>>      Result      k3=v3    |    k3=v3
   *    Release     k1=v1     |      k1=v1-1                                                 |    k4=v4
   *                k2=v2     |      k2=v2                                                   |    k6=v6
   *                k3=v3     |      k3=v3
   *                          |      k4=v4
   *                          |      k5=v5
   */
  @Test
  @Sql(scripts = "/sql/release-creation-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPublishBranch() {
    long parentNamespaceId = 109;
    String parentClusterName = "default5";
    long childNamespaceId = 1010;
    String childClusterName = "child-cluster5";
    //assert child namespace
    Namespace childNamespace = instanceNamespace(childNamespaceId, childClusterName);
    releaseService.publish(childNamespace, "", "", operator, false);

    Release latestChildNamespaceRelease = releaseService.findLatestActiveRelease(childNamespace);

    Assert.assertNotNull(latestChildNamespaceRelease);

    Map<String, String> childNamespaceConfiguration = parseConfiguration(latestChildNamespaceRelease.getConfigurations());
    Assert.assertEquals(5, childNamespaceConfiguration.size());
    Assert.assertEquals("v1-2", childNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2", childNamespaceConfiguration.get("k2"));
    Assert.assertEquals("v3", childNamespaceConfiguration.get("k3"));
    Assert.assertEquals("v4", childNamespaceConfiguration.get("k4"));
    Assert.assertEquals("v6", childNamespaceConfiguration.get("k6"));

    Namespace parentNamespace = instanceNamespace(parentNamespaceId, parentClusterName);

    Release latestParentNamespaceRelease = releaseService.findLatestActiveRelease(parentNamespace);

    //assert parent namespace
    Assert.assertNotNull(latestParentNamespaceRelease);

    Map<String, String> parentNamespaceConfiguration = parseConfiguration(latestParentNamespaceRelease.getConfigurations());
    Assert.assertEquals(3, parentNamespaceConfiguration.size());
    Assert.assertEquals("v1", parentNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2", parentNamespaceConfiguration.get("k2"));
    Assert.assertEquals("v3", parentNamespaceConfiguration.get("k3"));

    GrayReleaseRule rule= namespaceBranchService.findBranchGrayRules(testApp, parentClusterName,
                                                                     testNamespace, childClusterName);
    Assert.assertNotNull(rule);
    Assert.assertEquals(1, rule.getBranchStatus());
    Assert.assertEquals(Long.valueOf(latestChildNamespaceRelease.getId()), rule.getReleaseId());

    //assert release history
    Page<ReleaseHistory> releaseHistories = releaseHistoryService.findReleaseHistoriesByNamespace
        (testApp, parentClusterName, testNamespace, pageable);

    ReleaseHistory branchReleaseHistory = releaseHistories.getContent().get(0);

    Assert.assertEquals(1, releaseHistories.getTotalElements());
    Assert.assertEquals(ReleaseOperation.GRAY_RELEASE,
        branchReleaseHistory.getOperation());
    Assert.assertEquals(latestChildNamespaceRelease.getId(), branchReleaseHistory.getReleaseId());
    Assert.assertEquals(5, branchReleaseHistory.getPreviousReleaseId());
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains("\"baseReleaseId\":4"));
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains(rule.getRules()));
  }

  /**
   *                          Master     |      Branch
   *                       ------------------------------                                      Master   |    Branch
   *     Rollback Release      k1=v1     |      k1=v1-2                                    ----------------------------
   *                           k2=v2     |      k2=v2                                        k1=v1-1    |    k1=v1-2
   *                                     |      k3=v3                                        k2=v2-1    |    k2=v2-1
   *                                                            rollback                     k3=v3      |    k3=v3
   *                       ------------------------------     ===========>>   New Release
   *    New  Release           k1=v1-1   |
   *                           k2=v2-1   |
   *                           k3=v3     |
   *
   *
   */
  @Test
  @Sql(scripts = "/sql/release-creation-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testRollback() {

    String parentClusterName = "default6";
    String childClusterName = "child-cluster6";
    String operator = "apollo";

    Release parentNamespaceLatestRelease = releaseService.findLatestActiveRelease(testApp, parentClusterName, testNamespace);
    releaseService.rollback(parentNamespaceLatestRelease.getId(), operator);

    Release parentNamespaceNewLatestRelease = releaseService.findLatestActiveRelease(testApp, parentClusterName, testNamespace);
    Map<String, String> parentNamespaceConfiguration = parseConfiguration(parentNamespaceNewLatestRelease.getConfigurations());
    Assert.assertEquals(3, parentNamespaceConfiguration.size());
    Assert.assertEquals("v1-1", parentNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2-1", parentNamespaceConfiguration.get("k2"));
    Assert.assertEquals("v3", parentNamespaceConfiguration.get("k3"));

    Release childNamespaceNewLatestRelease = releaseService.findLatestActiveRelease(testApp, childClusterName, testNamespace);
    Map<String, String> childNamespaceConfiguration = parseConfiguration(childNamespaceNewLatestRelease.getConfigurations());
    Assert.assertEquals(3, childNamespaceConfiguration.size());
    Assert.assertEquals("v1-2", childNamespaceConfiguration.get("k1"));
    Assert.assertEquals("v2-1", childNamespaceConfiguration.get("k2"));
    Assert.assertEquals("v3", childNamespaceConfiguration.get("k3"));

    //assert release history
    Page<ReleaseHistory> releaseHistories = releaseHistoryService.findReleaseHistoriesByNamespace
        (testApp, parentClusterName, testNamespace, pageable);

    ReleaseHistory masterReleaseHistory = releaseHistories.getContent().get(1);
    ReleaseHistory branchReleaseHistory = releaseHistories.getContent().get(0);

    Assert.assertEquals(2, releaseHistories.getTotalElements());
    Assert.assertEquals(ReleaseOperation.ROLLBACK, masterReleaseHistory.getOperation());
    Assert.assertEquals(6, masterReleaseHistory.getReleaseId());
    Assert.assertEquals(7, masterReleaseHistory.getPreviousReleaseId());
    Assert.assertEquals(ReleaseOperation.MATER_ROLLBACK_MERGE_TO_GRAY,
        branchReleaseHistory.getOperation());
    Assert.assertEquals(childNamespaceNewLatestRelease.getId(), branchReleaseHistory.getReleaseId());
    Assert.assertEquals(8, branchReleaseHistory.getPreviousReleaseId());
    Assert.assertTrue(branchReleaseHistory.getOperationContext().contains(String.format
        ("\"baseReleaseId\":%d", parentNamespaceNewLatestRelease.getId())));
  }


  private Namespace instanceNamespace(long id, String clusterName) {
    Namespace namespace = new Namespace();
    namespace.setAppId(testApp);
    namespace.setNamespaceName(testNamespace);
    namespace.setId(id);
    namespace.setClusterName(clusterName);
    return namespace;
  }

  private Map<String, String> parseConfiguration(String configuration) {
    return GSON.fromJson(configuration, GsonType.CONFIG);
  }

}
