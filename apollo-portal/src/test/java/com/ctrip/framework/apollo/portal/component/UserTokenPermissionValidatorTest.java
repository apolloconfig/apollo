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
package com.ctrip.framework.apollo.portal.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenOperation;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenScope;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.portal.util.UserTokenAuthUtil;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTokenPermissionValidatorTest {

  @Mock
  private UserPermissionValidator userPermissionValidator;

  @Mock
  private UserTokenService userTokenService;

  @Mock
  private UserTokenAuthUtil userTokenAuthUtil;

  private UserTokenPermissionValidator validator;
  private UserToken userToken;
  private UserTokenScope scope;

  @BeforeEach
  void setUp() {
    validator = new UserTokenPermissionValidator(userPermissionValidator, userTokenService,
        userTokenAuthUtil);
    userToken = new UserToken();
    scope = new UserTokenScope();
    when(userTokenAuthUtil.retrieveUserTokenFromCtx()).thenReturn(userToken);
    when(userTokenService.parseScope(userToken)).thenReturn(scope);
  }

  @Test
  void hasModifyNamespacePermissionReturnsTrueWhenUserAndScopeAllow() {
    scope.setOperations(Collections.singleton(UserTokenOperation.CONFIG_MODIFY));
    scope.setAppIds(Collections.singleton("app"));
    scope.setEnvs(Collections.singleton("DEV"));
    when(userPermissionValidator.hasModifyNamespacePermission("app", "DEV", "default",
        "application")).thenReturn(true);

    assertTrue(validator.hasModifyNamespacePermission("app", "DEV", "default", "application"));
  }

  @Test
  void hasModifyNamespacePermissionReturnsFalseWhenScopeDeniesOperation() {
    scope.setOperations(Collections.singleton(UserTokenOperation.CONFIG_READ));
    when(userPermissionValidator.hasModifyNamespacePermission("app", "DEV", "default",
        "application")).thenReturn(true);

    assertFalse(validator.hasModifyNamespacePermission("app", "DEV", "default", "application"));
  }

  @Test
  void shouldHideConfigWhenReadScopeMissing() {
    scope.setOperations(Collections.singleton(UserTokenOperation.CONFIG_MODIFY));

    assertTrue(validator.shouldHideConfigToCurrentUser("app", "DEV", "default", "application"));
  }

  @Test
  void shouldHideConfigWhenAppScopeDenies() {
    scope.setOperations(Collections.singleton(UserTokenOperation.CONFIG_READ));
    scope.setAppIds(Collections.singleton("app"));

    assertTrue(
        validator.shouldHideConfigToCurrentUser("other-app", "DEV", "default", "application"));
  }

  @Test
  void hasReadApplicationPermissionRespectsAppScope() {
    scope.setOperations(Collections.singleton(UserTokenOperation.CONFIG_READ));
    scope.setAppIds(Collections.singleton("app"));

    assertTrue(validator.hasReadApplicationPermission("app"));
    assertFalse(validator.hasReadApplicationPermission("other-app"));
  }
}
