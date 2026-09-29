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
    '../../../../main/resources/static/scripts/controller/NamespaceController.js'), 'utf8');

function controller() {
    const scope = {}, errors = [], requests = [];
    let factory;
    const pending = () => ({ then: () => {} });
    const context = {
        namespace_module: { controller: (name, definition) => { factory = definition.at(-1); } },
        setTimeout: () => {}, setInterval: () => {}
    };
    vm.runInNewContext(source, context);
    factory(scope, { $$url: '' }, {}, { instant: (key, args) => ({ key, args }) },
        { error: message => errors.push(message) },
        { load: () => ({ then: callback => callback({ orgId: 'TEST1' }) }) },
        { parseParams: () => ({ appid: 'test' }) },
        { findPublicNamespaceNames: pending, createAppNamespace: (...args) => {
            requests.push(args); return pending();
        } }, { has_root_permission: pending }, { getPageSetting: pending });
    return { scope, errors, requests };
}

for (const format of ['properties', 'json', 'yaml', 'yml', 'xml', 'txt']) {
    for (const mode of ['private', 'public', 'public-no-prefix']) {
        test(format + ' ' + mode + ' validates the full 32-character boundary', () => {
            const { scope, errors, requests } = controller();
            scope.appNamespace.format = format;
            scope.appNamespace.isPublic = mode !== 'private';
            scope.appendNamespacePrefix = mode !== 'public-no-prefix';
            const prefix = mode === 'public' ? 'TEST1.' : '';
            const suffix = format === 'properties' ? '' : '.' + format;
            const max = 32 - prefix.length - suffix.length;
            scope.appNamespace.name = 'a'.repeat(max + 1);
            assert.equal(scope.concatNamespace(), prefix + 'a'.repeat(max + 1) + suffix);
            scope.createNamespace();
            assert.equal(requests.length, 0);
            assert.equal(errors.length, 1);
            assert.equal(errors[0].args.suffixLength, suffix.length);
            assert.equal(errors[0].args.departmentLength, prefix.length);
            assert.equal(scope.submitBtnDisabled, false);
            scope.appNamespace.name = 'a'.repeat(max);
            scope.createNamespace();
            assert.equal(requests.length, 1);
            assert.equal(requests[0][1].name, 'a'.repeat(max));
            assert.equal(scope.concatNamespace().length, 32);
        });
    }
}
