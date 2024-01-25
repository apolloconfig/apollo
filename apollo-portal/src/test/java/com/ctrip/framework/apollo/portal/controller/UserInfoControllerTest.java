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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.spi.springsecurity.SpringSecurityUserService;
import com.ctrip.framework.apollo.portal.util.checker.AuthUserPasswordChecker;
import com.ctrip.framework.apollo.portal.util.checker.CheckResult;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserInfoControllerTest {

  @InjectMocks
  private UserInfoController userInfoController;
  @Mock
  private SpringSecurityUserService userService;
  @Mock
  private AuthUserPasswordChecker userPasswordChecker;

  @Test
  public void testCreateOrUpdateUser() {
    UserPO user = new UserPO();
    user.setUsername("username");
    user.setPassword("password");

    Mockito.when(userPasswordChecker.checkWeakPassword(Mockito.anyString()))
        .thenReturn(new CheckResult(Boolean.TRUE, ""));

    userInfoController.createOrUpdateUser(true, user);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateOrUpdateUserFailed() {
    UserPO user = new UserPO();
    user.setUsername("username");
    user.setPassword("password");

    String msg = "fake error message";

    Mockito.when(userPasswordChecker.checkWeakPassword(Mockito.anyString()))
        .thenReturn(new CheckResult(Boolean.FALSE, msg));

    try {
      userInfoController.createOrUpdateUser(true, user);
    } catch (BadRequestException e) {
      Assert.assertEquals(msg, e.getMessage());
      throw e;
    }
  }

}