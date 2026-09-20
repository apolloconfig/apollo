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
const { test: base, expect } = require('@playwright/test');
const {
  USERNAME, generateUniqueId, login, openConfigPage, createNamespaceViaUi,
  createBranchViaPortalApi, switchNamespaceBranch, addGrayRuleViaUi,
  waitForApolloConfigValue,
} = require('./helpers/portal-helpers');

const ADMIN_URL = process.env.ADMIN_URL || 'http://127.0.0.1:8090';
const WIRE_DATE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}[+-]\d{4}$/;
const DISPLAY_DATE = /^\s*\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\s*$/;
const timeCells = 'td[ng-bind*="config.item.dataChangeLastModifiedTime"]:visible';

function namespaceUrl(appId, namespace = 'application', cluster = 'default') {
  return `/openapi/v1/envs/LOCAL/apps/${appId}/clusters/${cluster}/namespaces/${namespace}`;
}

function panel(page, namespace = 'application') {
  return page.locator('.panel.namespace-panel:not(.hidden)').filter({
    has: page.locator('b.namespace-name', { hasText: namespace }),
  }).first();
}

async function json(response) {
  expect(response.status(), response.url()).toBe(200);
  return response.json();
}

function expectAuditDates(value) {
  for (const field of ['dataChangeCreatedTime', 'dataChangeLastModifiedTime']) {
    expect(value[field], field).toMatch(WIRE_DATE);
    expect(Number.isNaN(Date.parse(value[field])), field).toBe(false);
  }
}

async function createItem(request, url, key, value = key) {
  return json(await request.post(`${url}/items`, { data: { key, value } }));
}

async function publish(request, url, title) {
  const release = await json(await request.post(`${url}/releases`, {
    data: { releaseTitle: title, releaseComment: 'field preservation regression' },
  }));
  expectAuditDates(release);
  return release;
}

function normalizedRules(items) {
  return items.map((item) => ({
    clientAppId: item.clientAppId,
    clientIpList: [...item.clientIpList].sort(),
    clientLabelList: [...item.clientLabelList].sort(),
  })).sort((a, b) => a.clientAppId.localeCompare(b.clientAppId));
}

const test = base.extend({
  appId: async ({ page }, use) => {
    const appId = generateUniqueId('e2e-fields-');
    await login(page);
    const request = page.context().request;
    try {
      const created = await request.post('/openapi/v1/apps', {
        data: {
          assignAppRoleToSelf: true,
          admins: [USERNAME],
          app: {
            appId, name: appId, orgId: 'TEST1', orgName: 'Sample Department 1',
            ownerName: USERNAME, ownerEmail: 'apollo@localhost',
          },
        },
      });
      expect(created.status(), 'create isolated field regression app').toBe(200);
      await expect.poll(async () => (await request.get(namespaceUrl(appId))).status())
        .toBe(200);
      await use(appId);
    } finally {
      const cleanup = await request.delete(`/openapi/v1/apps/${appId}`);
      expect([200, 404], 'remove isolated field regression app').toContain(cleanup.status());
    }
  },
});

// The metadata case reads access key secrets; keep API responses out of traces.
test.use({ trace: 'off' });

test('master and gray item timestamps render and sort correctly @regression', async ({ page, appId }, testInfo) => {
  const request = page.context().request;
  const url = namespaceUrl(appId);
  const earlier = await createItem(request, url, 'z-earlier');
  expectAuditDates(earlier);
  // Use distinct displayed seconds, so the UI assertions also catch swapped timestamps.
  await expect.poll(() => Date.now()).toBeGreaterThan(Date.parse(earlier.dataChangeLastModifiedTime) + 1100);
  const later = await createItem(request, url, 'a-later');
  expectAuditDates(later);
  await openConfigPage(page, appId);
  const namespacePanel = panel(page);
  await expect(namespacePanel.locator(timeCells)).toHaveText([DISPLAY_DATE, DISPLAY_DATE]);
  const rows = namespacePanel.locator('tr[ng-repeat="config in namespace.viewItems |orderBy:col:desc"]:visible');
  const sort = namespacePanel.locator('[ng-click="col=\'item.dataChangeLastModifiedTime\';desc=!desc;"]:visible').last();
  await sort.click();
  await expect(rows.first()).toContainText('a-later');
  await sort.click();
  await expect(rows.first()).toContainText('z-earlier');
  await page.screenshot({ path: testInfo.outputPath('master-item-timestamps.png'), fullPage: true });

  await namespacePanel.locator('[ng-click="publish(namespace)"]:visible').click();
  const previewDates = page.locator('#releaseModal [ng-bind*="dataChangeLastModifiedTime"]:visible');
  await expect(previewDates).toHaveCount(4);
  for (const cell of await previewDates.all()) {
    await expect(cell).toHaveText(/^(\d{4}-\d{2}-\d{2}|\d{2}:\d{2}:\d{2})$/);
  }
  await page.locator('#releaseModal button[data-dismiss="modal"]').first().click();
  await expect(page.locator('#releaseModal')).toBeHidden();
  await publish(request, url, 'master-baseline');

  const branch = await createBranchViaPortalApi(page, appId);
  const branchUrl = namespaceUrl(appId, 'application', branch);
  const grayEarlier = await createItem(request, branchUrl, 'z-earlier', 'gray-earlier');
  await expect.poll(() => Date.now()).toBeGreaterThan(Date.parse(grayEarlier.dataChangeLastModifiedTime) + 1100);
  await createItem(request, branchUrl, 'a-later', 'gray-later');
  // Reload after API writes; navigating to the same hash does not refresh Angular state.
  await page.reload({ waitUntil: 'domcontentloaded' });
  await switchNamespaceBranch(page, 'application', 'gray');
  const grayTable = namespacePanel.locator('table').filter({
    has: page.locator('tr[ng-repeat="config in namespace.branch.branchItems |orderBy:col:desc"]'),
  });
  await expect(grayTable.locator(timeCells)).toHaveText([DISPLAY_DATE, DISPLAY_DATE]);
  const grayRows = grayTable.locator('tbody tr:visible');
  const graySort = grayTable.locator('[ng-click="col=\'item.dataChangeLastModifiedTime\';desc=!desc;"]:visible').last();
  await graySort.click();
  await expect(grayRows.first()).toContainText('a-later');
  await graySort.click();
  await expect(grayRows.first()).toContainText('z-earlier');
  await page.screenshot({ path: testInfo.outputPath('gray-item-timestamps.png'), fullPage: true });
});

test('adding editing and deleting gray rules preserves other clients @regression', async ({ page, appId }, testInfo) => {
  const request = page.context().request;
  const namespace = await createNamespaceViaUi(page, appId, generateUniqueId('fields_'), { isPublic: true });
  const branch = await createBranchViaPortalApi(page, appId, { namespaceName: namespace });
  const rulesUrl = `${namespaceUrl(appId, namespace)}/branches/${branch}/rules`;
  const adminRulesUrl = `${ADMIN_URL}/apps/${appId}/clusters/default/namespaces/${namespace}/branches/${branch}/rules`;
  const original = [
    { clientAppId: 'client-a', clientIpList: ['10.0.0.1', '10.0.0.2'], clientLabelList: ['blue', 'canary'] },
    { clientAppId: 'client-b', clientIpList: ['*'], clientLabelList: ['green'] },
  ];
  const seed = await request.put(rulesUrl, { data: {
    appId, clusterName: 'default', namespaceName: namespace, branchName: branch, ruleItems: original,
  } });
  expect([200, 204]).toContain(seed.status());

  async function assertRules(expected) {
    const portalRules = await json(await request.get(rulesUrl));
    // Read persisted rules directly from AdminService, independently of the OpenAPI converter.
    const storedRules = await json(await request.get(adminRulesUrl));
    expect(normalizedRules(portalRules.ruleItems)).toEqual(normalizedRules(expected));
    expect(normalizedRules(storedRules.ruleItems)).toEqual(normalizedRules(expected));
  }

  await assertRules(original);
  await openConfigPage(page, appId, { namespaceName: namespace });
  await switchNamespaceBranch(page, namespace, 'gray');
  const namespacePanel = panel(page, namespace);
  const ruleView = namespacePanel.locator('[ng-click="switchView(namespace.branch, \'rule\')"]');
  await ruleView.click();
  const rows = namespacePanel.locator('tr[ng-repeat="ruleItem in namespace.branch.rules.ruleItems"]:visible');
  await expect(rows).toHaveCount(2);
  for (const value of ['10.0.0.1', '10.0.0.2', 'blue', 'canary']) {
    await expect(rows.filter({ hasText: 'client-a' })).toContainText(value);
  }

  const added = { clientAppId: 'client-c', clientIpList: ['10.0.0.3'], clientLabelList: ['third'] };
  await addGrayRuleViaUi(page, appId, { namespaceName: namespace, ...added });
  await assertRules([...original, added]);
  await page.reload({ waitUntil: 'domcontentloaded' });
  await switchNamespaceBranch(page, namespace, 'gray');
  await ruleView.click();
  await expect(rows).toHaveCount(3);
  await page.screenshot({ path: testInfo.outputPath('three-gray-rules.png'), fullPage: true });

  await rows.filter({ hasText: 'client-a' }).locator('[ng-click="editRuleItem(namespace.branch, ruleItem)"]').click();
  await page.locator('#rulesModal textarea[rows="1"]').fill('edited');
  await page.locator('#rulesModal button.add-rule').nth(1).click();
  let saved = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith(rulesUrl));
  await page.locator('#rulesModal .modal-footer button.btn-primary').click();
  expect([200, 204]).toContain((await saved).status());
  const edited = { ...original[0], clientLabelList: [...original[0].clientLabelList, 'edited'] };
  await assertRules([edited, original[1], added]);

  saved = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith(rulesUrl));
  await rows.filter({ hasText: 'client-c' }).locator('[ng-click="deleteRuleItem(namespace.branch, ruleItem)"]').click();
  expect([200, 204]).toContain((await saved).status());
  await assertRules([edited, original[1]]);
  await expect(rows).toHaveCount(2);
});

test('latest and older instances show delivery times and their release @regression', async ({ page, request, appId }, testInfo) => {
  const portal = page.context().request;
  const url = namespaceUrl(appId);
  await createItem(portal, url, 'version', 'one');
  const first = await publish(portal, url, 'first-release');
  await waitForApolloConfigValue(request, appId, 'application', 'version', 'one', { ip: '10.1.0.1' });
  await expect.poll(async () => {
    const result = await json(await portal.get(`${ADMIN_URL}/instances/by-release`, { params: { releaseId: first.id } }));
    return result.total;
  }, { timeout: 30000 }).toBe(1);

  const update = await portal.put(`${url}/items/version`, { data: { key: 'version', value: 'two' } });
  expect([200, 204]).toContain(update.status());
  expect((await json(await portal.get(`${url}/items/version`))).type).toBe(0);
  const latest = await publish(portal, url, 'latest-release');
  await waitForApolloConfigValue(request, appId, 'application', 'version', 'two', { ip: '10.1.0.2' });
  await expect.poll(async () => {
    const result = await json(await portal.get(`${ADMIN_URL}/instances/by-release`, { params: { releaseId: latest.id } }));
    return result.total;
  }, { timeout: 30000 }).toBe(1);

  const latestPage = await json(await portal.get('/openapi/v1/envs/LOCAL/instances/by-release', {
    params: { releaseId: latest.id, page: 0, size: 20 },
  }));
  expect(latestPage.total).toBe(1);
  expect(latestPage.instances[0].ip).toBe('10.1.0.2');
  expect(latestPage.instances[0].configs).toHaveLength(1);
  expect(latestPage.instances[0].configs[0].releaseDeliveryTime).toMatch(WIRE_DATE);
  expect(latestPage.instances[0].configs[0].dataChangeLastModifiedTime).toMatch(WIRE_DATE);
  const params = { appId, clusterName: 'default', namespaceName: 'application', releaseIds: latest.id };
  const older = await json(await portal.get('/openapi/v1/envs/LOCAL/instances/by-namespace-and-releases-not-in', { params }));
  const stored = await json(await portal.get(`${ADMIN_URL}/instances/by-namespace-and-releases-not-in`, { params }));
  expect(older).toHaveLength(1);
  expect(older[0].ip).toBe('10.1.0.1');
  expect(older[0].configs).toHaveLength(1);
  expect(older[0].configs[0].release.id).toBe(first.id);
  expect(older[0].configs[0].release.name).toBe(first.name);
  expectAuditDates(older[0].configs[0].release);
  expect(older[0].configs[0].releaseDeliveryTime).toBe(stored[0].configs[0].releaseDeliveryTime);
  expect(older[0].configs[0].dataChangeLastModifiedTime).toBe(stored[0].configs[0].dataChangeLastModifiedTime);
  const all = await json(await portal.get('/openapi/v1/envs/LOCAL/instances/by-namespace', {
    params: { ...params, page: 0, size: 20 },
  }));
  expect(all.total).toBe(2);
  expect(all.instances).toHaveLength(2);
  for (const instance of all.instances) expect(instance.dataChangeCreatedTime).toMatch(WIRE_DATE);

  await openConfigPage(page, appId);
  const namespacePanel = panel(page);
  await namespacePanel.locator('[ng-click="switchView(namespace, \'instance\')"]').click();
  const latestRows = namespacePanel.locator('tr[ng-repeat="instance in namespace.latestReleaseInstances.content"]:visible');
  await expect(latestRows).toHaveCount(1);
  await expect(latestRows).toContainText('10.1.0.2');
  await expect(latestRows.locator('td').last()).toHaveText(DISPLAY_DATE);
  await expect(namespacePanel.locator('.instance-view:visible')).toContainText('latest-release');
  await page.screenshot({ path: testInfo.outputPath('latest-instance.png'), fullPage: true });
  await expect(namespacePanel.locator('[ng-click="switchInstanceViewType(namespace, \'all\')"] .badge')).toHaveText('2');
  await namespacePanel.locator('[ng-click="switchInstanceViewType(namespace, \'not_latest_release\')"]').click();
  const olderRows = namespacePanel.locator('tr[ng-repeat="instance in namespace.notLatestReleaseInstances[release.id]"]:visible');
  await expect(olderRows).toHaveCount(1);
  await expect(olderRows).toContainText('10.1.0.1');
  await expect(olderRows.locator('td').last()).toHaveText(DISPLAY_DATE);
  await expect(namespacePanel.locator('.instance-view:visible')).toContainText('first-release');
  await page.screenshot({ path: testInfo.outputPath('older-instance.png'), fullPage: true });
});

test.describe('nonempty application and access key metadata', () => {
  test('application audit identities and access key dates survive conversion @regression', async ({ page, appId }) => {
    const request = page.context().request;
    const apps = await json(await request.get('/openapi/v1/apps', { params: { appIds: appId } }));
    expect(apps).toHaveLength(1);
    expectAuditDates(apps[0]);
    for (const field of ['dataChangeCreatedByDisplayName', 'dataChangeLastModifiedByDisplayName', 'ownerDisplayName']) {
      expect(apps[0][field], field).toBeTruthy();
    }
    const url = `/openapi/v1/apps/${appId}/envs/LOCAL/accesskeys`;
    const key = await json(await request.post(url, { params: { operator: USERNAME }, data: {} }));
    expectAuditDates(key);
    try {
      const keys = await json(await request.get(url));
      expect(keys.length).toBe(1);
      expectAuditDates(keys[0]);
      // Compare only timestamps, so assertion output cannot expose the generated secret.
      const legacy = await json(await request.get(`/apps/${appId}/envs/LOCAL/accesskeys`));
      expect(keys[0].dataChangeCreatedTime).toBe(legacy[0].dataChangeCreatedTime);
      expect(keys[0].dataChangeLastModifiedTime).toBe(legacy[0].dataChangeLastModifiedTime);
    } finally {
      const cleanup = await request.delete(`${url}/${key.id}`, { params: { operator: USERNAME } });
      expect([200, 204]).toContain(cleanup.status());
    }
  });
});
