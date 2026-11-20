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

import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.message.MessageSender;
import com.ctrip.framework.apollo.biz.service.NamespaceBranchService;
import com.ctrip.framework.apollo.biz.service.NamespaceService;
import com.ctrip.framework.apollo.common.constants.NamespaceBranchStatus;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the gray-branch binding check in checkBranch (GHSA-hv72): deleteBranch
 * must only delete the gray branch of the parent cluster, never an arbitrary
 * cluster passed as branchName.
 */
@RunWith(MockitoJUnitRunner.class)
public class NamespaceBranchControllerCheckBranchTest {

  private static final String APP_ID = "sample-app";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";
  private static final String BRANCH = "gray-release";

  @Mock
  private MessageSender messageSender;

  @Mock
  private NamespaceBranchService namespaceBranchService;

  @Mock
  private NamespaceService namespaceService;

  @InjectMocks
  private NamespaceBranchController namespaceBranchController;

  @Test
  public void deleteBranchShouldDeleteRealBranch() {
    when(namespaceService.findOne(APP_ID, CLUSTER, NAMESPACE)).thenReturn(namespace(CLUSTER));
    when(namespaceBranchService.findBranch(APP_ID, CLUSTER, NAMESPACE))
        .thenReturn(namespace(BRANCH));

    namespaceBranchController.deleteBranch(APP_ID, CLUSTER, NAMESPACE, BRANCH, "operator");

    verify(namespaceBranchService).deleteBranch(APP_ID, CLUSTER, NAMESPACE, BRANCH,
        NamespaceBranchStatus.DELETED, "operator");
  }

  @Test(expected = BadRequestException.class)
  public void deleteBranchShouldRejectOtherClusterAsBranchName() {
    // GHSA-hv72: branchName=`default` must not delete the default cluster.
    when(namespaceService.findOne(APP_ID, CLUSTER, NAMESPACE)).thenReturn(namespace(CLUSTER));
    when(namespaceBranchService.findBranch(APP_ID, CLUSTER, NAMESPACE))
        .thenReturn(namespace(BRANCH));

    namespaceBranchController.deleteBranch(APP_ID, CLUSTER, NAMESPACE, CLUSTER, "operator");
  }

  @Test(expected = BadRequestException.class)
  public void deleteBranchShouldRejectWhenNoBranchExists() {
    when(namespaceService.findOne(APP_ID, CLUSTER, NAMESPACE)).thenReturn(namespace(CLUSTER));
    when(namespaceBranchService.findBranch(APP_ID, CLUSTER, NAMESPACE)).thenReturn(null);

    namespaceBranchController.deleteBranch(APP_ID, CLUSTER, NAMESPACE, BRANCH, "operator");
  }

  @Test
  public void deleteBranchShouldNotCallServiceWhenBranchNameIsNotABranch() {
    when(namespaceService.findOne(APP_ID, CLUSTER, NAMESPACE)).thenReturn(namespace(CLUSTER));
    when(namespaceBranchService.findBranch(APP_ID, CLUSTER, NAMESPACE))
        .thenReturn(namespace(BRANCH));

    try {
      namespaceBranchController.deleteBranch(APP_ID, CLUSTER, NAMESPACE, CLUSTER, "operator");
    } catch (BadRequestException expected) {
      // expected
    }

    verify(namespaceBranchService, never()).deleteBranch(anyString(), anyString(), anyString(),
        anyString(), anyInt(), anyString());
  }

  private static Namespace namespace(String clusterName) {
    Namespace namespace = new Namespace();
    namespace.setAppId(APP_ID);
    namespace.setClusterName(clusterName);
    namespace.setNamespaceName(NAMESPACE);
    return namespace;
  }
}
