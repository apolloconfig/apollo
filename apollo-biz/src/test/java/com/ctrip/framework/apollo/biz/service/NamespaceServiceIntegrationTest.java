package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.Cluster;
import com.ctrip.framework.apollo.biz.entity.Commit;
import com.ctrip.framework.apollo.biz.entity.InstanceConfig;
import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.entity.ReleaseHistory;
import com.ctrip.framework.apollo.biz.repository.InstanceConfigRepository;
import com.ctrip.framework.apollo.common.entity.AppNamespace;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NamespaceServiceIntegrationTest extends AbstractIntegrationTest {


  @Autowired
  private NamespaceService namespaceService;
  @Autowired
  private ItemService itemService;
  @Autowired
  private CommitService commitService;
  @Autowired
  private AppNamespaceService appNamespaceService;
  @Autowired
  private ClusterService clusterService;
  @Autowired
  private ReleaseService releaseService;
  @Autowired
  private ReleaseHistoryService releaseHistoryService;
  @Autowired
  private InstanceConfigRepository instanceConfigRepository;

  private String testApp = "testApp";
  private String testCluster = "default";
  private String testChildCluster = "child-cluster";
  private String testPrivateNamespace = "application";
  private String testUser = "apollo";
  private String commitTestApp = "commitTestApp";


  @Test
  @Sql(scripts = "/sql/namespace-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testDeleteNamespace() {

    Namespace namespace = new Namespace();
    namespace.setAppId(testApp);
    namespace.setClusterName(testCluster);
    namespace.setNamespaceName(testPrivateNamespace);
    namespace.setId(1);

    namespaceService.deleteNamespace(namespace, testUser);

    List<Item> items = itemService.findItemsWithoutOrdered(testApp, testCluster, testPrivateNamespace);
    List<Commit> commits = commitService.find(testApp, testCluster, testPrivateNamespace, PageRequest.of(0, 10));
    AppNamespace appNamespace = appNamespaceService.findOne(testApp, testPrivateNamespace);
    List<Cluster> childClusters = clusterService.findChildClusters(testApp, testCluster);
    InstanceConfig instanceConfig = instanceConfigRepository.findById(1L).orElse(null);
    List<Release> parentNamespaceReleases = releaseService.findActiveReleases(testApp, testCluster,
                                                                              testPrivateNamespace,
                                                                              PageRequest.of(0, 10));
    List<Release> childNamespaceReleases = releaseService.findActiveReleases(testApp, testChildCluster,
                                                                             testPrivateNamespace,
                                                                             PageRequest.of(0, 10));
    Page<ReleaseHistory> releaseHistories =
        releaseHistoryService
            .findReleaseHistoriesByNamespace(testApp, testCluster, testPrivateNamespace, PageRequest.of(0, 10));

    assertEquals(0, items.size());
    assertEquals(0, commits.size());
    assertNotNull(appNamespace);
    assertEquals(0, childClusters.size());
    assertEquals(0, parentNamespaceReleases.size());
    assertEquals(0, childNamespaceReleases.size());
    assertTrue(!releaseHistories.hasContent());
    assertNull(instanceConfig);
  }

  @Test
  @Sql(scripts = "/sql/namespace-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testGetCommitsByModifiedTime() throws ParseException {
    String format = "yyyy-MM-dd HH:mm:ss";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);

    Date lastModifiedTime = simpleDateFormat.parse("2020-08-22 09:00:00");

    List<Commit> commitsByDate = commitService.find(commitTestApp, testCluster, testPrivateNamespace, lastModifiedTime, null);

    Date lastModifiedTimeGreater = simpleDateFormat.parse("2020-08-22 11:00:00");
    List<Commit> commitsByDateGreater = commitService.find(commitTestApp, testCluster, testPrivateNamespace, lastModifiedTimeGreater, null);


    Date lastModifiedTimePage = simpleDateFormat.parse("2020-08-22 09:30:00");
    List<Commit> commitsByDatePage = commitService.find(commitTestApp, testCluster, testPrivateNamespace, lastModifiedTimePage, PageRequest.of(0, 1));

    assertEquals(1, commitsByDate.size());
    assertEquals(0, commitsByDateGreater.size());
    assertEquals(1, commitsByDatePage.size());

  }


}
