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
const { defineConfig, devices } = require('@playwright/test');

const retries = Number(process.env.PLAYWRIGHT_RETRIES || '0');
const workers = Number(process.env.PLAYWRIGHT_WORKERS || (process.env.CI ? '2' : '1'));

module.exports = defineConfig({
  testDir: './tests',
  workers,
  retries,
  timeout: 180000,
  expect: {
    timeout: 20000,
  },
  reporter: process.env.CI ? [['line'], ['html', { open: 'never' }]] : [['list']],
  use: {
    baseURL: process.env.BASE_URL || 'http://127.0.0.1:8070',
    headless: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
      },
    },
  ],
});
