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
package com.ctrip.framework.apollo.adminservice.aop;


import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.entity.NamespaceLock;
import com.ctrip.framework.apollo.biz.service.ItemService;
import com.ctrip.framework.apollo.biz.service.NamespaceLockService;
import com.ctrip.framework.apollo.biz.service.NamespaceService;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.ServiceException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataIntegrityViolationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NamespaceLockTest {

  private static final String APP = "app-test";
  private static final String CLUSTER = "cluster-test";
  private static final String NAMESPACE = "namespace-test";
  private static final String CURRENT_USER = "user-test";
  private static final String ANOTHER_USER = "user-test2";
  private static final long NAMESPACE_ID = 100;

  @Mock
  private NamespaceLockService namespaceLockService;
  @Mock
  private NamespaceService namespaceService;
  @Mock
  private ItemService itemService;
  @Mock
  private BizConfig bizConfig;
  @InjectMocks
  NamespaceAcquireLockAspect namespaceLockAspect;

  @Test
  public void acquireLockWithNotLockedAndSwitchON() {

    when(bizConfig.isNamespaceLockSwitchOff()).thenReturn(true);

    namespaceLockAspect.acquireLock(APP, CLUSTER, NAMESPACE, CURRENT_USER);

    verify(namespaceService, times(0)).findOne(APP, CLUSTER, NAMESPACE);
  }

  @Test
  public void acquireLockWithNotLockedAndSwitchOFF() {

    when(bizConfig.isNamespaceLockSwitchOff()).thenReturn(false);
    when(namespaceService.findOne(APP, CLUSTER, NAMESPACE)).thenReturn(mockNamespace());
    when(namespaceLockService.findLock(anyLong())).thenReturn(null);

    namespaceLockAspect.acquireLock(APP, CLUSTER, NAMESPACE, CURRENT_USER);

    verify(bizConfig).isNamespaceLockSwitchOff();
    verify(namespaceService).findOne(APP, CLUSTER, NAMESPACE);
    verify(namespaceLockService).findLock(anyLong());
    verify(namespaceLockService).tryLock(any());

  }

  @Test(expected = BadRequestException.class)
  public void acquireLockWithAlreadyLockedByOtherGuy() {

    when(bizConfig.isNamespaceLockSwitchOff()).thenReturn(false);
    when(namespaceService.findOne(APP, CLUSTER, NAMESPACE)).thenReturn(mockNamespace());
    when(namespaceLockService.findLock(NAMESPACE_ID)).thenReturn(mockNamespaceLock(ANOTHER_USER));

    namespaceLockAspect.acquireLock(APP, CLUSTER, NAMESPACE, CURRENT_USER);

    verify(bizConfig).isNamespaceLockSwitchOff();
    verify(namespaceService).findOne(APP, CLUSTER, NAMESPACE);
    verify(namespaceLockService).findLock(NAMESPACE_ID);
  }

  @Test
  public void acquireLockWithAlreadyLockedBySelf() {

    when(bizConfig.isNamespaceLockSwitchOff()).thenReturn(false);
    when(namespaceService.findOne(APP, CLUSTER, NAMESPACE)).thenReturn(mockNamespace());
    when(namespaceLockService.findLock(NAMESPACE_ID)).thenReturn(mockNamespaceLock(CURRENT_USER));

    namespaceLockAspect.acquireLock(APP, CLUSTER, NAMESPACE, CURRENT_USER);

    verify(bizConfig).isNamespaceLockSwitchOff();
    verify(namespaceService).findOne(APP, CLUSTER, NAMESPACE);
    verify(namespaceLockService).findLock(NAMESPACE_ID);
  }

  @Test
  public void acquireLockWithNamespaceIdSwitchOn(){

    when(bizConfig.isNamespaceLockSwitchOff()).thenReturn(false);
    when(namespaceService.findOne(NAMESPACE_ID)).thenReturn(mockNamespace());
    when(namespaceLockService.findLock(NAMESPACE_ID)).thenReturn(null);

    namespaceLockAspect.acquireLock(NAMESPACE_ID, CURRENT_USER);

    verify(bizConfig).isNamespaceLockSwitchOff();
    verify(namespaceService).findOne(NAMESPACE_ID);
    verify(namespaceLockService).findLock(NAMESPACE_ID);
    verify(namespaceLockService).tryLock(any());
  }

  @Test(expected = ServiceException.class)
  public void testDuplicateLock(){

    when(bizConfig.isNamespaceLockSwitchOff()).thenReturn(false);
    when(namespaceService.findOne(NAMESPACE_ID)).thenReturn(mockNamespace());
    when(namespaceLockService.findLock(NAMESPACE_ID)).thenReturn(null);
    when(namespaceLockService.tryLock(any())).thenThrow(DataIntegrityViolationException.class);

    namespaceLockAspect.acquireLock(NAMESPACE_ID, CURRENT_USER);

    verify(bizConfig).isNamespaceLockSwitchOff();
    verify(namespaceService).findOne(NAMESPACE_ID);
    verify(namespaceLockService, times(2)).findLock(NAMESPACE_ID);
    verify(namespaceLockService).tryLock(any());

  }

  private Namespace mockNamespace() {
    Namespace namespace = new Namespace();
    namespace.setId(NAMESPACE_ID);
    namespace.setAppId(APP);
    namespace.setClusterName(CLUSTER);
    namespace.setNamespaceName(NAMESPACE);
    return namespace;
  }

  private NamespaceLock mockNamespaceLock(String locedUser) {
    NamespaceLock lock = new NamespaceLock();
    lock.setNamespaceId(NAMESPACE_ID);
    lock.setDataChangeCreatedBy(locedUser);
    return lock;
  }


}
