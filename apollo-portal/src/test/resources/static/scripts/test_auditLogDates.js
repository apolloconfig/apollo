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
    '../../../../main/resources/static/scripts/controller/AuditLogMenuController.js'), 'utf8');

function controller() {
    const scope = {}, requests = [];
    let respond;
    const service = {
        get_properties: () => ({ then: callback => callback({ enabled: true }) }),
        find_all_logs: () => ({ then: callback => callback([]) }),
        find_logs_by_opName: (...args) => {
            requests.push(args);
            return { then: callback => { respond = callback; } };
        }
    };
    const context = vm.createContext({ audit_log_menu_module: { controller: () => {} } });
    vm.runInContext('Date.prototype.Format = function () { return this.toISOString(); };', context);
    vm.runInContext(source, context);
    context.auditLogMenuController(scope, {}, {}, { on: () => {} }, {}, {}, {}, {}, service);
    return { scope, requests, respond: rows => respond(rows) };
}

for (const cleared of [null, undefined, '']) {
    test('clearing both dates discards the previous filter: ' + String(cleared), () => {
        const { scope, requests, respond } = controller();
        scope.searchByOpNameAndDate('NamespaceBranch.updateBranchRules', '2026-09-26T15:15:00Z', '2026-09-26T15:16:00Z');
        respond([{ id: 1 }]);
        assert.equal(scope.hasLoadAll, true);
        scope.searchByOpNameAndDate('NamespaceBranch.updateBranchRules', cleared, cleared);
        assert.deepEqual(requests.at(-1), ['NamespaceBranch.updateBranchRules', null, null, 0, 10]);
        assert.equal(scope.hasLoadAll, false);
        respond(Array.from({ length: 10 }, (_, id) => ({ id })));
        scope.getMoreAuditLogs();
        assert.deepEqual(requests.at(-1), ['NamespaceBranch.updateBranchRules', null, null, 1, 10]);
    });
}

for (const cleared of [null, undefined, '']) {
    for (const bound of ['start', 'end']) {
        test('clearing ' + bound + ' preserves the other bound when paging: ' + String(cleared), () => {
            const { scope, requests, respond } = controller();
            const date = '2026-09-26T15:15:00.000Z';
            scope.searchByOpNameAndDate('operation', date, date);
            respond([{ id: 1 }]);
            assert.equal(scope.hasLoadAll, true);
            scope.searchByOpNameAndDate('operation',
                bound === 'start' ? cleared : date, bound === 'end' ? cleared : date);
            const start = bound === 'start' ? null : date;
            const end = bound === 'end' ? null : date;
            assert.deepEqual(requests.at(-1), ['operation', start, end, 0, 10]);
            assert.equal(scope.auditLogList.length, 0);
            assert.equal(scope.hasLoadAll, false);
            respond(Array.from({ length: 10 }, (_, id) => ({ id })));
            scope.getMoreAuditLogs();
            assert.deepEqual(requests.at(-1), ['operation', start, end, 1, 10]);
        });
    }
}
