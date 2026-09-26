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

const directivePath = path.resolve(__dirname,
    '../../../../main/resources/static/scripts/directive/namespace-panel-directive.js');
const source = fs.readFileSync(directivePath, 'utf8');

// Execute the real directive, leaving unrelated service requests pending.
function createPanel(options = {}) {
    const listeners = {};
    const scopeListeners = {};
    const state = { container: null, exitCalls: 0, requestCalls: 0, applyCalls: 0 };
    const fullscreenProperty = options.fullscreenProperty || 'fullscreenElement';
    const document = {
        [fullscreenProperty]: null,
        getElementById: () => state.container,
        addEventListener: (event, callback) => { listeners[event] = callback; },
        removeEventListener: event => { delete listeners[event]; }
    };
    document[options.exitMethod || 'exitFullscreen'] = function () {
        assert.equal(this, document);
        state.exitCalls++;
        document[fullscreenProperty] = null;
        if (options.rejectExit) {
            return Promise.reject(new TypeError('Document not active'));
        }
        if (options.throwOnExit) {
            throw new TypeError('Document not active');
        }
        return options.legacy ? undefined : Promise.resolve();
    };
    const scope = {
        appId: 'test', env: 'LOCAL', cluster: 'default', showBody: true,
        namespace: {
            format: 'properties', items: [],
            baseInfo: { appId: 'test', clusterName: 'default', namespaceName: 'application' }
        },
        $on: (event, callback) => { scopeListeners[event] = callback; },
        $applyAsync: callback => { state.applyCalls++; callback(); }
    };
    const pendingService = new Proxy({}, { get: () => () => ({ then: () => {} }) });
    let factory;
    const context = {
        directive_module: { directive: (name, callback) => { factory = callback; } },
        setTimeout: () => {}
    };
    vm.runInNewContext(source, context, { filename: directivePath });
    factory({ document, addEventListener: () => {}, removeEventListener: () => {},
        setTimeout: () => {} }, {}, {}, { prefixPath: () => '' },
        { EventType: {}, subscribe: () => 1, unsubscribe: () => {} },
        pendingService, pendingService, pendingService, pendingService, pendingService,
        pendingService, pendingService, pendingService).link(scope);

    return {
        state, scope, document,
        mount() {
            state.container = {
                requestFullscreen() {
                    state.requestCalls++;
                    document[fullscreenProperty] = this;
                    return Promise.resolve();
                }
            };
            return state.container;
        },
        fullscreen(element) {
            document[fullscreenProperty] = element;
            listeners.fullscreenchange();
        },
        destroy() {
            scope.$$destroyed = true;
            scopeListeners.$destroy();
        }
    };
}

test('properties namespace initializes before its editor is mounted without exiting fullscreen', () => {
    const panel = createPanel();
    assert.equal(panel.scope.namespace.viewType, 'table');
    assert.equal(panel.scope.namespace.isTextFullscreen, false);
    assert.equal(panel.state.exitCalls, 0);
});

test('fullscreen change with no active element and no editor keeps the namespace out of fullscreen', () => {
    const panel = createPanel();
    panel.fullscreen(null);
    assert.equal(panel.scope.namespace.isTextFullscreen, false);
    assert.equal(panel.state.applyCalls, 0);
});

test('destroying an unmounted namespace does not exit fullscreen', () => {
    const panel = createPanel();
    panel.destroy();
    assert.equal(panel.state.exitCalls, 0);
});

test('switching views or destroying another namespace preserves the active fullscreen element', () => {
    const panel = createPanel();
    panel.mount();
    const otherElement = {};
    panel.fullscreen(otherElement);
    panel.scope.switchView(panel.scope.namespace, 'table');
    panel.destroy();
    assert.equal(panel.state.exitCalls, 0);
    assert.equal(panel.document.fullscreenElement, otherElement);
});

test('toggle enters and exits the current namespace fullscreen', () => {
    const panel = createPanel();
    const editor = panel.mount();
    panel.scope.toggleTextFullscreen(panel.scope.namespace);
    panel.fullscreen(editor);
    assert.equal(panel.state.requestCalls, 1);
    assert.equal(panel.scope.namespace.isTextFullscreen, true);
    panel.scope.toggleTextFullscreen(panel.scope.namespace);
    panel.fullscreen(null);
    assert.equal(panel.state.exitCalls, 1);
    assert.equal(panel.scope.namespace.isTextFullscreen, false);
});

test('text view preserves fullscreen and table view exits it', () => {
    const panel = createPanel();
    panel.fullscreen(panel.mount());
    panel.scope.switchView(panel.scope.namespace, 'text');
    assert.equal(panel.state.exitCalls, 0);
    panel.scope.switchView(panel.scope.namespace, 'table');
    assert.equal(panel.state.exitCalls, 1);
    assert.equal(panel.scope.namespace.viewType, 'table');
});

test('destroying the fullscreen namespace exits fullscreen', () => {
    const panel = createPanel();
    panel.fullscreen(panel.mount());
    panel.destroy();
    assert.equal(panel.state.exitCalls, 1);
});

test('rejected fullscreen exit while switching views is handled and resynchronizes state', async () => {
    const panel = createPanel({ rejectExit: true });
    panel.fullscreen(panel.mount());
    panel.scope.switchView(panel.scope.namespace, 'table');
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(panel.scope.namespace.viewType, 'table');
    assert.equal(panel.scope.namespace.isTextFullscreen, false);
});

test('rejected fullscreen exit during destruction does not update the destroyed scope', async () => {
    const panel = createPanel({ rejectExit: true });
    panel.fullscreen(panel.mount());
    const applyCalls = panel.state.applyCalls;
    panel.destroy();
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(panel.state.applyCalls, applyCalls);
});

test('a synchronous exit failure does not interrupt switching views', () => {
    const panel = createPanel({ throwOnExit: true });
    panel.fullscreen(panel.mount());
    panel.scope.switchView(panel.scope.namespace, 'table');
    assert.equal(panel.scope.namespace.viewType, 'table');
    assert.equal(panel.scope.namespace.isTextFullscreen, false);
});

for (const [fullscreenProperty, exitMethod] of [
    ['webkitFullscreenElement', 'webkitExitFullscreen'],
    ['mozFullScreenElement', 'mozCancelFullScreen'],
    ['msFullscreenElement', 'msExitFullscreen']
]) {
    test(`${exitMethod} supports legacy APIs without a Promise return value`, () => {
        const panel = createPanel({ fullscreenProperty, exitMethod, legacy: true });
        panel.fullscreen(panel.mount());
        panel.scope.switchView(panel.scope.namespace, 'table');
        assert.equal(panel.state.exitCalls, 1);
        assert.equal(panel.document[fullscreenProperty], null);
    });
}
