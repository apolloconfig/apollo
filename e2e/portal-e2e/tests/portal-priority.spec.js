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
  USERNAME,
  generateUniqueId,
  login,
  createAppViaUi,
  createNamespaceViaUi,
  assignNamespaceRoleViaUi,
  revokeNamespaceRoleViaUi,
  openConfigPage,
  createNamespaceItem,
  createBranchNamespaceItem,
  updateNamespaceItem,
  publishNamespace,
  editNamespaceTextViaUi,
  linkPublicNamespacesViaUi,
  createBranchViaUi,
  addGrayRuleViaUi,
  grayPublishNamespaceViaUi,
  mergeAndPublishNamespaceViaUi,
  discardGrayBranchViaUi,
  modifyNamespaceTextViaPortalApi,
  waitForApolloConfigValue,
  toPropertiesText,
} = require('./helpers/portal-helpers');

test.describe.serial('@regression Apollo Portal high-priority user-guide scenarios', () => {
  test('namespace role page supports grant and revoke operations @regression', async ({ page }) => {
    const appId = generateUniqueId('e2e-role-');
    const namespaceSeed = generateUniqueId('role_ns_');

    await login(page);
    await createAppViaUi(page, appId);
    const namespaceName = await createNamespaceViaUi(page, appId, namespaceSeed);

    const modifyRoleEnv = await assignNamespaceRoleViaUi(page, appId, namespaceName, {
      roleType: 'ModifyNamespace',
      userId: USERNAME,
    });
    const releaseRoleEnv = await assignNamespaceRoleViaUi(page, appId, namespaceName, {
      roleType: 'ReleaseNamespace',
      userId: USERNAME,
    });

    await revokeNamespaceRoleViaUi(page, appId, namespaceName, {
      roleType: 'ReleaseNamespace',
      userId: USERNAME,
      env: releaseRoleEnv,
    });
    await revokeNamespaceRoleViaUi(page, appId, namespaceName, {
      roleType: 'ModifyNamespace',
      userId: USERNAME,
      env: modifyRoleEnv,
    });
  });

  test('text mode edit and publish are readable from config service @regression', async ({ page, request }) => {
    const appId = generateUniqueId('e2e-text-');
    const configKey = generateUniqueId('text_key_');

    await login(page);
    await createAppViaUi(page, appId);

    await openConfigPage(page, appId);
    await createNamespaceItem(page, appId, configKey, '100', 'priority text mode baseline');
    await publishNamespace(page, appId, generateUniqueId('release_'), 'priority text mode baseline release');
    await waitForApolloConfigValue(request, appId, 'application', configKey, '100', {
      ip: '2.2.2.2',
    });

    await editNamespaceTextViaUi(page, appId, toPropertiesText({
      [configKey]: '300',
    }));
    await publishNamespace(page, appId, generateUniqueId('release_'), 'priority text mode publish');
    await waitForApolloConfigValue(request, appId, 'application', configKey, '300', {
      ip: '2.2.2.2',
    });
  });

  test('linked public namespace supports association and override @regression', async ({ page, request }) => {
    const providerAppId = generateUniqueId('e2e-pub-');
    const consumerAppId = generateUniqueId('e2e-link-');
    const publicNamespaceSeed = `pub${Date.now().toString().slice(-6)}`;
    const sharedKey = generateUniqueId('shared_');
    const overrideKey = generateUniqueId('override_');

    await login(page);

    await createAppViaUi(page, providerAppId);
    const publicNamespaceName = await createNamespaceViaUi(page, providerAppId, publicNamespaceSeed, {
      isPublic: true,
      format: 'properties',
    });
    await openConfigPage(page, providerAppId, { namespaceName: publicNamespaceName });
    await createNamespaceItem(
      page,
      providerAppId,
      sharedKey,
      'provider-default',
      'priority linked namespace shared value',
      { namespaceName: publicNamespaceName }
    );
    await createNamespaceItem(
      page,
      providerAppId,
      overrideKey,
      'provider-v1',
      'priority linked namespace override baseline',
      { namespaceName: publicNamespaceName }
    );
    await publishNamespace(
      page,
      providerAppId,
      generateUniqueId('release_'),
      'priority public namespace release',
      { namespaceName: publicNamespaceName }
    );

    await createAppViaUi(page, consumerAppId);
    await linkPublicNamespacesViaUi(page, consumerAppId, [publicNamespaceName]);

    await openConfigPage(page, consumerAppId, { namespaceName: publicNamespaceName });
    await updateNamespaceItem(
      page,
      consumerAppId,
      overrideKey,
      'consumer-v2',
      'priority linked namespace override',
      { namespaceName: publicNamespaceName }
    );
    await publishNamespace(
      page,
      consumerAppId,
      generateUniqueId('release_'),
      'priority linked namespace release',
      { namespaceName: publicNamespaceName }
    );

    await waitForApolloConfigValue(request, consumerAppId, publicNamespaceName, sharedKey, 'provider-default');
    await waitForApolloConfigValue(request, consumerAppId, publicNamespaceName, overrideKey, 'consumer-v2');
  });

  test('grayscale ui supports create rule publish merge and discard @regression', async ({ page, request }) => {
    const appId = generateUniqueId('e2e-gray-');
    const configKey = generateUniqueId('gray_key_');
    const mergeKey = generateUniqueId('merge_key_');
    const firstGrayValue = '300';
    const mergePublishValue = '350';
    const grayClientIp = '1.1.1.1';
    const normalClientIp = '2.2.2.2';

    await login(page);
    await createAppViaUi(page, appId);

    await openConfigPage(page, appId);
    await createNamespaceItem(page, appId, configKey, '100', 'priority grayscale baseline');
    await publishNamespace(page, appId, generateUniqueId('release_'), 'priority grayscale baseline release');

    const branchName = await createBranchViaUi(page, appId);
    expect(branchName).toBeTruthy();

    await modifyNamespaceTextViaPortalApi(
      page,
      appId,
      toPropertiesText({ [configKey]: firstGrayValue }),
      {
        clusterName: branchName,
        namespaceName: 'application',
        format: 'properties',
      }
    );

    await openConfigPage(page, appId);
    await addGrayRuleViaUi(page, appId, {
      namespaceName: 'application',
      clientAppId: appId,
      clientIpList: [grayClientIp],
    });
    await grayPublishNamespaceViaUi(
      page,
      appId,
      generateUniqueId('gray_'),
      'priority grayscale publish',
      { namespaceName: 'application' }
    );

    await waitForApolloConfigValue(request, appId, 'application', configKey, firstGrayValue, {
      ip: grayClientIp,
    });
    await waitForApolloConfigValue(request, appId, 'application', configKey, '100', {
      ip: normalClientIp,
    });

    await openConfigPage(page, appId);
    await discardGrayBranchViaUi(page, appId, { namespaceName: 'application' });

    await openConfigPage(page, appId);
    const mergeBranchName = await createBranchViaUi(page, appId, { namespaceName: 'application' });
    expect(mergeBranchName).toBeTruthy();
    await createBranchNamespaceItem(
      page,
      appId,
      mergeKey,
      mergePublishValue,
      'priority grayscale merge branch value',
      { namespaceName: 'application' }
    );
    await openConfigPage(page, appId);

    await mergeAndPublishNamespaceViaUi(
      page,
      appId,
      generateUniqueId('merge_'),
      'priority grayscale merge and publish',
      {
        namespaceName: 'application',
        deleteBranch: true,
      }
    );
    await waitForApolloConfigValue(request, appId, 'application', mergeKey, mergePublishValue, {
      ip: normalClientIp,
    });

    await openConfigPage(page, appId);
    const createBranchButton = page.locator('.panel.namespace-panel:not(.hidden)').filter({
      has: page.locator('b.namespace-name', { hasText: /^application$/i }),
    }).first().locator('[ng-click="preCreateBranch(namespace)"]:visible').first();
    await expect(createBranchButton).toBeVisible({ timeout: 30000 });
  });
});
