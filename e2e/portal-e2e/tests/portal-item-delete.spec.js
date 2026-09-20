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
const { generateUniqueId } = require('./helpers/portal-helpers');

const USERNAME = process.env.PORTAL_USERNAME || 'apollo';
const PASSWORD = process.env.PORTAL_PASSWORD || 'admin';
const ITEMS = [
  { kind: 'plain', key: 'logging', value: 'plain-value', resource: 'items/logging' },
  { kind: 'slash', key: 'logging/level', value: 'slash-value', resource: 'encodedItems/bG9nZ2luZy9sZXZlbA' },
  { kind: 'backslash', key: 'logging\\level', value: 'backslash-value', resource: 'encodedItems/bG9nZ2luZ1xsZXZlbA' },
];

// Token creation responses contain credentials and must not be saved in traces.
test.use({ trace: 'off' });

test.describe('Apollo OpenAPI item deletion', () => {
  for (const target of ITEMS) {
    test(`user-token deletes a ${target.kind} key without affecting other keys @regression`, async ({ request }) => {
      const appId = generateUniqueId('e2e-delete-');
      const namespaceUrl = `/openapi/v1/envs/LOCAL/apps/${appId}/clusters/default/namespaces/application`;
      const adminHeaders = {
        Authorization: `Basic ${Buffer.from(`${USERNAME}:${PASSWORD}`).toString('base64')}`,
      };
      let tokenId;

      try {
        const createApp = await request.post('/openapi/v1/apps', {
          headers: adminHeaders,
          data: {
            assignAppRoleToSelf: true,
            admins: [USERNAME],
            app: {
              appId,
              name: appId,
              orgId: 'TEST1',
              orgName: 'Sample Department 1',
              ownerName: USERNAME,
              ownerEmail: 'apollo@localhost',
            },
          },
        });
        expect(createApp.status(), 'create isolated test app').toBe(200);

        const createToken = await request.post('/openapi/v1/user-tokens', {
          headers: adminHeaders,
          data: {
            name: appId,
            operations: ['config:read', 'config:modify'],
            appIds: [appId],
            envs: ['LOCAL'],
            expires: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
          },
        });
        expect(createToken.status(), 'create scoped user token').toBe(200);
        const token = await createToken.json();
        tokenId = token.id;
        const headers = { Authorization: `Bearer ${token.tokenValue}` };

        for (const item of ITEMS) {
          const createItem = await request.post(`${namespaceUrl}/items`, {
            headers,
            data: { key: item.key, value: item.value },
          });
          expect(createItem.status(), `create ${item.kind} key`).toBe(200);

          const readItem = await request.get(`${namespaceUrl}/${item.resource}`, { headers });
          expect(readItem.status(), `read ${item.kind} key before deletion`).toBe(200);
          expect(await readItem.json()).toMatchObject({ key: item.key, value: item.value });
        }

        // No operator is supplied: both routes must resolve it from the user token.
        const deleteItem = await request.delete(`${namespaceUrl}/${target.resource}`, { headers });
        expect(deleteItem.status(), `delete ${target.kind} key without operator`).toBe(200);

        const deletedItem = await request.get(`${namespaceUrl}/${target.resource}`, { headers });
        expect(deletedItem.status(), 'deleted key must no longer be readable').toBe(404);

        const remaining = ITEMS.filter((item) => item.key !== target.key);
        for (const item of remaining) {
          const readItem = await request.get(`${namespaceUrl}/${item.resource}`, { headers });
          expect(readItem.status(), `preserve ${item.kind} key`).toBe(200);
          expect(await readItem.json()).toMatchObject({ key: item.key, value: item.value });
        }

        const listItems = await request.get(`${namespaceUrl}/items`, {
          headers,
          params: { page: 0, size: 50 },
        });
        expect(listItems.status(), 'list remaining keys').toBe(200);
        const page = await listItems.json();
        expect(page.content.map((item) => item.key).sort())
          .toEqual(remaining.map((item) => item.key).sort());
      } finally {
        // App deletion also cleans up keys when the encoded DELETE regression fails.
        try {
          const cleanup = await request.delete(`/openapi/v1/apps/${appId}`, { headers: adminHeaders });
          expect([200, 404], 'remove isolated test app').toContain(cleanup.status());
        } finally {
          if (tokenId !== undefined) {
            const revoke = await request.post(`/openapi/v1/user-tokens/${tokenId}/revoke`, {
              headers: adminHeaders,
            });
            expect(revoke.status(), 'revoke test user token').toBe(200);
          }
        }
      }
    });
  }
});
