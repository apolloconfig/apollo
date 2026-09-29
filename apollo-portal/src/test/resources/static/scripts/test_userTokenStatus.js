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
 */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const vm = require('node:vm');
const source = fs.readFileSync(path.resolve(__dirname,
    '../../../../main/resources/static/scripts/services/UserTokenService.js'), 'utf8');
let formatter;
vm.runInNewContext(source, { appService: { service: (name, definition) => {
    if (name === 'UserTokenFormatterService') {
        formatter = definition.at(-1)({ instant: key => key });
    }
} } });

for (const [status, label, css] of [
    ['active', 'UserToken.Active', 'label-primary'],
    ['revoked', 'UserToken.RevokedStatus', 'label-default'],
    ['expired', 'UserToken.ExpiredStatus', 'label-warning']
]) {
    for (const value of [status, status.toUpperCase()]) {
        test('normalizes OpenAPI and legacy token status ' + value, () => {
            const token = { status: value };
            assert.equal(formatter.tokenStatus(token), status);
            assert.equal(formatter.statusLabel(token), label);
            assert.equal(formatter.statusClass(token), css);
        });
    }
}

test('derives status for legacy responses without an explicit status', () => {
    assert.equal(formatter.tokenStatus({ revokedAt: '2026-01-01' }), 'revoked');
    assert.equal(formatter.tokenStatus({ expires: '2000-01-01' }), 'expired');
    assert.equal(formatter.tokenStatus({ expires: '2999-01-01' }), 'active');
});
