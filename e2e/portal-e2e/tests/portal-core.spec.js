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
  openConfigPage,
  createNamespaceItem,
  updateNamespaceItem,
  publishNamespace,
  rollbackLatestRelease,
} = require('./helpers/portal-helpers');

test.describe.serial('@smoke Apollo Portal config lifecycle', () => {
  let createdAppId = '';
  let itemKey = '';
  let firstReleaseName = '';
  let secondReleaseName = '';

  test('login flow works @smoke', async ({ page }) => {
    await login(page);
    await expect(page).not.toHaveURL(/login\.html/);
  });

  test('create app flow works @smoke', async ({ page }) => {
    createdAppId = generateUniqueId('e2e');

    await login(page);
    await createAppViaUi(page, createdAppId);
  });

  test('create item and first release works @smoke', async ({ page }) => {
    expect(createdAppId).toBeTruthy();

    itemKey = generateUniqueId('timeout_');
    firstReleaseName = generateUniqueId('release_');

    await login(page);
    await openConfigPage(page, createdAppId);
    await createNamespaceItem(page, createdAppId, itemKey, '100', 'portal smoke create item');
    await publishNamespace(page, createdAppId, firstReleaseName, 'portal smoke first release');
  });

  test('update item and second release works @smoke', async ({ page }) => {
    expect(createdAppId).toBeTruthy();
    expect(itemKey).toBeTruthy();

    secondReleaseName = generateUniqueId('release_');

    await login(page);
    await openConfigPage(page, createdAppId);
    await updateNamespaceItem(page, createdAppId, itemKey, '200', 'portal smoke update item');
    await publishNamespace(page, createdAppId, secondReleaseName, 'portal smoke second release');
  });

  test('rollback latest release works @smoke', async ({ page }) => {
    expect(createdAppId).toBeTruthy();

    await login(page);
    await openConfigPage(page, createdAppId);
    await rollbackLatestRelease(page);
  });

  test('release history contains publish and rollback records @smoke', async ({ page }) => {
    expect(createdAppId).toBeTruthy();

    await login(page);

    const historyResponsePromise = waitForApiResponse(
      page,
      'GET',
      `/apps/${createdAppId}/envs/LOCAL/clusters/default/namespaces/application/releases/histories`,
      200
    );

    await page.goto(
      `/config/history.html?#/appid=${createdAppId}&env=LOCAL&clusterName=default&namespaceName=application`,
      { waitUntil: 'domcontentloaded' }
    );

    const historyResponse = await historyResponsePromise;
    const histories = await historyResponse.json();

    expect(Array.isArray(histories)).toBeTruthy();
    expect(histories.length).toBeGreaterThanOrEqual(3);
    expect(histories.some((history) => history.operation === 0 || history.operation === 5)).toBeTruthy();
    expect(histories.some((history) => history.operation === 1 || history.operation === 6)).toBeTruthy();
    expect(histories.some((history) => history.releaseTitle === firstReleaseName)).toBeTruthy();
    expect(histories.some((history) => history.releaseTitle === secondReleaseName)).toBeTruthy();

    await expect(page.locator('.release-history-list .media').first()).toBeVisible({ timeout: 30000 });
  });
});
