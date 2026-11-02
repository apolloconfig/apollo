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
directive_module.directive('viewpermissionsmodal', viewPermissionsModalDirective);

function viewPermissionsModalDirective($translate, toastr, AppUtil, ConsumerService) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/open/view-permissions-modal.html',
        replace: true,
        scope: {
            consumer: '='
        },
        link: function (scope) {
            var loadVersion = 0;
            scope.roles = [];
            scope.loading = false;
            scope.loadError = false;
            scope.revoking = false;
            scope.pendingRole = null;
            scope.reloadRoles = loadRoles;

            scope.$watch('consumer', function () {
                scope.pendingRole = null;
                scope.revoking = false;
                loadRoles();
            });

            scope.preRevokeRole = function (role) {
                if (!scope.revoking) {
                    scope.pendingRole = role;
                }
            };

            scope.cancelRevokeRole = function () {
                if (!scope.revoking) {
                    scope.pendingRole = null;
                }
            };

            scope.revokeRole = function () {
                if (!scope.pendingRole || scope.revoking || !scope.consumer) {
                    return;
                }
                var consumerId = scope.consumer.consumerId;
                scope.revoking = true;
                ConsumerService.revokeConsumerRoles(consumerId, scope.pendingRole)
                    .then(function () {
                        toastr.success($translate.instant('Open.Manage.RevokePermission.Success'));
                        if (scope.consumer && scope.consumer.consumerId === consumerId) {
                            scope.revoking = false;
                            scope.pendingRole = null;
                            loadRoles();
                        }
                    }, function (response) {
                        if (scope.consumer && scope.consumer.consumerId === consumerId) {
                            scope.revoking = false;
                            AppUtil.showErrorMsg(response,
                                $translate.instant('Open.Manage.RevokePermission.Failed'));
                        }
                    });
            };

            function loadRoles() {
                var version = ++loadVersion;
                scope.roles = [];
                scope.loadError = false;
                scope.loading = !!scope.consumer;
                if (!scope.consumer) {
                    return;
                }
                ConsumerService.getConsumerRoles(scope.consumer.consumerId)
                    .then(function (roles) {
                        if (version === loadVersion) {
                            scope.roles = roles;
                            scope.loading = false;
                        }
                    }, function (response) {
                        if (version === loadVersion) {
                            scope.loading = false;
                            scope.loadError = true;
                            AppUtil.showErrorMsg(response,
                                $translate.instant('Open.Manage.ViewPermissions.Failed'));
                        }
                    });
            }
        }
    };
}
