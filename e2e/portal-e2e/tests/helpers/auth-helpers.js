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

const DEFAULT_BASE_URL = process.env.BASE_URL || 'http://127.0.0.1:8070';

const MODE_LDAP = 'ldap';
const MODE_OIDC = 'oidc';
const SESSION_COOKIE_NAME = 'SESSION';

function resolveAuthMode() {
  const mode = `${process.env.PORTAL_AUTH_MODE || MODE_LDAP}`.trim().toLowerCase();
  if (mode !== MODE_LDAP && mode !== MODE_OIDC) {
    throw new Error(
      `Unsupported PORTAL_AUTH_MODE=${mode}. Expected one of: ${MODE_LDAP}, ${MODE_OIDC}`
    );
  }
  return mode;
}

function getAuthUsers() {
  const mode = resolveAuthMode();
  if (mode === MODE_OIDC) {
    return {
      successUser: process.env.OIDC_USERNAME || 'apollo',
      successPassword: process.env.OIDC_PASSWORD || 'admin',
      secondaryUser: process.env.OIDC_SECONDARY_USERNAME || 'oidcdev1',
      secondaryPassword: process.env.OIDC_SECONDARY_PASSWORD || 'admin',
      secondaryDisplayName: process.env.OIDC_SECONDARY_DISPLAY_NAME
        || process.env.OIDC_SECONDARY_USERNAME
        || 'oidcdev1',
      secondaryEmail: process.env.OIDC_SECONDARY_EMAIL || 'oidcdev1@example.org',
      invalidUser: process.env.OIDC_INVALID_USERNAME || 'nouser1',
      invalidPassword: process.env.OIDC_INVALID_PASSWORD || 'wrongpass',
      wrongPassword: process.env.OIDC_WRONG_PASSWORD || 'wrongpass',
    };
  }

  return {
    successUser: process.env.PORTAL_USERNAME || process.env.LDAP_ALLOWED_USER || 'apollo',
    successPassword:
      process.env.PORTAL_PASSWORD || process.env.LDAP_ALLOWED_USER_PASSWORD || 'admin',
    secondaryUser: process.env.LDAP_ALLOWED_USER_SECONDARY || 'devops1',
    secondaryPassword: process.env.LDAP_ALLOWED_USER_SECONDARY_PASSWORD || 'admin',
    secondaryDisplayName:
      process.env.LDAP_ALLOWED_USER_SECONDARY_DISPLAY_NAME || 'Dev Ops One',
    secondaryEmail:
      process.env.LDAP_ALLOWED_USER_SECONDARY_EMAIL || 'devops1@example.org',
    blockedUser: process.env.LDAP_BLOCKED_USER || 'blocked1',
    blockedPassword: process.env.LDAP_BLOCKED_USER_PASSWORD || 'admin',
    invalidUser: process.env.LDAP_INVALID_USER || 'nouser1',
    invalidPassword: process.env.LDAP_INVALID_PASSWORD || 'wrongpass',
    wrongPassword: process.env.LDAP_WRONG_PASSWORD || 'wrongpass',
  };
}

function getBaseUrl() {
  return DEFAULT_BASE_URL;
}

async function loginByMode(page, options = {}) {
  const mode = resolveAuthMode();
  if (mode === MODE_OIDC) {
    await loginByOidc(page, options);
    return;
  }
  await loginByForm(page, options);
}

async function expectLoginFailureByMode(page, options = {}) {
  const mode = resolveAuthMode();
  if (mode === MODE_OIDC) {
    await expectOidcLoginFailure(page, options);
    return;
  }
  await expectFormLoginFailure(page, options);
}

async function loginByForm(page, options = {}) {
  const users = getAuthUsers();
  const username = options.username || users.successUser;
  const password = options.password || users.successPassword;

  await page.goto('/login.html', { waitUntil: 'domcontentloaded' });
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="password"]', password);

  await Promise.all([
    page.waitForURL(
      (url) => {
        const value = url.toString();
        return !value.includes('/signin') && !value.includes('login.html');
      },
      { timeout: 60000 }
    ),
    page.click('#login-submit'),
  ]);

  const cookies = await page.context().cookies();
  expect(cookies.some((cookie) => cookie.name === SESSION_COOKIE_NAME)).toBeTruthy();
}

async function expectFormLoginFailure(page, options = {}) {
  const users = getAuthUsers();
  const username = options.username || users.invalidUser;
  const password = options.password || users.invalidPassword;

  await page.goto('/login.html', { waitUntil: 'domcontentloaded' });
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="password"]', password);

  await Promise.all([
    page.waitForURL(
      (url) => {
        const value = url.toString();
        return value.includes('/signin') || value.includes('login.html');
      },
      { timeout: 60000 }
    ),
    page.click('#login-submit'),
  ]);

  await expect(page).toHaveURL(/(signin|login\.html)/);
  await expect(page).toHaveURL(/#\/error/);
}

async function waitForOidcLoginPage(page) {
  await page.waitForURL(
    (url) => {
      const value = url.toString();
      return value.includes('/realms/')
        && (
          value.includes('/protocol/openid-connect/')
          || value.includes('/login-actions/')
        );
    },
    { timeout: 90000 }
  );
  await page.locator('#username').waitFor({ state: 'visible', timeout: 30000 });
  await page.locator('#password').waitFor({ state: 'visible', timeout: 30000 });
}

async function loginByOidc(page, options = {}) {
  const users = getAuthUsers();
  const username = options.username || users.successUser;
  const password = options.password || users.successPassword;

  await page.goto('/', { waitUntil: 'domcontentloaded' });
  await waitForOidcLoginPage(page);
  await page.fill('#username', username);
  await page.fill('#password', password);

  const baseUrl = options.baseUrl || getBaseUrl();
  await Promise.all([
    page.waitForURL(
      (url) => {
        const value = url.toString();
        return value.startsWith(baseUrl) && !value.includes('/login/oauth2/code/');
      },
      { timeout: 90000 }
    ),
    page.click('#kc-login'),
  ]);

  const cookies = await page.context().cookies();
  expect(cookies.some((cookie) => cookie.name === SESSION_COOKIE_NAME)).toBeTruthy();
}

async function expectOidcLoginFailure(page, options = {}) {
  const users = getAuthUsers();
  const username = options.username || users.invalidUser;
  const password = options.password || users.invalidPassword;

  await page.goto('/', { waitUntil: 'domcontentloaded' });
  await waitForOidcLoginPage(page);
  await page.fill('#username', username);
  await page.fill('#password', password);
  await page.click('#kc-login');

  await waitForOidcLoginPage(page);
  const errorSelectors = ['#input-error', '.alert-error', '#kc-error-message', '.kc-feedback-text'];
  const visibleError = page.locator(errorSelectors.join(','));
  await expect(visibleError.first()).toBeVisible({ timeout: 30000 });
}

async function warmUpOidcSecondaryUser(browser, options = {}) {
  if (resolveAuthMode() !== MODE_OIDC) {
    return;
  }

  const users = getAuthUsers();
  const context = await browser.newContext({
    baseURL: options.baseUrl || getBaseUrl(),
  });

  try {
    const page = await context.newPage();
    await loginByOidc(page, {
      username: options.username || users.secondaryUser,
      password: options.password || users.secondaryPassword,
      baseUrl: options.baseUrl,
    });
  } finally {
    await context.close();
  }
}

module.exports = {
  MODE_LDAP,
  MODE_OIDC,
  resolveAuthMode,
  getBaseUrl,
  getAuthUsers,
  loginByMode,
  expectLoginFailureByMode,
  warmUpOidcSecondaryUser,
};
