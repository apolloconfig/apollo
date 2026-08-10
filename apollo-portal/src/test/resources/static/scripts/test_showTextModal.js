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
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const directivePath = path.join(__dirname, '../../../../main/resources/static/scripts/directive',
    'show-text-modal-directive.js');
const templatePath = path.join(__dirname, '../../../../main/resources/static/views/component',
    'show-text-modal.html');

const directiveModule = {
    directive: function () {
        return {
            filter: function () {
                return this;
            }
        };
    }
};

const context = {
    directive_module: directiveModule
};
vm.runInNewContext(fs.readFileSync(directivePath, 'utf8'), context, {
    filename: directivePath
});

const directiveDefinition = context.showTextModalDirective({
    prefixPath: function () {
        return '';
    },
    hasDuplicateKeys: function (text) {
        return text === '{"a":1,"a":2}';
    }
});

function createScope() {
    const watchers = {};
    const scope = {
        $watch: function (name, callback) {
            watchers[name] = callback;
        }
    };
    directiveDefinition.link(scope);
    return {
        scope: scope,
        refresh: function () {
            watchers.text();
        }
    };
}

function runTests() {
    const formatted = createScope();
    formatted.scope.text = '{\n  "a": "b"\n}';
    formatted.refresh();
    assert.strictEqual(formatted.scope.canFormat, true);
    assert.strictEqual(formatted.scope.viewMode, 'formatted');
    formatted.scope.setViewMode('raw');
    assert.strictEqual(formatted.scope.viewMode, 'raw');
    assert.strictEqual(formatted.scope.text, '{\n  "a": "b"\n}');

    const plainText = createScope();
    plainText.scope.text = '{"a":1,"a":2}';
    plainText.refresh();
    assert.strictEqual(plainText.scope.canFormat, false);
    assert.strictEqual(plainText.scope.viewMode, 'raw');

    const template = fs.readFileSync(templatePath, 'utf8');
    assert.ok(template.includes('Component.ShowText.FormattedValue'));
    assert.ok(template.includes('Component.ShowText.RawValue'));
    assert.ok(template.includes("viewMode == 'formatted'"));
    assert.ok(template.includes("viewMode == 'raw'"));

    console.log('All show-text modal tests passed.');
}

runTests();
