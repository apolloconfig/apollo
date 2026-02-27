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
package com.ctrip.framework.apollo.portal.controller;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.NamespaceLockDTO;
import com.ctrip.framework.apollo.portal.entity.vo.LockInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.NamespaceLockService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NamespaceLockControllerTest {

  @Mock
  private NamespaceLockService namespaceLockService;

  @InjectMocks
  private NamespaceLockController namespaceLockController;

  @Test
  public void shouldGetNamespaceLock() {
    NamespaceLockDTO expected = new NamespaceLockDTO();
    when(namespaceLockService.getNamespaceLock("SampleApp", Env.DEV, "default", "application"))
        .thenReturn(expected);

    NamespaceLockDTO result =
        namespaceLockController.getNamespaceLock("SampleApp", "DEV", "default", "application");

    assertSame(expected, result);
    verify(namespaceLockService).getNamespaceLock("SampleApp", Env.DEV, "default", "application");
  }

  @Test
  public void shouldGetNamespaceLockInfo() {
    LockInfo expected = new LockInfo();
    expected.setLockOwner("apollo");
    when(namespaceLockService.getNamespaceLockInfo("SampleApp", Env.DEV, "default", "application"))
        .thenReturn(expected);

    LockInfo result =
        namespaceLockController.getNamespaceLockInfo("SampleApp", "DEV", "default", "application");

    assertSame(expected, result);
    verify(namespaceLockService).getNamespaceLockInfo("SampleApp", Env.DEV, "default",
        "application");
  }
}
