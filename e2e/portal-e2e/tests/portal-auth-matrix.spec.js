/*
 * Copyright 2026 Apollo Authors
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
const { test, expect } = require('@playwright/test');
const {
  generateUniqueId,
  createAppViaUiWithUserSelection,
  assignNamespaceRoleViaUiBySearch,
  revokeNamespaceRoleViaUi,
} = require('./helpers/portal-helpers');
const {
  MODE_LDAP,
  MODE_OIDC,
  resolveAuthMode,
  getAuthUsers,
  loginByMode,
  expectLoginFailureByMode,
  warmUpOidcSecondaryUser,
} = require('./helpers/auth-helpers');

const mode = resolveAuthMode();
const users = getAuthUsers();

function assertSelectionText(selectionText) {
  expect(selectionText).toContain(users.secondaryUser);
  expect(selectionText).toContain(users.secondaryEmail);
  if (mode === MODE_LDAP && users.secondaryDisplayName) {
    expect(selectionText).toContain(users.secondaryDisplayName);
  }
}

test.describe.serial(`@auth-matrix portal login matrix (${mode})`, () => {
  let createdAppId = '';

  test.beforeAll(async ({ browser }) => {
    if (mode === MODE_OIDC) {
      await warmUpOidcSecondaryUser(browser);
    }
  });

  test('login succeeds for allowed user @auth-matrix', async ({ page }) => {
    await loginByMode(page, {
      username: users.successUser,
      password: users.successPassword,
    });
    await expect(page).toHaveURL(/127\.0\.0\.1:8070|localhost:8070/);
  });

  test('login fails for non-existent user @auth-matrix', async ({ page }) => {
    await expectLoginFailureByMode(page, {
      username: users.invalidUser,
      password: users.invalidPassword,
    });
  });

  test('login fails for wrong password @auth-matrix', async ({ page }) => {
    await expectLoginFailureByMode(page, {
      username: users.successUser,
      password: users.wrongPassword,
    });
  });

  test('ldap blocked user is rejected by group filter @auth-matrix', async ({ page }) => {
    test.skip(mode !== MODE_LDAP, 'LDAP-only scenario');

    await expectLoginFailureByMode(page, {
      username: users.blockedUser,
      password: users.blockedPassword,
    });
  });

  test('create app can search and select secondary user with display fields @auth-matrix', async ({
    page,
  }) => {
    createdAppId = generateUniqueId('auth');

    await loginByMode(page, {
      username: users.successUser,
      password: users.successPassword,
    });

    const { ownerSelectionText, adminSelectionText } = await createAppViaUiWithUserSelection(
      page,
      createdAppId,
      {
        ownerKeyword: users.secondaryUser,
        adminKeyword: users.secondaryUser,
      }
    );

    assertSelectionText(ownerSelectionText);
    assertSelectionText(adminSelectionText);
  });

  test('namespace role assignment supports searching secondary user @auth-matrix', async ({ page }) => {
    expect(createdAppId).toBeTruthy();

    await loginByMode(page, {
      username: users.secondaryUser,
      password: users.secondaryPassword || users.successPassword,
    });

    const modifyRoleResult = await assignNamespaceRoleViaUiBySearch(
      page,
      createdAppId,
      'application',
      {
        roleType: 'ModifyNamespace',
        userKeyword: users.secondaryUser,
      }
    );
    assertSelectionText(modifyRoleResult.selectedUserText);

    await revokeNamespaceRoleViaUi(page, createdAppId, 'application', {
      roleType: 'ModifyNamespace',
      userId: modifyRoleResult.selectedUserId,
      ...(modifyRoleResult.targetEnv ? { env: modifyRoleResult.targetEnv } : {}),
    });

    const releaseRoleResult = await assignNamespaceRoleViaUiBySearch(
      page,
      createdAppId,
      'application',
      {
        roleType: 'ReleaseNamespace',
        userKeyword: users.secondaryUser,
      }
    );
    assertSelectionText(releaseRoleResult.selectedUserText);

    await revokeNamespaceRoleViaUi(page, createdAppId, 'application', {
      roleType: 'ReleaseNamespace',
      userId: releaseRoleResult.selectedUserId,
      ...(releaseRoleResult.targetEnv ? { env: releaseRoleResult.targetEnv } : {}),
    });
  });
});
