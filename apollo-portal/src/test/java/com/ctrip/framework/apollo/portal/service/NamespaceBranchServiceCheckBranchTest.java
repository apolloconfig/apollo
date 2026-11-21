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
package com.ctrip.framework.apollo.portal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.ItemsComparator;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the gray-branch binding check (GHSA-hv72): a branchName that is not the
 * gray branch of the parent cluster must be rejected before any delete, publish
 * or merge targets it.
 */
@ExtendWith(MockitoExtension.class)
class NamespaceBranchServiceCheckBranchTest {

  private static final String APP_ID = "sample-app";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";
  private static final String BRANCH = "gray-release";

  @Mock
  private ItemsComparator itemsComparator;

  @Mock
  private NamespaceService namespaceService;

  @Mock
  private ItemService itemService;

  @Mock
  private AdminServiceAPI.NamespaceBranchAPI namespaceBranchAPI;

  @Mock
  private ReleaseService releaseService;

  @InjectMocks
  private NamespaceBranchService namespaceBranchService;

  @Test
  void checkBranchShouldAcceptRealBranch() {
    when(namespaceBranchAPI.findBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE))
        .thenReturn(namespace(BRANCH));

    namespaceBranchService.checkBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE, BRANCH);
  }

  @Test
  void checkBranchShouldRejectOtherClusterAsBranchName() {
    // GHSA-hv72: `default` (or any other real cluster) is not a gray branch.
    when(namespaceBranchAPI.findBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE))
        .thenReturn(namespace(BRANCH));

    assertThrows(BadRequestException.class, () -> namespaceBranchService
        .checkBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE, CLUSTER));
  }

  @Test
  void checkBranchShouldRejectWhenNoBranchExists() {
    when(namespaceBranchAPI.findBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE)).thenReturn(null);

    assertThrows(BadRequestException.class, () -> namespaceBranchService
        .checkBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE, BRANCH));
  }

  @Test
  void deleteBranchShouldDeleteRealBranch() {
    when(namespaceBranchAPI.findBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE))
        .thenReturn(namespace(BRANCH));

    namespaceBranchService.deleteBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE, BRANCH, "operator");

    verify(namespaceBranchAPI).deleteBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE, BRANCH,
        "operator");
  }

  @Test
  void deleteBranchShouldRejectNonBranchCluster() {
    // GHSA-hv72: deleting via branchName=`default` must not delete the default cluster.
    when(namespaceBranchAPI.findBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE))
        .thenReturn(namespace(BRANCH));

    assertThrows(BadRequestException.class, () -> namespaceBranchService
        .deleteBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE, CLUSTER, "operator"));

    verify(namespaceBranchAPI, never()).deleteBranch(any(), any(), any(), any(), any(), any());
  }

  @Test
  void mergeShouldMergeRealBranch() {
    when(namespaceBranchAPI.findBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE))
        .thenReturn(namespace(BRANCH));
    when(namespaceService.loadNamespaceBO(APP_ID, Env.DEV, CLUSTER, NAMESPACE))
        .thenReturn(parentNamespace());
    when(itemService.findItems(APP_ID, Env.DEV, CLUSTER, NAMESPACE))
        .thenReturn(Collections.emptyList());
    when(itemService.findItems(APP_ID, Env.DEV, BRANCH, NAMESPACE))
        .thenReturn(Collections.emptyList());
    when(itemsComparator.compareIgnoreBlankAndCommentItem(anyLong(), any(), any()))
        .thenReturn(new ItemChangeSets());
    when(releaseService.updateAndPublish(any(), any(), any(), any(), any(), any(), any(),
        anyBoolean(), anyBoolean(), any())).thenReturn(release(1L));

    ReleaseDTO result = namespaceBranchService.merge(APP_ID, Env.DEV, CLUSTER, NAMESPACE, BRANCH,
        "merge title", "merge comment", false, false, "operator");

    assertEquals(1L, result.getId());
    verify(releaseService).updateAndPublish(eq(APP_ID), eq(Env.DEV), eq(CLUSTER), eq(NAMESPACE),
        eq("merge title"), eq("merge comment"), eq(BRANCH), eq(false), eq(false), any());
  }

  @Test
  void mergeShouldRejectNonBranchCluster() {
    // GHSA-hv72: merging via branchName=`default` must not publish the default
    // cluster's items into the parent namespace. With deleteBranch=false the biz
    // deleteBranch guard never runs, so this check is the only defence.
    when(namespaceBranchAPI.findBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE))
        .thenReturn(namespace(BRANCH));

    assertThrows(BadRequestException.class, () -> namespaceBranchService.merge(APP_ID, Env.DEV,
        CLUSTER, NAMESPACE, CLUSTER, "merge title", "merge comment", false, false, "operator"));

    verify(namespaceService, never()).loadNamespaceBO(any(), any(), any(), any());
    verify(releaseService, never()).updateAndPublish(any(), any(), any(), any(), any(), any(),
        any(), anyBoolean(), anyBoolean(), any());
  }

  private static NamespaceDTO namespace(String clusterName) {
    NamespaceDTO namespace = new NamespaceDTO();
    namespace.setAppId(APP_ID);
    namespace.setClusterName(clusterName);
    namespace.setNamespaceName(NAMESPACE);
    return namespace;
  }

  private static NamespaceBO parentNamespace() {
    NamespaceBO parentNamespace = new NamespaceBO();
    NamespaceDTO baseInfo = namespace(CLUSTER);
    baseInfo.setId(1L);
    parentNamespace.setBaseInfo(baseInfo);
    parentNamespace.setItemModifiedCnt(0);
    return parentNamespace;
  }

  private static ReleaseDTO release(long id) {
    ReleaseDTO release = new ReleaseDTO();
    release.setId(id);
    return release;
  }
}
