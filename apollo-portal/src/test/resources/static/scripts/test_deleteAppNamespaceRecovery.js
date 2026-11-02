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
    '../../../../main/resources/static/scripts/controller/DeleteAppClusterNamespaceController.js'), 'utf8');

const settled = (ok, value) => ({ then: (onOk, onFail) => (ok ? onOk(value) : onFail(value)) });
const pending = () => ({ then: () => {} });
// an answer that the test delivers by itself, to answer a request after a later one
const later = () => {
    const waiting = [];
    return {
        then: (onOk, onFail) => { waiting.push({ onOk, onFail }); },
        answer: value => waiting.forEach(({ onOk }) => onOk(value)),
        fail: value => waiting.forEach(({ onFail }) => onFail(value))
    };
};

function page({ lookup, checks = [], cleanUp = settled(true, {}), confirmed = true }) {
    const scope = {}, calls = { checks: [], cleanUps: [], errors: [], confirms: [], success: [] };
    let factory;
    vm.runInNewContext(source, {
        delete_app_cluster_namespace_module: { controller: (name, definition) => { factory = definition.at(-1); } },
        angular: { forEach: (items, callback) => items.forEach(callback) },
        confirm: text => { calls.confirms.push(text); return confirmed; }
    });
    factory(scope, { instant: (key, args) => ({ key, args }) },
        { warning: () => {}, success: message => calls.success.push(message) },
        { showErrorMsg: result => calls.errors.push(result) }, {}, {},
        {
            loadAppNamespace: () => lookup,
            checkAppNamespaceDeletion: (appId, namespaceName) => {
                calls.checks.push([appId, namespaceName]);
                const next = checks.shift();
                return next && next.then ? next : settled(true, next);
            },
            cleanUpAppNamespace: (appId, namespaceName) => {
                calls.cleanUps.push([appId, namespaceName]);
                return cleanUp;
            }
        },
        { has_root_permission: pending });
    scope.appNamespace.appId = 'sample-app';
    scope.appNamespace.name = 'db-config';
    return { scope, calls };
}

const remainingInDev = { existsInPortal: false, environments: [
    { env: 'DEV', status: 'REMAINING' }, { env: 'PRO', status: 'ABSENT' }] };
const nothingRemains = { existsInPortal: false, environments: [
    { env: 'DEV', status: 'ABSENT' }, { env: 'PRO', status: 'ABSENT' }] };

test('reopening the page after a partial deletion offers the check and cleans up what remains', () => {
    const { scope, calls } = page({ lookup: settled(false, { status: 400 }),
        checks: [remainingInDev, nothingRemains] });

    scope.getAppNamespaceInfo();
    assert.equal(scope.appNamespace.notInPortal, true);
    assert.equal(scope.deleteAppNamespaceBtnDisabled, true);
    assert.equal(calls.errors.length, 0);

    scope.checkAppNamespaceDeletion();
    assert.deepEqual(Array.from(scope.appNamespace.deletionCheck.remainingEnvs), ['DEV']);
    assert.deepEqual(Array.from(scope.appNamespace.deletionCheck.environments, e => e.key),
        ['Delete.AppNamespaceEnvRemaining', 'Delete.AppNamespaceEnvAbsent']);
    assert.equal(scope.cleanUpAppNamespaceBtnDisabled, false);

    scope.cleanUpAppNamespace();
    assert.equal(calls.confirms[0].key, 'Delete.ConfirmCleanUpAppNamespace');
    assert.equal(calls.confirms[0].args.envs, 'DEV');
    assert.deepEqual(calls.cleanUps, [['sample-app', 'db-config']]);
    assert.equal(calls.success.length, 1);
    assert.equal(calls.checks.length, 2);
    assert.equal(scope.cleanUpAppNamespaceBtnDisabled, true);
});

test('an AppNamespace recreated in the Portal is not offered for cleanup', () => {
    const { scope } = page({ lookup: settled(false, { status: 400 }),
        checks: [{ existsInPortal: true, environments: [{ env: 'DEV', status: 'REMAINING' }] }] });

    scope.checkAppNamespaceDeletion();

    assert.equal(scope.appNamespace.deletionCheck.existsInPortal, true);
    assert.equal(scope.cleanUpAppNamespaceBtnDisabled, true);
});

test('unreachable and skipped environments are listed, and alone they do not enable the cleanup', () => {
    const { scope } = page({ lookup: settled(false, { status: 400 }), checks: [{ existsInPortal: false,
        environments: [{ env: 'DEV', status: 'UNREACHABLE', message: 'Admin servers are unresponsive.' },
            { env: 'PRO', status: 'SKIPPED' }] }] });

    scope.checkAppNamespaceDeletion();

    assert.deepEqual(Array.from(scope.appNamespace.deletionCheck.environments, e => [e.key, e.args.message]),
        [['Delete.AppNamespaceEnvUnreachable', 'Admin servers are unresponsive.'],
            ['Delete.AppNamespaceEnvSkipped', undefined]]);
    assert.equal(scope.cleanUpAppNamespaceBtnDisabled, true);
});

test('the confirmation also names the environments that could not be checked, which the cleanup checks again', () => {
    const { scope, calls } = page({ lookup: settled(false, { status: 400 }), checks: [{ existsInPortal: false,
        environments: [{ env: 'DEV', status: 'REMAINING' }, { env: 'FAT', status: 'ABSENT' },
            { env: 'UAT', status: 'UNREACHABLE', message: 'Admin servers are unresponsive.' },
            { env: 'PRO', status: 'SKIPPED' }] }, nothingRemains] });

    scope.checkAppNamespaceDeletion();
    assert.equal(scope.cleanUpAppNamespaceBtnDisabled, false);
    scope.cleanUpAppNamespace();

    assert.equal(calls.confirms[0].key, 'Delete.ConfirmCleanUpAppNamespaceWithUncheckedEnvs');
    assert.equal(calls.confirms[0].args.envs, 'DEV');
    assert.equal(calls.confirms[0].args.uncheckedEnvs, 'UAT, PRO');
    assert.deepEqual(calls.cleanUps, [['sample-app', 'db-config']]);
});

test('the cleanup acts on the checked AppNamespace even if the inputs change afterwards', () => {
    const { scope, calls } = page({ lookup: settled(false, { status: 400 }),
        checks: [remainingInDev, nothingRemains] });

    scope.checkAppNamespaceDeletion();
    scope.appNamespace.name = 'another';
    scope.cleanUpAppNamespace();

    assert.deepEqual(calls.cleanUps, [['sample-app', 'db-config']]);
});

test('a check answered after a newer check does not replace the newer result', () => {
    const older = later();
    const { scope, calls } = page({ lookup: settled(false, { status: 400 }),
        checks: [older, remainingInDev, nothingRemains] });

    scope.checkAppNamespaceDeletion();
    scope.appNamespace.name = 'another';
    scope.checkAppNamespaceDeletion();
    older.answer(nothingRemains);

    assert.equal(scope.appNamespace.deletionCheck.namespaceName, 'another');
    assert.deepEqual(Array.from(scope.appNamespace.deletionCheck.remainingEnvs), ['DEV']);
    assert.equal(scope.cleanUpAppNamespaceBtnDisabled, false);
    scope.cleanUpAppNamespace();
    assert.deepEqual(calls.cleanUps, [['sample-app', 'another']]);
});

test('a check that fails after a newer check shows no error', () => {
    const older = later();
    const { scope, calls } = page({ lookup: settled(false, { status: 400 }),
        checks: [older, remainingInDev] });

    scope.checkAppNamespaceDeletion();
    scope.checkAppNamespaceDeletion();
    older.fail({ status: 500, data: { message: 'boom' } });

    assert.equal(calls.errors.length, 0);
    assert.deepEqual(Array.from(scope.appNamespace.deletionCheck.remainingEnvs), ['DEV']);
});

test('a check answered after the lookup was repeated does not fill the panel again', () => {
    const slow = later();
    const { scope } = page({ lookup: settled(false, { status: 400 }), checks: [slow] });

    scope.checkAppNamespaceDeletion();
    scope.getAppNamespaceInfo();
    slow.answer(remainingInDev);

    assert.equal(scope.appNamespace.deletionCheck, null);
    assert.equal(scope.cleanUpAppNamespaceBtnDisabled, true);
});

test('a failed cleanup shows the server message and checks again', () => {
    const failure = { status: 500, data: { message: 'incomplete' } };
    const { scope, calls } = page({ lookup: settled(false, { status: 400 }),
        checks: [remainingInDev, remainingInDev], cleanUp: settled(false, failure) });

    scope.checkAppNamespaceDeletion();
    scope.cleanUpAppNamespace();

    assert.deepEqual(calls.errors, [failure]);
    assert.equal(calls.checks.length, 2);
    assert.equal(scope.cleanUpAppNamespaceBtnDisabled, false);
});

test('a declined confirmation cleans up nothing', () => {
    const { scope, calls } = page({ lookup: settled(false, { status: 400 }), checks: [remainingInDev],
        confirmed: false });

    scope.checkAppNamespaceDeletion();
    scope.cleanUpAppNamespace();

    assert.equal(calls.cleanUps.length, 0);
});

test('a lookup error other than 400 keeps the error message and offers no check', () => {
    const failure = { status: 500, data: { message: 'boom' } };
    const { scope, calls } = page({ lookup: settled(false, failure) });

    scope.getAppNamespaceInfo();

    assert.deepEqual(calls.errors, [failure]);
    assert.equal(scope.appNamespace.notInPortal, false);
});
