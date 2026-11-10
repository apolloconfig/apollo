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

const staticRoot = path.resolve(__dirname, '../../../../main/resources/static');
const directivePath = path.join(staticRoot,
    'scripts/directive/open-manage-view-permissions-modal-directive.js');
const directiveSource = fs.readFileSync(directivePath, 'utf8');
const template = fs.readFileSync(path.join(staticRoot, 'open/view-permissions-modal.html'), 'utf8');

function deferred() {
    let resolve;
    let reject;
    const promise = new Promise((accept, fail) => {
        resolve = accept;
        reject = fail;
    });
    return {promise, resolve, reject};
}

function createModal() {
    let factory;
    vm.runInNewContext(directiveSource, {
        directive_module: {
            directive: (name, callback) => {
                assert.strictEqual(name, 'viewpermissionsmodal');
                factory = callback;
            }
        }
    });
    const loads = [];
    const revokes = [];
    const errors = [];
    const successes = [];
    let watcher;
    const scope = {
        consumer: {consumerId: 1, appId: 'consumer-app'},
        $watch: (name, callback) => {
            assert.strictEqual(name, 'consumer');
            watcher = callback;
        }
    };
    factory({instant: key => key}, {success: key => successes.push(key)}, {
        prefixPath: () => '',
        showErrorMsg: (response, key) => errors.push({response, key})
    }, {
        getConsumerRoles: consumerId => {
            const request = deferred();
            loads.push({consumerId, request});
            return request.promise;
        },
        revokeConsumerRoles: (consumerId, role) => {
            const request = deferred();
            revokes.push({consumerId, role, request});
            return request.promise;
        }
    }).link(scope);
    watcher();
    return {scope, loads, revokes, errors, successes, changed: () => watcher()};
}

function testServiceParameters() {
    let service;
    let lastRequest;
    let actions;
    const source = fs.readFileSync(path.join(staticRoot, 'scripts/services/ConsumerService.js'), 'utf8');
    vm.runInNewContext(source, {
        appService: {
            service: (name, injection) => {
                service = injection[injection.length - 1]((url, defaults, definitions) => {
                    actions = definitions;
                    const resource = {};
                    Object.keys(definitions).forEach(key => {
                        resource[key] = {key};
                    });
                    return resource;
                }, {}, {
                    prefixPath: () => '/portal',
                    ajax: (action, params, body) => {
                        lastRequest = {action: action.key, params, body};
                    }
                });
            }
        }
    });
    service.getConsumerRoles(42);
    assert.strictEqual(lastRequest.action, 'get_consumer_roles');
    assert.strictEqual(lastRequest.params.consumerId, 42);
    assert.strictEqual(actions.get_consumer_roles.method, 'GET');
    assert.strictEqual(actions.get_consumer_roles.url,
        '/portal/openapi/v1/consumers/:consumerId/roles');

    service.revokeConsumerRoles(42, {type: 'NamespaceRole', appId: 'app',
        namespaceName: 'apollo.xml', env: 'DEV'});
    assert.strictEqual(actions.revoke_consumer_roles.method, 'DELETE');
    assert.strictEqual(lastRequest.params.consumerId, 42);
    assert.strictEqual(lastRequest.params.namespaceName, 'apollo.xml');
    assert.strictEqual(lastRequest.params.env, 'DEV');

    service.revokeConsumerRoles(42, {type: 'NamespaceRole', appId: 'app',
        namespaceName: 'application', env: null});
    assert.strictEqual(lastRequest.params.env, undefined);
    service.revokeConsumerRoles(42, {type: 'AppRole', appId: 'app',
        namespaceName: 'ignored', env: 'DEV'});
    assert.strictEqual(lastRequest.params.namespaceName, undefined);
    assert.strictEqual(lastRequest.params.env, undefined);
}

async function testConfirmationAndSuccessfulRevoke() {
    const modal = createModal();
    const role = {type: 'NamespaceRole', appId: 'app', namespaceName: 'application', env: null};
    assert.strictEqual(modal.scope.loading, true);
    modal.loads[0].request.resolve([role]);
    await Promise.resolve();
    assert.strictEqual(modal.scope.loading, false);
    assert.strictEqual(modal.scope.roles[0], role);

    modal.scope.revokeRole();
    modal.scope.preRevokeRole(role);
    assert.strictEqual(modal.revokes.length, 0);
    modal.scope.cancelRevokeRole();
    assert.strictEqual(modal.scope.pendingRole, null);
    modal.scope.preRevokeRole(role);
    modal.scope.revokeRole();
    modal.scope.revokeRole();
    modal.scope.cancelRevokeRole();
    assert.strictEqual(modal.revokes.length, 1);
    assert.strictEqual(modal.scope.revoking, true);
    assert.strictEqual(modal.scope.pendingRole, role);
    assert.strictEqual(modal.revokes[0].consumerId, 1);
    modal.revokes[0].request.resolve();
    await Promise.resolve();
    assert.strictEqual(modal.scope.pendingRole, null);
    assert.strictEqual(modal.scope.revoking, false);
    assert.strictEqual(modal.loads.length, 2);
    assert.deepStrictEqual(modal.successes, ['Open.Manage.RevokePermission.Success']);
    modal.loads[1].request.resolve([]);
    await Promise.resolve();
    assert.strictEqual(modal.scope.roles.length, 0);
}

async function testFailuresAndRetry() {
    const modal = createModal();
    modal.loads[0].request.reject({status: 500});
    await Promise.resolve();
    assert.strictEqual(modal.scope.loadError, true);
    assert.strictEqual(modal.scope.loading, false);
    assert.strictEqual(modal.errors[0].key, 'Open.Manage.ViewPermissions.Failed');
    modal.scope.reloadRoles();
    const role = {type: 'AppRole', appId: 'app'};
    modal.loads[1].request.resolve([role]);
    await Promise.resolve();
    assert.strictEqual(modal.scope.loadError, false);
    modal.scope.preRevokeRole(role);
    modal.scope.revokeRole();
    modal.revokes[0].request.reject({status: 403});
    await Promise.resolve();
    assert.strictEqual(modal.scope.revoking, false);
    assert.strictEqual(modal.scope.pendingRole, role);
    assert.strictEqual(modal.errors[1].key, 'Open.Manage.RevokePermission.Failed');
    assert.strictEqual(modal.successes.length, 0);
}

async function testStaleResponsesDoNotReplaceAnotherConsumer() {
    const modal = createModal();
    modal.scope.consumer = {consumerId: 2, appId: 'other-consumer'};
    modal.changed();
    const currentRole = {type: 'AppRole', appId: 'other-app'};
    modal.loads[1].request.resolve([currentRole]);
    await Promise.resolve();
    modal.loads[0].request.resolve([{type: 'AppRole', appId: 'stale-app'}]);
    await Promise.resolve();
    assert.strictEqual(modal.scope.roles[0], currentRole);
    assert.strictEqual(modal.scope.loading, false);
}

async function testStaleRevokeDoesNotClearNewerConfirmation() {
    const modal = createModal();
    const firstRole = {type: 'NamespaceRole', appId: 'app', namespaceName: 'application', env: null};
    modal.loads[0].request.resolve([firstRole]);
    await Promise.resolve();

    modal.scope.preRevokeRole(firstRole);
    modal.scope.revokeRole();
    assert.strictEqual(modal.revokes.length, 1);
    assert.strictEqual(modal.scope.revoking, true);

    modal.scope.consumer = {consumerId: 2, appId: 'other-consumer'};
    modal.changed();
    const secondRole = {type: 'AppRole', appId: 'other-app'};
    modal.loads[1].request.resolve([secondRole]);
    await Promise.resolve();
    modal.scope.preRevokeRole(secondRole);

    modal.revokes[0].request.resolve();
    await Promise.resolve();

    assert.deepStrictEqual(modal.successes, []);
    assert.strictEqual(modal.scope.pendingRole, secondRole);
    assert.strictEqual(modal.scope.revoking, false);
    assert.strictEqual(modal.loads.length, 2);
}

async function testReopeningConsumerInvalidatesInFlightRevoke() {
    const modal = createModal();
    const role = {type: 'AppRole', appId: 'app'};
    const otherRole = {type: 'NamespaceRole', appId: 'app', namespaceName: 'other', env: 'DEV'};
    modal.loads[0].request.resolve([role, otherRole]);
    await Promise.resolve();

    modal.scope.preRevokeRole(role);
    modal.scope.revokeRole();
    modal.changed();
    // 重开时列表先于撤销返回，此时已撤销的授权仍会在列表中
    modal.loads[1].request.resolve([role, otherRole]);
    await Promise.resolve();
    assert.deepStrictEqual(modal.scope.roles, [role, otherRole]);

    modal.revokes[0].request.resolve();
    await Promise.resolve();

    // 仍然是旧的上下文，不弹成功提示，但要为同一个 consumer 重新拉一次列表
    assert.deepStrictEqual(modal.successes, []);
    assert.strictEqual(modal.scope.revoking, false);
    assert.strictEqual(modal.loads.length, 3);
    assert.strictEqual(modal.loads[2].consumerId, 1);

    modal.loads[2].request.resolve([otherRole]);
    await Promise.resolve();
    assert.deepStrictEqual(modal.scope.roles, [otherRole]);
    assert.ok(!modal.scope.roles.some(r => r === role));
}

async function testReopeningAnotherConsumerDoesNotRefresh() {
    const modal = createModal();
    const role = {type: 'AppRole', appId: 'app'};
    modal.loads[0].request.resolve([role]);
    await Promise.resolve();

    modal.scope.preRevokeRole(role);
    modal.scope.revokeRole();

    modal.scope.consumer = {consumerId: 2, appId: 'other-consumer'};
    modal.changed();
    const otherRole = {type: 'AppRole', appId: 'other-app'};
    modal.loads[1].request.resolve([otherRole]);
    await Promise.resolve();

    modal.revokes[0].request.resolve();
    await Promise.resolve();

    // 列表已切换到另一个 consumer，旧撤销完成时不应再触发刷新
    assert.deepStrictEqual(modal.successes, []);
    assert.strictEqual(modal.loads.length, 2);
    assert.deepStrictEqual(modal.scope.roles, [otherRole]);
}

function testTemplateAndTranslations() {
    assert.ok(template.includes('ng-click="preRevokeRole(role)"'));
    assert.ok(template.includes('ng-if="pendingRole"'));
    assert.ok(template.includes('ng-click="revokeRole()" ng-disabled="revoking"'));
    assert.ok(template.includes('ng-click="cancelRevokeRole()"'));
    assert.ok(template.includes('Open.Manage.ConsumerRole.AllEnvs'));
    assert.ok(template.includes('Open.Manage.RevokePermission.OverlapWarning'));
    const keys = new Set((directiveSource + template).match(/Open\.Manage\.[A-Za-z.]+/g));
    for (const language of ['en', 'zh-CN']) {
        const translations = JSON.parse(fs.readFileSync(
            path.join(staticRoot, 'i18n', language + '.json'), 'utf8'));
        for (const key of keys) {
            assert.ok(translations[key], language + ' translation missing: ' + key);
        }
    }
    const page = fs.readFileSync(path.join(staticRoot, 'open/manage.html'), 'utf8');
    assert.ok(page.includes('ng-click="preViewPermissions(consumer)"'));
    assert.ok(page.includes('<viewpermissionsmodal consumer="permissionConsumer">'));
    assert.ok(page.includes('open-manage-view-permissions-modal-directive.js'));
}

async function testGrantEnvironmentSelection() {
    let factory;
    const source = fs.readFileSync(path.join(staticRoot,
        'scripts/directive/open-manage-grant-permission-modal-directive.js'), 'utf8');
    vm.runInNewContext(source, {
        directive_module: {directive: (name, callback) => { factory = callback; }}
    });
    const envRequest = deferred();
    let assignedEnvs;
    const scope = {consumerRole: {token: 'test-token', type: 'NamespaceRole',
        appId: 'app', namespaceName: 'application'}};
    factory({instant: key => key}, {success: () => {}}, {}, {
        prefixPath: () => '', hideModal: () => {}, showErrorMsg: () => {}
    }, {find_all_envs: () => envRequest.promise}, {
        assignRoleToConsumer: (token, type, appId, namespaceName, envs) => {
            assignedEnvs = Array.from(envs);
            return Promise.resolve([]);
        }
    }).link(scope);
    envRequest.resolve(['LOCAL']);
    await Promise.resolve();
    assert.strictEqual(typeof scope.switchSelect, 'function');
    scope.switchSelect(scope.envs[0]);
    scope.doAssignRoleToConsumer();
    assert.deepStrictEqual(assignedEnvs, ['LOCAL']);
    await Promise.resolve();
}

async function main() {
    testServiceParameters();
    testTemplateAndTranslations();
    await testGrantEnvironmentSelection();
    await testConfirmationAndSuccessfulRevoke();
    await testFailuresAndRetry();
    await testStaleResponsesDoNotReplaceAnotherConsumer();
    await testStaleRevokeDoesNotClearNewerConfirmation();
    await testReopeningConsumerInvalidatesInFlightRevoke();
    await testReopeningAnotherConsumerDoesNotRefresh();
    console.log('Consumer permission viewing, confirmation, scoped revocation and translations passed.');
}

main().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
