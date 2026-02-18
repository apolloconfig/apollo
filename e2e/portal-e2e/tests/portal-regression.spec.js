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
  login,
  waitForApiResponse,
  createAppViaUi,
  submitAppCreation,
  submitClusterCreation,
  createClusterViaUi,
  submitNamespaceCreation,
  createNamespaceViaUi,
  openConfigPage,
  createNamespaceItem,
  publishNamespace,
} = require('./helpers/portal-helpers');

test.describe.serial('@regression Apollo Portal extended scenarios', () => {
  test('duplicate app creation is rejected @regression', async ({ page }) => {
    const appId = generateUniqueId('e2e-dup-');

    await login(page);
    await createAppViaUi(page, appId);

    const duplicateCreateResponse = await submitAppCreation(page, appId);
    expect(duplicateCreateResponse.status()).toBeGreaterThanOrEqual(400);
    await expect(page).toHaveURL(/app\.html/, { timeout: 30000 });
  });

  test('cluster and namespace pages support creation flow @regression', async ({ page }) => {
    const appId = generateUniqueId('e2e-reg-');
    const clusterName = generateUniqueId('cluster_');
    const namespaceName = generateUniqueId('ns_');

    await login(page);
    await createAppViaUi(page, appId);

    await createClusterViaUi(page, appId, clusterName);

    const duplicateClusterResponse = await submitClusterCreation(page, appId, clusterName);
    expect(duplicateClusterResponse.status()).toBeGreaterThanOrEqual(400);
    await expect(page).toHaveURL(/cluster\.html/, { timeout: 30000 });

    await createNamespaceViaUi(page, appId, namespaceName);

    const duplicateNamespaceResponse = await submitNamespaceCreation(page, appId, namespaceName);
    expect(duplicateNamespaceResponse.status()).toBeGreaterThanOrEqual(400);
    await expect(page).toHaveURL(/namespace\.html/, { timeout: 30000 });
  });

  test('config export and instance view paths are reachable @regression', async ({ page }) => {
    const appId = generateUniqueId('e2e-exp-');
    const itemKey = generateUniqueId('qps_');
    const releaseName = generateUniqueId('release_');

    await login(page);
    await createAppViaUi(page, appId);
    await openConfigPage(page, appId);
    await createNamespaceItem(page, appId, itemKey, '100', 'portal regression item');
    await publishNamespace(page, appId, releaseName, 'portal regression release');

    await page.goto('/config_export.html', { waitUntil: 'domcontentloaded' });
    await page.click('a[href="#app_config"]');

    await page.fill('#app_config input[ng-model="cluster.appId"]', appId);
    await page.fill('#app_config input[ng-model="cluster.env"]', 'LOCAL');
    await page.fill('#app_config input[ng-model="cluster.name"]', 'default');
    await page.click('#app_config button.btn-info');

    await expect(page.locator('#app_config h5').first()).toContainText(appId, { timeout: 30000 });

    const downloadPromise = page.waitForEvent('download');
    await page.click('#app_config a.btn.btn-primary[ng-click="exportAppConfig()"]');
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain(appId);

    await openConfigPage(page, appId);
    const byNamespaceResponse =
      waitForApiResponse(page, 'GET', '/envs/LOCAL/instances/by-namespace', 200);
    await page.locator('[ng-click="switchView(namespace, \'instance\')"]').first().click();

    const response = await byNamespaceResponse;
    expect(response.status()).toBe(200);
  });
});
