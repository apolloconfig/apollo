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

const staticRoot = path.resolve(__dirname, '../../../../main/resources/static');
const source = fs.readFileSync(path.join(staticRoot,
    'scripts/controller/ConfigExportController.js'), 'utf8');
const template = fs.readFileSync(path.join(staticRoot, 'config_export.html'), 'utf8');

function controller() {
    const scope = {}, requests = [];
    let factory;
    vm.runInNewContext(source, {
        config_export_module: { controller: (name, definition) => { factory = definition.at(-1); } },
        $: () => ({ removeClass: () => {} }),
        document: { getElementById: () => ({ files: [{ name: 'configs.zip' }] }) },
        FormData: class { append() {} },
        angular: { identity: value => value }
    });
    const http = request => {
        requests.push(request);
        return { success: () => ({ error: () => {} }) };
    };
    factory(scope, {}, {}, http, { instant: key => key },
        { info: () => {}, warning: () => {}, success: () => {}, error: () => {} }, {},
        { find_all_envs: () => ({ then: callback => callback(['LOCAL']) }) }, {}, {},
        { prefixPath: () => '/portal' });
    scope.importEnvs[0].checked = true;
    scope.cluster = { appId: 'test-app', env: 'LOCAL', name: 'default', info: 'test cluster' };

    const pane = id => {
        const start = template.indexOf('id="' + id + '"');
        assert.notEqual(start, -1);
        const end = id === 'env_config' ? template.indexOf('id="app_config"', start) : template.length;
        const html = template.slice(start, end);
        // ng-if creates a prototypically inherited scope for the environment pane.
        const pageScope = id === 'env_config' ? Object.create(scope) : scope;
        const radios = [...html.matchAll(/<input\b[^>]*type="radio"[^>]*>/g)];
        assert.equal(radios.length, 2);
        const model = radios[0][0].match(/ng-model="([^"]+)"/)[1];
        return {
            select: value => {
                const radio = radios.find(match => match[0].includes('value="' + value + '"'));
                assert.ok(radio);
                const expression = radio[0].match(/ng-model="([^"]+)"/)[1];
                vm.runInNewContext('scope.' + expression + ' = value', { scope: pageScope, value });
            },
            selected: () => vm.runInNewContext('scope.' + model, { scope: pageScope }),
            submit: () => id === 'env_config' ? pageScope.import() : pageScope.importAppConfig()
        };
    };
    return {
        pane,
        env: pane('env_config'),
        app: pane('app_config'),
        requests,
        lastAction: () => new URL(requests.at(-1).url, 'http://localhost').searchParams.get('conflictAction')
    };
}

for (const pane of ['env', 'app']) {
    test(pane + ' import defaults to skipping existing namespaces', () => {
        const page = controller();
        assert.equal(page[pane].selected(), 'ignore');
        page[pane].submit();
        assert.equal(page.lastAction(), 'ignore');
    });
}

test('environment cover selection reaches the request from its child scope', () => {
    const page = controller();
    page.env.select('cover');
    page.env.submit();
    assert.equal(page.lastAction(), 'cover');
    assert.equal(page.requests[0].url, '/portal/openapi/v1/configs/import?envs=LOCAL&conflictAction=cover');
});

test('changing environment cover back to skip uses the latest visible choice', () => {
    const page = controller();
    page.env.select('cover');
    page.env.submit();
    assert.equal(page.lastAction(), 'cover');
    page.env.select('ignore');
    page.env.submit();
    assert.equal(page.lastAction(), 'ignore');
});

test('switching between application and environment panes keeps selection and submission consistent', () => {
    const page = controller();
    page.app.select('cover');
    page.app.submit();
    assert.equal(page.lastAction(), 'cover');
    assert.equal(page.env.selected(), 'cover');
    page.env.select('ignore');
    page.env.submit();
    assert.equal(page.lastAction(), 'ignore');
    assert.equal(page.app.selected(), 'ignore');
    page.app.submit();
    assert.equal(page.lastAction(), 'ignore');
    page.env.select('cover');
    assert.equal(page.app.selected(), 'cover');
    page.app.submit();
    assert.equal(page.lastAction(), 'cover');
});

test('recreating the environment pane retains the selected strategy', () => {
    const page = controller();
    page.env.select('cover');
    const recreated = page.pane('env_config');
    assert.equal(recreated.selected(), 'cover');
    recreated.submit();
    assert.equal(page.lastAction(), 'cover');
});
