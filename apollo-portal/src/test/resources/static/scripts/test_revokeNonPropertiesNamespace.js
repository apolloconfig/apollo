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

var fs = require('fs');
var path = require('path');

var templatePath = path.resolve(
    __dirname,
    '../../../../main/resources/static/views/component/namespace-panel-master-tab.html'
);
var template = fs.readFileSync(templatePath, 'utf8');
var revokeControls =
    template.match(/<(?:button|img)\b[^>]*ng-click="preRevokeItem\(namespace\)"[^>]*>/g) || [];

var hasNonPropertiesRevokeControl = revokeControls.some(function (control) {
    return control.indexOf("namespace.viewType == 'text'") >= 0
        && control.indexOf("namespace.displayControl.currentOperateBranch == 'master'") >= 0
        && control.indexOf("!namespace.isPropertiesFormat") >= 0
        && control.indexOf("!namespace.isTextEditing") >= 0
        && control.indexOf("namespace.hasModifyPermission") >= 0
        && control.indexOf('aria-label=') >= 0
        && control.indexOf('<button') === 0;
});

if (!hasNonPropertiesRevokeControl) {
    console.error('Non-properties namespace revoke control is missing or has incomplete guards.');
    process.exit(1);
}

console.log('Non-properties namespace revoke control is available.');
