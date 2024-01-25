/*
 * Copyright 2024 Apollo Authors
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
directive_module.directive('grantpermissionmodal', grantPermissionModalDirective);

function grantPermissionModalDirective($translate, toastr, $sce, AppUtil, EnvService, ConsumerService) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/open/grant-permission-modal.html',
        transclude: true,
        replace: true,
        scope: {
            consumerRole: '=',
            assignRoleToConsumer: '=',
        },
        link: function (scope) {
            scope.initialized = false;
            scope.envs = [];
            scope.envsChecked = [];

            if(!scope.initialized){
                initEnv();
            }

            scope.doAssignRoleToConsumer = function () {
                ConsumerService.assignRoleToConsumer(scope.consumerRole.token,
                    scope.consumerRole.type,
                    scope.consumerRole.appId,
                    scope.consumerRole.namespaceName,
                    scope.envsChecked)
                .then(function (consumerRoles) {
                    toastr.success($translate.instant('Open.Manage.GrantSuccessfully'));
                    AppUtil.hideModal('#grantPermissionModal');
                    scope.consumerRole = {}
                }, function (response) {
                    AppUtil.showErrorMsg(response, $translate.instant('Open.Manage.GrantFailed'));
                })
            }

            function initEnv() {
                EnvService.find_all_envs()
                .then(function (result) {
                    for (let iLoop = 0; iLoop < result.length; iLoop++) {
                        scope.envs.push({ checked: false, env: result[iLoop] });
                        scope.envsChecked = [];
                    }

                    scope.envsChecked.switchSelect = function (item) {
                        item.checked = !item.checked;
                        scope.envsChecked = [];
                        for (let iLoop = 0; iLoop < scope.envs.length; iLoop++) {
                            const env = scope.envs[iLoop];
                            if (env.checked) {
                                scope.envsChecked.push(env.env);
                            }
                        }
                    };
                });
            }

        }
    }
}


