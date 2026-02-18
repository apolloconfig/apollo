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
const { expect } = require('@playwright/test');

const USERNAME = process.env.PORTAL_USERNAME || 'apollo';
const PASSWORD = process.env.PORTAL_PASSWORD || 'admin';
const DEFAULT_SUCCESS_STATUSES = [200, 201, 202, 204];
const DEFAULT_ENV = 'LOCAL';
const DEFAULT_CLUSTER = 'default';
const DEFAULT_NAMESPACE = 'application';
const DEFAULT_CONFIG_BASE_URL = 'http://127.0.0.1:8080';

function generateUniqueId(prefix) {
  const randomSuffix = Math.floor(Math.random() * 10000)
    .toString()
    .padStart(4, '0');
  return `${prefix}${Date.now()}${randomSuffix}`;
}

function isExpectedStatus(actualStatus, expectedStatus) {
  if (Array.isArray(expectedStatus)) {
    return expectedStatus.includes(actualStatus);
  }
  if (expectedStatus === null || expectedStatus === undefined) {
    return actualStatus >= 200 && actualStatus < 400;
  }
  return actualStatus === expectedStatus;
}

function resolveConfigServiceBaseUrl() {
  const explicitConfigUrl = process.env.CONFIG_URL;
  if (explicitConfigUrl) {
    return explicitConfigUrl.replace(/\/$/, '');
  }

  const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8070';
  try {
    const parsed = new URL(baseUrl);
    if (!parsed.port || parsed.port === '8070') {
      parsed.port = '8080';
    }
    return parsed.origin.replace(/\/$/, '');
  } catch (error) {
    return DEFAULT_CONFIG_BASE_URL;
  }
}

function encodePathSegment(value) {
  return encodeURIComponent(value);
}

function toPropertiesText(properties) {
  const lines = Object.entries(properties).map(([key, value]) => `${key}=${value}`);
  return `${lines.join('\n')}\n`;
}

function normalizeNotificationNamespace(namespaceName) {
  return `${namespaceName || ''}`.replace(/\.properties$/i, '').toLowerCase();
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function resolveNamespaceDisplayNames(namespaceName) {
  if (!namespaceName) {
    return [DEFAULT_NAMESPACE];
  }

  const stripped = namespaceName.replace(/\.(properties|yaml|yml|json)$/i, '');
  return Array.from(new Set([namespaceName, stripped].filter(Boolean)));
}

async function sleep(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

async function locateNamespacePanel(page, namespaceName) {
  await page.locator('.panel.namespace-panel').first().waitFor({ state: 'visible', timeout: 90000 });
  const candidates = resolveNamespaceDisplayNames(namespaceName);

  for (const candidate of candidates) {
    const panel = page.locator('.panel.namespace-panel:not(.hidden)').filter({
      has: page.locator('b.namespace-name', {
        hasText: new RegExp(`^\\s*${escapeRegExp(candidate)}\\s*$`, 'i'),
      }),
    }).first();

    if (await panel.count()) {
      await panel.waitFor({ state: 'visible', timeout: 30000 });
      return panel;
    }
  }

  const fallbackPanel = page.locator('.panel.namespace-panel:not(.hidden)').filter({
    has: page.locator('b.namespace-name', { hasText: candidates[0] || DEFAULT_NAMESPACE }),
  }).first();
  await fallbackPanel.waitFor({ state: 'visible', timeout: 30000 });
  return fallbackPanel;
}

async function waitForApiResponse(page, method, urlFragment, status = DEFAULT_SUCCESS_STATUSES) {
  return page.waitForResponse(
    (response) =>
      response.request().method() === method
      && response.url().includes(urlFragment)
      && isExpectedStatus(response.status(), status),
    { timeout: 90000 }
  );
}

async function waitForApiCall(page, method, urlFragment) {
  return page.waitForResponse(
    (response) =>
      response.request().method() === method
      && response.url().includes(urlFragment),
    { timeout: 90000 }
  );
}

async function waitForApiResponseByFragments(
  page,
  method,
  requiredFragments,
  status = DEFAULT_SUCCESS_STATUSES
) {
  return page.waitForResponse(
    (response) => {
      if (response.request().method() !== method) {
        return false;
      }
      if (!isExpectedStatus(response.status(), status)) {
        return false;
      }
      const url = response.url();
      return requiredFragments.every((fragment) => url.includes(fragment));
    },
    { timeout: 90000 }
  );
}

async function login(page) {
  await page.goto('/login.html', { waitUntil: 'domcontentloaded' });
  await page.fill('input[name="username"]', USERNAME);
  await page.fill('input[name="password"]', PASSWORD);

  await Promise.all([
    page.waitForURL((url) => !url.toString().includes('login.html'), {
      timeout: 60000,
    }),
    page.click('#login-submit'),
  ]);

  const cookies = await page.context().cookies();
  expect(cookies.some((cookie) => cookie.name === 'JSESSIONID')).toBeTruthy();
}

async function selectOrganization(page) {
  await page.waitForFunction(() => {
    return (
      typeof window.$ === 'function'
      && window.$('#organization').length > 0
      && window.$('#organization').data('select2')
    );
  });

  await page.evaluate(() => {
    const select = window.$('#organization');
    const internal = select.data('select2');
    const data = internal?.options?.options?.data || [];
    const first = data.find((item) => item && item.id);
    if (!first) {
      throw new Error('No organization options are available');
    }

    if (select.find(`option[value="${first.id}"]`).length === 0) {
      const option = new Option(first.text, first.id, true, true);
      select.append(option);
    }

    select.val(first.id).trigger('change');
  });
}

async function selectUserByKeyword(page, panelSelector, keyword, options = {}) {
  const {
    exact = false,
    timeoutMs = 20000,
  } = options;
  await page.locator(`${panelSelector} .select2-selection`).first().click();
  const searchInput = page.locator('body .select2-container--open .select2-search__field');
  await searchInput.fill(keyword);

  const optionMatcher = exact
    ? new RegExp(`^\\s*${escapeRegExp(keyword)}\\s*$`, 'i')
    : keyword;
  const matchedOption = page
    .locator('body .select2-container--open .select2-results__option')
    .filter({ hasText: optionMatcher })
    .first();

  await matchedOption.waitFor({ state: 'visible', timeout: timeoutMs });
  const selectedText = (await matchedOption.innerText()).trim();
  await matchedOption.click();
  return selectedText;
}

async function createAppViaUiWithUserSelection(page, appId, options = {}) {
  const {
    ownerKeyword = USERNAME,
    adminKeyword = USERNAME,
  } = options;
  await page.goto('/app.html', { waitUntil: 'domcontentloaded' });

  await selectOrganization(page);
  await page.fill('input[name="appId"]', appId);
  await page.fill('input[name="appName"]', appId);
  const ownerSelectionText = await selectUserByKeyword(
    page,
    '.J_ownerSelectorPanel',
    ownerKeyword
  );
  const adminSelectionText = await selectUserByKeyword(
    page,
    '.J_adminSelectorPanel',
    adminKeyword
  );

  await Promise.all([
    page.waitForURL(
      (url) =>
        url.toString().includes('config.html')
        && url.toString().includes(`appid=${appId}`),
      { timeout: 90000 }
    ),
    page.click('button[type="submit"]'),
  ]);

  await expect(page).toHaveURL(/config\.html/);
  return {
    ownerSelectionText,
    adminSelectionText,
  };
}

async function createAppViaUi(page, appId) {
  await createAppViaUiWithUserSelection(page, appId);
}

async function submitAppCreation(page, appId) {
  await page.goto('/app.html', { waitUntil: 'domcontentloaded' });

  await selectOrganization(page);
  await page.fill('input[name="appId"]', appId);
  await page.fill('input[name="appName"]', appId);
  await selectUserByKeyword(page, '.J_ownerSelectorPanel', USERNAME);
  await selectUserByKeyword(page, '.J_adminSelectorPanel', USERNAME);

  const createAppResponse = waitForApiCall(page, 'POST', '/apps');
  await page.click('button[type="submit"]');

  return createAppResponse;
}

async function submitClusterCreation(page, appId, clusterName) {
  await page.goto(`/cluster.html?e2e=${Date.now()}#/appid=${appId}`, { waitUntil: 'domcontentloaded' });
  const clusterNameInput = page.locator('.apollo-container:not(.hidden) input[name="clusterName"]').first();
  const clusterCommentInput = page.locator('.apollo-container:not(.hidden) textarea[name="clusterComment"]').first();
  await clusterNameInput.waitFor({ state: 'visible', timeout: 60000 });
  await clusterNameInput.fill(clusterName);
  await clusterCommentInput.fill('portal regression cluster creation');
  await page.locator('.apollo-container:not(.hidden) tr:has-text("LOCAL") input[type="checkbox"]').first().click();

  const createClusterResponse = waitForApiCall(page, 'POST', `/apps/${appId}/clusters`);
  await page.click('.apollo-container:not(.hidden) form[name="clusterForm"] button[type="submit"]');

  return createClusterResponse;
}

async function createClusterViaUi(page, appId, clusterName) {
  const createClusterResponse = await submitClusterCreation(page, appId, clusterName);
  expect([200, 201, 204, 302]).toContain(createClusterResponse.status());
  await expect(page.locator('div.row.text-center h3')).toBeVisible({ timeout: 30000 });
}

async function submitNamespaceCreation(page, appId, namespaceName) {
  return submitNamespaceCreationWithOptions(page, appId, namespaceName, {});
}

async function submitNamespaceCreationWithOptions(page, appId, namespaceName, options) {
  const { format = 'properties', isPublic, comment = 'portal regression namespace creation' } = options;
  await page.goto(`/namespace.html?#/appid=${appId}`, { waitUntil: 'domcontentloaded' });
  const namespaceNameInput = page.locator('.apollo-container:not(.hidden) input[name="namespaceName"]').first();
  const namespaceCommentInput = page.locator('.apollo-container:not(.hidden) textarea[name="comment"]').first();
  await namespaceNameInput.waitFor({ state: 'visible', timeout: 60000 });
  if (typeof isPublic === 'boolean') {
    const visibilitySelector = isPublic
      ? '.apollo-container:not(.hidden) input[name="namespaceType"][value="true"]'
      : '.apollo-container:not(.hidden) input[name="namespaceType"][value="false"]';
    const namespaceTypeRadio = page.locator(visibilitySelector).first();
    if (await namespaceTypeRadio.isVisible().catch(() => false)) {
      await namespaceTypeRadio.check();
    }
  }
  if (format) {
    await page.locator('.apollo-container:not(.hidden) select[name="format"]').first().selectOption(format);
  }
  await namespaceNameInput.fill(namespaceName);
  await namespaceCommentInput.fill(comment);

  const createNamespaceResponse = waitForApiCall(page, 'POST', `/apps/${appId}/appnamespaces`);
  await page.click('.apollo-container:not(.hidden) form[name="namespaceForm"] button[type="submit"]');

  return createNamespaceResponse;
}

async function createNamespaceViaUi(page, appId, namespaceName, options = {}) {
  const createNamespaceResponse =
    await submitNamespaceCreationWithOptions(page, appId, namespaceName, options);
  expect([200, 201, 202, 204, 302]).toContain(createNamespaceResponse.status());
  let createdNamespaceName = namespaceName;
  const createNamespaceBody = await createNamespaceResponse.json().catch(() => null);
  if (createNamespaceBody && createNamespaceBody.name) {
    createdNamespaceName = createNamespaceBody.name;
  } else if (options.format && options.format !== 'properties') {
    createdNamespaceName = `${namespaceName}.${options.format}`;
  }
  await page.waitForURL((url) => url.toString().includes('/namespace/role.html'), { timeout: 30000 });
  return createdNamespaceName;
}

async function clearNamespaceRoleViaPortalApi(page, appId, namespaceName, options = {}) {
  const {
    roleType = 'ModifyNamespace',
    userId = USERNAME,
    env,
  } = options;
  const namespacePath = encodePathSegment(namespaceName);
  const appPath = encodePathSegment(appId);
  const envPath = env ? encodePathSegment(env) : '';
  const userQuery = encodePathSegment(userId);
  const path = env
    ? `/apps/${appPath}/envs/${envPath}/namespaces/${namespacePath}/roles/${roleType}?user=${userQuery}`
    : `/apps/${appPath}/namespaces/${namespacePath}/roles/${roleType}?user=${userQuery}`;
  const response = await page.context().request.delete(path);
  expect([200, 204, 400, 404]).toContain(response.status());
}

async function assignNamespaceRoleViaUi(page, appId, namespaceName, options = {}) {
  const {
    roleType = 'ModifyNamespace',
    userId = USERNAME,
    env = '',
  } = options;
  const namespaceParam = encodePathSegment(namespaceName);
  await page.goto(`/namespace/role.html?#/appid=${appId}&namespaceName=${namespaceParam}`, {
    waitUntil: 'domcontentloaded',
  });
  await page.locator('section.panel[ng-controller="NamespaceRoleController"]').waitFor({
    state: 'visible',
    timeout: 60000,
  });

  const isReleaseRole = roleType === 'ReleaseNamespace';
  const widgetClass = isReleaseRole ? 'releaseRoleWidgetId' : 'modifyRoleWidgetId';
  const formSelector = isReleaseRole
    ? 'form[ng-submit="assignRoleToUser(\'ReleaseNamespace\')"]'
    : 'form[ng-submit="assignRoleToUser(\'ModifyNamespace\')"]';

  await page.waitForFunction(
    ({ widgetClass: targetWidgetClass, userId: targetUserId }) => {
      if (typeof window.$ !== 'function') {
        return false;
      }
      const selector = window.$(`.${targetWidgetClass}`);
      if (!selector.length || !selector.data('select2')) {
        return false;
      }
      if (selector.find(`option[value="${targetUserId}"]`).length === 0) {
        selector.append(new Option(targetUserId, targetUserId, true, true));
      }
      selector.val(targetUserId).trigger('change');
      return true;
    },
    { widgetClass, userId }
  );

  let targetEnv = env;
  if (env) {
    const envSelector = isReleaseRole
      ? `${formSelector} select[ng-model="releaseRoleSelectedEnv"]`
      : `${formSelector} select[ng-model="modifyRoleSelectedEnv"]`;
    const roleEnvSelector = page.locator(envSelector).first();
    await roleEnvSelector.waitFor({ state: 'visible', timeout: 30000 });
    targetEnv = await roleEnvSelector.evaluate((select, preferredEnv) => {
      const envOptions = Array.from(select.options)
        .map((option) => option.value)
        .filter(Boolean);
      if (envOptions.length === 0) {
        return '';
      }
      const selectedEnv = preferredEnv && envOptions.includes(preferredEnv)
        ? preferredEnv
        : envOptions[0];
      select.value = selectedEnv;
      select.dispatchEvent(new Event('change', { bubbles: true }));
      return selectedEnv;
    }, env);
  }

  await clearNamespaceRoleViaPortalApi(page, appId, namespaceName, {
    roleType,
    userId,
    ...(targetEnv ? { env: targetEnv } : {}),
  });

  const grantRoleResponse = waitForApiResponseByFragments(
    page,
    'POST',
    [
      `/apps/${appId}/`,
      '/namespaces/',
      `/roles/${roleType}`,
      ...(targetEnv ? [`/envs/${targetEnv}/`] : []),
    ]
  );

  await Promise.all([
    grantRoleResponse,
    page.locator(`${formSelector} button[type="submit"]`).first().click(),
  ]);

  await expect(page.locator('.toast-success').first()).toBeVisible({ timeout: 30000 });
  return targetEnv;
}

function parseUserIdFromSelect2Text(selectionText) {
  if (!selectionText) {
    return '';
  }
  const [firstPart] = selectionText.split('|');
  return `${firstPart || ''}`.trim();
}

async function assignNamespaceRoleViaUiBySearch(page, appId, namespaceName, options = {}) {
  const {
    roleType = 'ModifyNamespace',
    userKeyword = USERNAME,
    env = '',
  } = options;
  const namespaceParam = encodePathSegment(namespaceName);
  await page.goto(`/namespace/role.html?#/appid=${appId}&namespaceName=${namespaceParam}`, {
    waitUntil: 'domcontentloaded',
  });
  await page.locator('section.panel[ng-controller="NamespaceRoleController"]').waitFor({
    state: 'visible',
    timeout: 60000,
  });

  const isReleaseRole = roleType === 'ReleaseNamespace';
  const formSelector = isReleaseRole
    ? 'form[ng-submit="assignRoleToUser(\'ReleaseNamespace\')"]'
    : 'form[ng-submit="assignRoleToUser(\'ModifyNamespace\')"]';
  const selectedUserText = await selectUserByKeyword(page, formSelector, userKeyword);
  const selectedUserId = parseUserIdFromSelect2Text(selectedUserText);
  expect(selectedUserId).toBeTruthy();

  let targetEnv = env;
  if (env) {
    const envSelector = isReleaseRole
      ? `${formSelector} select[ng-model="releaseRoleSelectedEnv"]`
      : `${formSelector} select[ng-model="modifyRoleSelectedEnv"]`;
    const roleEnvSelector = page.locator(envSelector).first();
    await roleEnvSelector.waitFor({ state: 'visible', timeout: 30000 });
    targetEnv = await roleEnvSelector.evaluate((select, preferredEnv) => {
      const envOptions = Array.from(select.options)
        .map((option) => option.value)
        .filter(Boolean);
      if (envOptions.length === 0) {
        return '';
      }
      const selectedEnv = preferredEnv && envOptions.includes(preferredEnv)
        ? preferredEnv
        : envOptions[0];
      select.value = selectedEnv;
      select.dispatchEvent(new Event('change', { bubbles: true }));
      return selectedEnv;
    }, env);
  }

  await clearNamespaceRoleViaPortalApi(page, appId, namespaceName, {
    roleType,
    userId: selectedUserId,
    ...(targetEnv ? { env: targetEnv } : {}),
  });

  const grantRoleResponse = waitForApiResponseByFragments(
    page,
    'POST',
    [
      `/apps/${appId}/`,
      '/namespaces/',
      `/roles/${roleType}`,
      ...(targetEnv ? [`/envs/${targetEnv}/`] : []),
    ]
  );

  await Promise.all([
    grantRoleResponse,
    page.locator(`${formSelector} button[type="submit"]`).first().click(),
  ]);

  await expect(page.locator('.toast-success').first()).toBeVisible({ timeout: 30000 });
  return {
    targetEnv,
    selectedUserId,
    selectedUserText,
  };
}

async function revokeNamespaceRoleViaUi(page, appId, namespaceName, options = {}) {
  const {
    roleType = 'ModifyNamespace',
    userId = USERNAME,
    env = '',
  } = options;
  const namespaceParam = encodePathSegment(namespaceName);
  await page.goto(`/namespace/role.html?#/appid=${appId}&namespaceName=${namespaceParam}`, {
    waitUntil: 'domcontentloaded',
  });
  await page.locator('section.panel[ng-controller="NamespaceRoleController"]').waitFor({
    state: 'visible',
    timeout: 60000,
  });

  const isReleaseRole = roleType === 'ReleaseNamespace';
  const formSelector = isReleaseRole
    ? 'form[ng-submit="assignRoleToUser(\'ReleaseNamespace\')"]'
    : 'form[ng-submit="assignRoleToUser(\'ModifyNamespace\')"]';
  const roleSection = page.locator(formSelector)
    .locator('xpath=ancestor::div[contains(@class,"col-sm-8")]')
    .first();
  const envContainer = env
    ? roleSection.locator('.item-container').filter({
      has: roleSection.locator('h5', {
        hasText: new RegExp(`^\\s*${escapeRegExp(env)}\\s*$`),
      }),
    }).first()
    : roleSection;
  const userRole = envContainer.locator('.btn-group.item-info').filter({ hasText: userId }).first();
  await userRole.waitFor({ state: 'visible', timeout: 30000 });

  const revokeRoleResponse = waitForApiResponseByFragments(
    page,
    'DELETE',
    [
      `/apps/${appId}/`,
      '/namespaces/',
      `/roles/${roleType}`,
      ...(env ? [`/envs/${env}/`] : []),
    ],
    [200, 204, 400, 404]
  );

  await Promise.all([
    revokeRoleResponse,
    userRole.locator('button.dropdown-toggle').first().click(),
  ]);

  await expect(page.locator('.toast-success').first()).toBeVisible({ timeout: 30000 });
}

async function openConfigPage(page, appId, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
    allowBranchView = false,
  } = options;
  const encodedNamespaceName = encodePathSegment(namespaceName);
  await page.evaluate(() => window.localStorage.removeItem('OperateBranch')).catch(() => {});
  await page.goto(
    `/config.html?#/appid=${appId}&env=${env}&cluster=${clusterName}&namespace=${encodedNamespaceName}`,
    { waitUntil: 'domcontentloaded' }
  );

  const publishButton = page.locator('[ng-click="publish(namespace)"]:visible').first();
  const createMissingEnvButton = page.locator('a.list-group-item[ng-click="createAppInMissEnv()"]').first();

  const publishVisible = await publishButton.isVisible().catch(() => false);
  if (!publishVisible) {
    const createMissingEnvVisible = await createMissingEnvButton.isVisible().catch(() => false);
    if (createMissingEnvVisible) {
      const createInMissEnvResponse = waitForApiCall(page, 'POST', `/apps/${appId}/envs/`);
      await createMissingEnvButton.click();
      await createInMissEnvResponse;
    }
  }

  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  const masterPublishButton = namespacePanel.locator('[ng-click="publish(namespace)"]:visible').first();
  const branchPublishButton = namespacePanel.locator('[ng-click="publish(namespace.branch)"]:visible').first();

  const isMasterVisible = await masterPublishButton.isVisible().catch(() => false);
  if (isMasterVisible) {
    return;
  }

  const isBranchVisible = await branchPublishButton.isVisible().catch(() => false);
  if (isBranchVisible && !allowBranchView) {
    await namespacePanel.locator('a[ng-click="switchBranch(\'master\', true)"]').first().click();
  }

  await masterPublishButton.waitFor({
    state: 'visible',
    timeout: 90000,
  });
}

async function switchNamespaceView(page, namespaceName, viewType) {
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  await namespacePanel.locator(`li[ng-click="switchView(namespace, '${viewType}')"]`).first().click();
}

async function editNamespaceTextViaUi(page, appId, configText, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;

  await openConfigPage(page, appId, {
    env,
    clusterName,
    namespaceName,
  });
  await switchNamespaceView(page, namespaceName, 'text');

  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  const toggleEditButton = namespacePanel.locator('[ng-click="toggleTextEditStatus(namespace)"]:visible').first();
  await toggleEditButton.waitFor({ state: 'visible', timeout: 30000 });
  await toggleEditButton.click();

  await namespacePanel.locator('[ng-click="modifyByText(namespace)"]:visible').first().waitFor({
    state: 'visible',
    timeout: 30000,
  });

  await page.evaluate(
    ({ targetNamespaceName, text }) => {
      if (!window.angular) {
        throw new Error('angular is not available on config page');
      }
      const escapeRegExpInBrowser = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const stripped = targetNamespaceName.replace(/\.(properties|yaml|yml|json)$/i, '');
      const candidates = [targetNamespaceName, stripped].filter(Boolean);
      const panels = Array.from(document.querySelectorAll('.panel.namespace-panel'));
      const targetPanel = candidates
        .map((candidate) => {
          const matcher = new RegExp(`^\\s*${escapeRegExpInBrowser(candidate)}\\s*$`, 'i');
          return panels.find((panel) => {
            const namespaceLabel = panel.querySelector('b.namespace-name');
            return namespaceLabel && matcher.test(namespaceLabel.textContent || '');
          });
        })
        .find(Boolean);

      if (!targetPanel) {
        throw new Error(`Unable to locate namespace panel by namespaceName=${targetNamespaceName}`);
      }

      const targetScope = window.angular.element(targetPanel).scope();
      if (!targetScope || !targetScope.namespace) {
        throw new Error('Unable to locate namespace scope for text editing');
      }
      targetScope.namespace.editText = text;
      targetScope.$applyAsync();
    },
    { targetNamespaceName: namespaceName, text: configText }
  );

  const updateItemsResponse = waitForApiResponse(
    page,
    'PUT',
    `/apps/${appId}/envs/${env}/clusters/${clusterName}/namespaces/${namespaceName}/items`
  );

  await Promise.all([
    updateItemsResponse,
    namespacePanel.locator('[ng-click="modifyByText(namespace)"]:visible').first().click(),
  ]);

  await expect(page.locator('.toast-success').first()).toBeVisible({ timeout: 30000 });
}

async function linkPublicNamespacesViaUi(page, appId, namespaceNames) {
  await page.goto(`/namespace.html?#/appid=${appId}`, { waitUntil: 'domcontentloaded' });
  const namespaceForm = page.locator('.apollo-container:not(.hidden) form[name="namespaceForm"]');
  await namespaceForm.waitFor({ state: 'visible', timeout: 60000 });

  await page.locator('.apollo-container:not(.hidden) button[ng-click="switchType(\'link\')"]').first().click();

  await page.waitForFunction(() => {
    return typeof window.$ === 'function'
      && window.$('#namespaces').length > 0
      && window.$('#namespaces').data('select2');
  });

  await page.waitForFunction((targetNamespaces) => {
    const select = window.$('#namespaces');
    if (!select.length || !select.data('select2')) {
      return false;
    }

    targetNamespaces.forEach((targetNamespace) => {
      if (select.find(`option[value="${targetNamespace}"]`).length === 0) {
        select.append(new Option(targetNamespace, targetNamespace, true, true));
      }
    });

    select.val(targetNamespaces).trigger('change');
    return true;
  }, namespaceNames);

  const linkNamespaceResponse = waitForApiResponseByFragments(
    page,
    'POST',
    [`/apps/${appId}/namespaces`]
  );
  await Promise.all([
    linkNamespaceResponse,
    namespaceForm.locator('button[type="submit"]').click(),
  ]);

  await page.locator('div.row.text-center h3').waitFor({ state: 'visible', timeout: 30000 });
  await page.waitForURL((url) => {
    const href = url.toString();
    return href.includes('/namespace/role.html') || href.includes('/config.html');
  }, { timeout: 30000 });
}

async function switchNamespaceBranch(page, namespaceName, targetBranchType = 'master') {
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  if (targetBranchType === 'master') {
    await namespacePanel.locator('a[ng-click="switchBranch(\'master\', true)"]').first().click();
    await namespacePanel.locator('[ng-click="publish(namespace)"]:visible').first().waitFor({
      state: 'visible',
      timeout: 30000,
    });
    return;
  }

  await namespacePanel.locator('a[ng-click="switchBranch(namespace.branchName, true)"]').first().click();
  await namespacePanel.locator('[ng-click="publish(namespace.branch)"]:visible').first().waitFor({
    state: 'visible',
    timeout: 30000,
  });
}

async function createBranchViaUi(page, appId, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;
  const namespacePanel = await locateNamespacePanel(page, namespaceName);

  await namespacePanel.locator('[ng-click="preCreateBranch(namespace)"]:visible').first().click();
  await expect(page.locator('#createBranchTips')).toBeVisible({ timeout: 30000 });

  const createBranchResponse = waitForApiResponseByFragments(
    page,
    'POST',
    [
      `/apps/${appId}/envs/${env}/clusters/${clusterName}/namespaces/${namespaceName}/branches`,
    ]
  );

  await Promise.all([
    createBranchResponse,
    page.locator('#createBranchTips .modal-footer button.btn-primary').first().click(),
  ]);

  const response = await createBranchResponse;
  const body = await response.json().catch(() => null);
  await expect(page.locator('.toast-success').first()).toBeVisible({ timeout: 30000 });
  await namespacePanel.locator('a[ng-click="switchBranch(namespace.branchName, true)"]').first().waitFor({
    state: 'visible',
    timeout: 30000,
  });
  return body?.clusterName;
}

async function addGrayRuleViaUi(page, appId, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
    clientAppId = appId,
    clientIpList = ['1.1.1.1'],
    clientLabelList = [],
  } = options;

  await switchNamespaceBranch(page, namespaceName, 'gray');
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  await namespacePanel.locator('li[ng-click="switchView(namespace.branch, \'rule\')"]').first().click();
  await namespacePanel.locator('[ng-click="addRuleItem(namespace.branch)"]:visible').first().click();
  await expect(page.locator('#rulesModal')).toBeVisible({ timeout: 30000 });

  const clientAppInput = page.locator('#rulesModal input[type="text"]').first();
  if (await clientAppInput.isVisible().catch(() => false)) {
    await clientAppInput.fill(clientAppId);
  }

  if (clientIpList.length > 0) {
    const manualInputToggle = page.locator('#rulesModal a[ng-click*="manual"]').first();
    if (await manualInputToggle.isVisible().catch(() => false)) {
      await manualInputToggle.click();
    }
    const ipTextarea = page.locator('#rulesModal textarea[rows="3"]').first();
    await ipTextarea.waitFor({ state: 'visible', timeout: 30000 });
    await ipTextarea.fill(clientIpList.join(','));
    await page.locator('#rulesModal button.add-rule').first().click();
  }

  if (clientLabelList.length > 0) {
    await page.locator('#rulesModal textarea[rows="1"]').fill(clientLabelList.join(','));
    await page.locator('#rulesModal button.add-rule').nth(1).click();
  }

  const updateGrayRulesResponse = waitForApiResponseByFragments(
    page,
    'PUT',
    [
      `/apps/${appId}/envs/${env}/clusters/${clusterName}/namespaces/${namespaceName}/branches/`,
      '/rules',
    ]
  );

  await Promise.all([
    updateGrayRulesResponse,
    page.locator('#rulesModal .modal-footer button.btn-primary').click(),
  ]);

  await expect(page.locator('#rulesModal')).toBeHidden({ timeout: 30000 });
}

async function grayPublishNamespaceViaUi(page, appId, releaseName, comment, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;

  await switchNamespaceBranch(page, namespaceName, 'gray');
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  await namespacePanel.locator('[ng-click="publish(namespace.branch)"]:visible').first().click();
  await expect(page.locator('#releaseModal')).toBeVisible({ timeout: 30000 });

  await page.fill('#releaseModal input[name="releaseName"]', releaseName);
  await page.fill('#releaseModal textarea[name="comment"]', comment);

  const grayReleaseResponse = waitForApiResponseByFragments(
    page,
    'POST',
    [
      `/apps/${appId}/envs/${env}/clusters/${clusterName}/namespaces/${namespaceName}/branches/`,
      '/releases',
    ]
  );

  await Promise.all([
    grayReleaseResponse,
    page.click('#releaseModal button.btn-primary[type="submit"]'),
  ]);

  await expect(page.locator('.toast-success').first()).toBeVisible({ timeout: 30000 });
}

async function mergeAndPublishNamespaceViaUi(page, appId, releaseName, comment, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
    deleteBranch = true,
  } = options;

  await switchNamespaceBranch(page, namespaceName, 'gray');
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  await namespacePanel.locator('[ng-click="mergeAndPublish(namespace.branch)"]:visible').first().click();
  await expect(page.locator('#mergeAndPublishModal')).toBeVisible({ timeout: 30000 });

  if (!deleteBranch) {
    await page.locator('#mergeAndPublishModal input[name="deleteBranch"]').nth(1).check();
  }

  await page.locator('#mergeAndPublishModal .modal-footer button.btn-primary').click();
  await expect(page.locator('#releaseModal')).toBeVisible({ timeout: 30000 });

  await page.fill('#releaseModal input[name="releaseName"]', releaseName);
  await page.fill('#releaseModal textarea[name="comment"]', comment);

  const mergeReleaseResponse = waitForApiResponseByFragments(
    page,
    'POST',
    [
      `/apps/${appId}/envs/${env}/clusters/${clusterName}/namespaces/${namespaceName}/branches/`,
      '/merge',
    ]
  );

  await Promise.all([
    mergeReleaseResponse,
    page.click('#releaseModal button.btn-primary[type="submit"]'),
  ]);

  await expect(page.locator('.toast-success').first()).toBeVisible({ timeout: 30000 });
}

async function discardGrayBranchViaUi(page, appId, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;

  await switchNamespaceBranch(page, namespaceName, 'gray');
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  await namespacePanel.locator('[ng-click="preDeleteBranch(namespace.branch)"]:visible').first().click();
  await expect(page.locator('#deleteBranchDialog')).toBeVisible({ timeout: 30000 });

  const deleteBranchResponse = waitForApiResponseByFragments(
    page,
    'DELETE',
    [
      `/apps/${appId}/envs/${env}/clusters/${clusterName}/namespaces/${namespaceName}/branches/`,
    ]
  );

  await Promise.all([
    deleteBranchResponse,
    page.click('#deleteBranchDialog button.btn-danger'),
  ]);

  await expect(page.locator('.toast-success').first()).toBeVisible({ timeout: 30000 });
}

async function createNamespaceItem(page, appId, itemKey, value, comment, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  const createItemButton = namespacePanel.locator('[ng-click="createItem(namespace)"]:visible').first();
  await createItemButton.waitFor({ state: 'visible', timeout: 30000 });
  await createItemButton.click();
  await expect(page.locator('#itemModal')).toBeVisible();

  await page.fill('#itemModal input[name="key"]', itemKey);
  await page.fill('#itemModal textarea[name="value"]', value);
  await page.fill('#itemModal textarea[name="comment"]', comment);

  const createItemResponse = waitForApiResponse(
    page,
    'POST',
    `/apps/${appId}/envs/${env}/clusters/${clusterName}/namespaces/${namespaceName}/item`
  );

  await Promise.all([
    createItemResponse,
    page.click('#itemModal button[type="submit"]'),
  ]);

  await expect(page.locator('#itemModal')).toBeHidden();
}

async function createBranchNamespaceItem(page, appId, itemKey, value, comment, options = {}) {
  const {
    env = DEFAULT_ENV,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;
  await switchNamespaceBranch(page, namespaceName, 'gray');
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  const createItemButton = namespacePanel.locator('[ng-click="createItem(namespace.branch)"]:visible').first();
  await createItemButton.waitFor({ state: 'visible', timeout: 30000 });
  await createItemButton.click();
  await expect(page.locator('#itemModal')).toBeVisible();

  await page.fill('#itemModal input[name="key"]', itemKey);
  await page.fill('#itemModal textarea[name="value"]', value);
  await page.fill('#itemModal textarea[name="comment"]', comment);

  const createBranchItemResponse = page.waitForResponse(
    (response) => response.request().method() === 'POST'
      && response.url().includes(`/apps/${appId}/envs/${env}/clusters/`)
      && response.url().includes(`/namespaces/${namespaceName}/item`)
      && isExpectedStatus(response.status(), DEFAULT_SUCCESS_STATUSES),
    { timeout: 90000 }
  );

  await Promise.all([
    createBranchItemResponse,
    page.click('#itemModal button[type="submit"]'),
  ]);

  await expect(page.locator('#itemModal')).toBeHidden();
}

async function updateNamespaceItem(page, appId, itemKey, value, comment, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  const itemRow = namespacePanel.locator('tr').filter({ hasText: itemKey }).first();
  await itemRow.waitFor({ state: 'visible', timeout: 30000 });

  await itemRow.locator('[ng-click="editItem(namespace, config.item)"]:visible').first().click();
  await expect(page.locator('#itemModal')).toBeVisible();

  await page.fill('#itemModal textarea[name="value"]', value);
  await page.fill('#itemModal textarea[name="comment"]', comment);

  const updateResponse = page.waitForResponse(
    (response) => {
      const method = response.request().method();
      return ['PUT', 'POST'].includes(method)
        && response.url().includes(`/apps/${appId}/envs/${env}/clusters/${clusterName}/namespaces/${namespaceName}/item`)
        && isExpectedStatus(response.status(), DEFAULT_SUCCESS_STATUSES);
    },
    { timeout: 90000 }
  );

  await Promise.all([
    updateResponse,
    page.click('#itemModal button[type="submit"]'),
  ]);

  await expect(page.locator('#itemModal')).toBeHidden();
}

async function publishNamespace(page, appId, releaseName, comment, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  await namespacePanel.locator('[ng-click="publish(namespace)"]:visible').first().click();
  await expect(page.locator('#releaseModal')).toBeVisible();

  await page.fill('#releaseModal input[name="releaseName"]', releaseName);
  await page.fill('#releaseModal textarea[name="comment"]', comment);

  const releaseResponse = waitForApiResponse(
    page,
    'POST',
    `/apps/${appId}/envs/${env}/clusters/${clusterName}/namespaces/${namespaceName}/releases`
  );

  await Promise.all([
    releaseResponse,
    page.click('#releaseModal button.btn-primary[type="submit"]'),
  ]);

  await expect(page.locator('.toast-success').first()).toBeVisible({ timeout: 30000 });
}

async function loadNamespaceViaPortalApi(page, appId, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;
  const response = await page.context().request.get(
    `/apps/${encodePathSegment(appId)}/envs/${encodePathSegment(env)}/clusters/${encodePathSegment(clusterName)}/namespaces/${encodePathSegment(namespaceName)}`
  );
  expect(response.status()).toBe(200);
  return response.json();
}

async function modifyNamespaceTextViaPortalApi(page, appId, configText, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
    format = 'properties',
  } = options;
  const namespace = await loadNamespaceViaPortalApi(page, appId, { env, clusterName, namespaceName });
  const namespaceId = namespace?.baseInfo?.id;
  expect(namespaceId).toBeTruthy();

  const response = await page.context().request.put(
    `/apps/${encodePathSegment(appId)}/envs/${encodePathSegment(env)}/clusters/${encodePathSegment(clusterName)}/namespaces/${encodePathSegment(namespaceName)}/items`,
    {
      data: {
        namespaceId,
        format,
        configText,
      },
    }
  );
  expect([200, 204]).toContain(response.status());
}

async function createBranchViaPortalApi(page, appId, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;
  const response = await page.context().request.post(
    `/apps/${encodePathSegment(appId)}/envs/${encodePathSegment(env)}/clusters/${encodePathSegment(clusterName)}/namespaces/${encodePathSegment(namespaceName)}/branches`,
    { data: {} }
  );
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body?.clusterName).toBeTruthy();
  return body.clusterName;
}

async function updateGrayRulesViaPortalApi(page, appId, branchName, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
    clientAppId = appId,
    clientIpList = ['1.1.1.1'],
    clientLabelList = [],
  } = options;
  const response = await page.context().request.put(
    `/apps/${encodePathSegment(appId)}/envs/${encodePathSegment(env)}/clusters/${encodePathSegment(clusterName)}/namespaces/${encodePathSegment(namespaceName)}/branches/${encodePathSegment(branchName)}/rules`,
    {
      data: {
        appId,
        clusterName,
        namespaceName,
        branchName,
        ruleItems: [
          {
            clientAppId,
            clientIpList,
            clientLabelList,
          },
        ],
      },
    }
  );
  expect([200, 204]).toContain(response.status());
}

async function publishGrayReleaseViaPortalApi(page, appId, branchName, releaseTitle, releaseComment, options = {}) {
  const {
    env = DEFAULT_ENV,
    clusterName = DEFAULT_CLUSTER,
    namespaceName = DEFAULT_NAMESPACE,
  } = options;
  const response = await page.context().request.post(
    `/apps/${encodePathSegment(appId)}/envs/${encodePathSegment(env)}/clusters/${encodePathSegment(clusterName)}/namespaces/${encodePathSegment(namespaceName)}/branches/${encodePathSegment(branchName)}/releases`,
    {
      data: {
        releaseTitle,
        releaseComment,
        isEmergencyPublish: false,
      },
    }
  );
  expect([200, 201]).toContain(response.status());
}

async function fetchApolloConfigFromConfigService(request, appId, namespaceName, options = {}) {
  const {
    clusterName = DEFAULT_CLUSTER,
    dataCenter,
    ip = '127.0.0.1',
    label,
  } = options;
  const configBaseUrl = resolveConfigServiceBaseUrl();
  const response = await request.get(
    `${configBaseUrl}/configs/${encodePathSegment(appId)}/${encodePathSegment(clusterName)}/${encodePathSegment(namespaceName)}`,
    {
      params: {
        ...(dataCenter ? { dataCenter } : {}),
        ...(ip ? { ip } : {}),
        ...(label ? { label } : {}),
      },
    }
  );
  let body = null;
  if (response.ok()) {
    body = await response.json();
  }
  return { response, body };
}

async function waitForApolloConfigValue(request, appId, namespaceName, key, expectedValue, options = {}) {
  const timeoutMs = options.timeoutMs || 60000;
  const intervalMs = options.intervalMs || 1500;
  const deadline = Date.now() + timeoutMs;
  let lastStatus = 0;
  let lastValue;

  while (Date.now() < deadline) {
    const { response, body } = await fetchApolloConfigFromConfigService(request, appId, namespaceName, options);
    lastStatus = response.status();
    if (response.ok() && body?.configurations) {
      lastValue = body.configurations[key];
      if (`${lastValue}` === `${expectedValue}`) {
        return body;
      }
    }
    await sleep(intervalMs);
  }

  throw new Error(
    `Timed out waiting for config value. appId=${appId}, namespace=${namespaceName}, key=${key}, expected=${expectedValue}, actual=${lastValue}, status=${lastStatus}`
  );
}

async function fetchRawConfigFromConfigService(request, appId, namespaceName, options = {}) {
  const {
    clusterName = DEFAULT_CLUSTER,
    dataCenter,
    ip = '127.0.0.1',
    label,
  } = options;
  const configBaseUrl = resolveConfigServiceBaseUrl();
  const response = await request.get(
    `${configBaseUrl}/configfiles/raw/${encodePathSegment(appId)}/${encodePathSegment(clusterName)}/${encodePathSegment(namespaceName)}`,
    {
      params: {
        ...(dataCenter ? { dataCenter } : {}),
        ...(ip ? { ip } : {}),
        ...(label ? { label } : {}),
      },
    }
  );
  const text = await response.text();
  return { response, text };
}

async function fetchNotificationsV2FromConfigService(request, appId, notifications, options = {}) {
  const {
    clusterName = DEFAULT_CLUSTER,
    dataCenter,
    ip,
    requestTimeoutMs = 10000,
  } = options;
  const configBaseUrl = resolveConfigServiceBaseUrl();
  const response = await request.get(
    `${configBaseUrl}/notifications/v2`,
    {
      params: {
        appId,
        cluster: clusterName,
        notifications: JSON.stringify(notifications),
        ...(dataCenter ? { dataCenter } : {}),
        ...(ip ? { ip } : {}),
      },
      timeout: requestTimeoutMs,
    }
  );
  const body = await response.json().catch(() => null);
  return { response, body };
}

async function waitForNotificationV2Update(request, appId, namespaceName, previousNotificationId, options = {}) {
  const timeoutMs = options.timeoutMs || 60000;
  const intervalMs = options.intervalMs || 1000;
  const requestTimeoutMs = options.requestTimeoutMs || 10000;
  const deadline = Date.now() + timeoutMs;
  let lastStatus = 0;
  let lastBody = null;

  while (Date.now() < deadline) {
    try {
      const { response, body } = await fetchNotificationsV2FromConfigService(
        request,
        appId,
        [
          {
            namespaceName,
            notificationId: previousNotificationId,
          },
        ],
        {
          ...options,
          requestTimeoutMs,
        }
      );

      lastStatus = response.status();
      lastBody = body;

      if (response.status() === 200 && Array.isArray(body)) {
        const matched = body.find((item) => {
          if (!item || item.notificationId === undefined || item.notificationId === null) {
            return false;
          }
          return normalizeNotificationNamespace(item.namespaceName)
            === normalizeNotificationNamespace(namespaceName);
        });

        if (matched && Number(matched.notificationId) > Number(previousNotificationId)) {
          return matched;
        }
      }
    } catch (error) {
      lastBody = error?.message || `${error}`;
    }

    await sleep(intervalMs);
  }

  throw new Error(
    `Timed out waiting for notifications/v2 update. appId=${appId}, namespace=${namespaceName}, previousNotificationId=${previousNotificationId}, lastStatus=${lastStatus}, lastBody=${JSON.stringify(lastBody)}`
  );
}

async function waitForRawConfig(request, appId, namespaceName, predicate, options = {}) {
  const timeoutMs = options.timeoutMs || 60000;
  const intervalMs = options.intervalMs || 1500;
  const deadline = Date.now() + timeoutMs;
  let lastStatus = 0;
  let lastText = '';

  while (Date.now() < deadline) {
    const { response, text } = await fetchRawConfigFromConfigService(request, appId, namespaceName, options);
    lastStatus = response.status();
    lastText = text;
    if (response.ok() && predicate(text)) {
      return text;
    }
    await sleep(intervalMs);
  }

  throw new Error(
    `Timed out waiting for raw config. appId=${appId}, namespace=${namespaceName}, status=${lastStatus}, body=${lastText.slice(0, 200)}`
  );
}

async function rollbackLatestRelease(page, options = {}) {
  const { namespaceName = DEFAULT_NAMESPACE, env = DEFAULT_ENV } = options;
  const namespacePanel = await locateNamespacePanel(page, namespaceName);
  await namespacePanel.locator('[ng-click="rollback(namespace)"]:visible').first().click();
  await expect(page.locator('#rollbackModal')).toBeVisible({ timeout: 30000 });

  await page.click('#rollbackModal button[type="submit"]');
  await expect(page.locator('#rollbackAlertDialog')).toBeVisible({ timeout: 30000 });

  const rollbackResponse = waitForApiResponse(page, 'PUT', `/envs/${env}/releases/`);
  await Promise.all([
    rollbackResponse,
    page.click('#rollbackAlertDialog button.btn-danger'),
  ]);

  await expect(page.locator('.toast-success').first()).toBeVisible({ timeout: 30000 });
}

module.exports = {
  USERNAME,
  generateUniqueId,
  login,
  selectOrganization,
  selectUserByKeyword,
  createAppViaUiWithUserSelection,
  waitForApiResponse,
  waitForApiCall,
  waitForApiResponseByFragments,
  createAppViaUi,
  submitAppCreation,
  submitClusterCreation,
  createClusterViaUi,
  submitNamespaceCreation,
  createNamespaceViaUi,
  clearNamespaceRoleViaPortalApi,
  assignNamespaceRoleViaUi,
  assignNamespaceRoleViaUiBySearch,
  revokeNamespaceRoleViaUi,
  openConfigPage,
  switchNamespaceView,
  editNamespaceTextViaUi,
  linkPublicNamespacesViaUi,
  switchNamespaceBranch,
  createBranchViaUi,
  addGrayRuleViaUi,
  grayPublishNamespaceViaUi,
  mergeAndPublishNamespaceViaUi,
  discardGrayBranchViaUi,
  createNamespaceItem,
  createBranchNamespaceItem,
  updateNamespaceItem,
  publishNamespace,
  loadNamespaceViaPortalApi,
  modifyNamespaceTextViaPortalApi,
  createBranchViaPortalApi,
  updateGrayRulesViaPortalApi,
  publishGrayReleaseViaPortalApi,
  fetchApolloConfigFromConfigService,
  waitForApolloConfigValue,
  fetchRawConfigFromConfigService,
  fetchNotificationsV2FromConfigService,
  waitForNotificationV2Update,
  waitForRawConfig,
  toPropertiesText,
  rollbackLatestRelease,
};
