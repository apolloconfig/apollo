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
  createAppViaUi,
  createNamespaceViaUi,
  openConfigPage,
  createNamespaceItem,
  updateNamespaceItem,
  publishNamespace,
  rollbackLatestRelease,
  createBranchViaPortalApi,
  modifyNamespaceTextViaPortalApi,
  updateGrayRulesViaPortalApi,
  publishGrayReleaseViaPortalApi,
  waitForApolloConfigValue,
  waitForRawConfig,
  waitForNotificationV2Update,
  toPropertiesText,
} = require('./helpers/portal-helpers');

test.describe.serial('@regression Apollo Portal to ConfigService full chain', () => {
  test('published, gray published and rolled back configs are readable from config service @regression',
    async ({ page, request }) => {
      const appId = generateUniqueId('e2e-cfg-');
      const configKey = generateUniqueId('timeout_');
      const nonGrayClientIp = '2.2.2.2';
      const grayClientIp = '1.1.1.1';
      let notificationId = -1;

      await login(page);
      await createAppViaUi(page, appId);

      await openConfigPage(page, appId);
      await createNamespaceItem(page, appId, configKey, '100', 'e2e config service first value');
      await publishNamespace(page, appId, generateUniqueId('release_'), 'e2e config service first publish');
      await waitForApolloConfigValue(request, appId, 'application', configKey, '100', {
        ip: nonGrayClientIp,
      });
      const firstNotification = await waitForNotificationV2Update(
        request,
        appId,
        'application',
        notificationId,
        { ip: nonGrayClientIp }
      );
      notificationId = Number(firstNotification.notificationId);

      await openConfigPage(page, appId);
      await updateNamespaceItem(page, appId, configKey, '200', 'e2e config service second value');
      await publishNamespace(page, appId, generateUniqueId('release_'), 'e2e config service second publish');
      await waitForApolloConfigValue(request, appId, 'application', configKey, '200', {
        ip: nonGrayClientIp,
      });
      const secondNotification = await waitForNotificationV2Update(
        request,
        appId,
        'application',
        notificationId,
        { ip: nonGrayClientIp }
      );
      expect(Number(secondNotification.notificationId)).toBeGreaterThan(notificationId);
      notificationId = Number(secondNotification.notificationId);

      const branchName = await createBranchViaPortalApi(page, appId, {
        namespaceName: 'application',
      });
      await modifyNamespaceTextViaPortalApi(
        page,
        appId,
        toPropertiesText({ [configKey]: '300' }),
        {
          clusterName: branchName,
          namespaceName: 'application',
          format: 'properties',
        }
      );
      await updateGrayRulesViaPortalApi(page, appId, branchName, {
        namespaceName: 'application',
        clientAppId: appId,
        clientIpList: [grayClientIp],
      });
      await publishGrayReleaseViaPortalApi(
        page,
        appId,
        branchName,
        generateUniqueId('gray_'),
        'e2e gray publish',
        { namespaceName: 'application' }
      );

      await waitForApolloConfigValue(request, appId, 'application', configKey, '300', {
        ip: grayClientIp,
      });
      await waitForApolloConfigValue(request, appId, 'application', configKey, '200', {
        ip: nonGrayClientIp,
      });
      const grayNotification = await waitForNotificationV2Update(
        request,
        appId,
        'application',
        notificationId,
        { ip: grayClientIp }
      );
      expect(Number(grayNotification.notificationId)).toBeGreaterThan(notificationId);
      notificationId = Number(grayNotification.notificationId);

      await openConfigPage(page, appId);
      await rollbackLatestRelease(page);

      await waitForApolloConfigValue(request, appId, 'application', configKey, '100', {
        ip: nonGrayClientIp,
      });
      await waitForApolloConfigValue(request, appId, 'application', configKey, '300', {
        ip: grayClientIp,
      });
      const rollbackNotification = await waitForNotificationV2Update(
        request,
        appId,
        'application',
        notificationId,
        { ip: nonGrayClientIp }
      );
      expect(Number(rollbackNotification.notificationId)).toBeGreaterThan(notificationId);
    });

  test('properties, yaml and json namespaces are readable from config service @regression',
    async ({ page, request }) => {
      const appId = generateUniqueId('e2e-fmt-');
      const propertiesNamespaceSeed = generateUniqueId('props_');
      const yamlNamespaceSeed = generateUniqueId('yaml_');
      const jsonNamespaceSeed = generateUniqueId('json_');
      const propertiesKey = generateUniqueId('rate_');
      const notificationIds = {};

      await login(page);
      await createAppViaUi(page, appId);

      const propertiesNamespace = await createNamespaceViaUi(page, appId, propertiesNamespaceSeed, {
        format: 'properties',
      });
      notificationIds[propertiesNamespace] = -1;
      const yamlNamespace = await createNamespaceViaUi(page, appId, yamlNamespaceSeed, {
        format: 'yaml',
      });
      notificationIds[yamlNamespace] = -1;
      const jsonNamespace = await createNamespaceViaUi(page, appId, jsonNamespaceSeed, {
        format: 'json',
      });
      notificationIds[jsonNamespace] = -1;

      await openConfigPage(page, appId, { namespaceName: propertiesNamespace });
      await createNamespaceItem(
        page,
        appId,
        propertiesKey,
        '101',
        'e2e properties namespace value',
        { namespaceName: propertiesNamespace }
      );
      await publishNamespace(
        page,
        appId,
        generateUniqueId('release_'),
        'e2e properties namespace publish',
        { namespaceName: propertiesNamespace }
      );
      await waitForApolloConfigValue(request, appId, propertiesNamespace, propertiesKey, '101');
      const propertiesNotification = await waitForNotificationV2Update(
        request,
        appId,
        propertiesNamespace,
        notificationIds[propertiesNamespace]
      );
      notificationIds[propertiesNamespace] = Number(propertiesNotification.notificationId);

      const yamlText = 'limits:\n  qps: 300\nenabled: true\n';
      await modifyNamespaceTextViaPortalApi(page, appId, yamlText, {
        namespaceName: yamlNamespace,
        format: 'yaml',
      });
      await openConfigPage(page, appId, { namespaceName: yamlNamespace });
      await publishNamespace(
        page,
        appId,
        generateUniqueId('release_'),
        'e2e yaml namespace publish',
        { namespaceName: yamlNamespace }
      );
      const yamlRaw = await waitForRawConfig(
        request,
        appId,
        yamlNamespace,
        (raw) => raw.includes('qps: 300') && raw.includes('enabled: true')
      );
      expect(yamlRaw).toContain('limits:');
      const yamlNotification = await waitForNotificationV2Update(
        request,
        appId,
        yamlNamespace,
        notificationIds[yamlNamespace]
      );
      notificationIds[yamlNamespace] = Number(yamlNotification.notificationId);

      const jsonText = '{"limits":{"qps":500},"enabled":true}';
      await modifyNamespaceTextViaPortalApi(page, appId, jsonText, {
        namespaceName: jsonNamespace,
        format: 'json',
      });
      await openConfigPage(page, appId, { namespaceName: jsonNamespace });
      await publishNamespace(
        page,
        appId,
        generateUniqueId('release_'),
        'e2e json namespace publish',
        { namespaceName: jsonNamespace }
      );
      const jsonRaw = await waitForRawConfig(
        request,
        appId,
        jsonNamespace,
        (raw) => {
          try {
            const parsed = JSON.parse(raw);
            return parsed?.limits?.qps === 500 && parsed.enabled === true;
          } catch (error) {
            return false;
          }
        }
      );
      const parsedJson = JSON.parse(jsonRaw);
      expect(parsedJson.limits.qps).toBe(500);
      expect(parsedJson.enabled).toBeTruthy();
      const jsonNotification = await waitForNotificationV2Update(
        request,
        appId,
        jsonNamespace,
        notificationIds[jsonNamespace]
      );
      notificationIds[jsonNamespace] = Number(jsonNotification.notificationId);
      expect(notificationIds[jsonNamespace]).toBeGreaterThan(-1);
    });
});
