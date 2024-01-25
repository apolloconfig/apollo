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
directive_module.directive('deletenamespacemodal', deleteNamespaceModalDirective);

function deleteNamespaceModalDirective($window, $q, $translate, toastr, AppUtil, EventManager,
    PermissionService, UserService, NamespaceService) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/delete-namespace-modal.html',
        transclude: true,
        replace: true,
        scope: {
            env: '='
        },
        link: function (scope) {

            scope.doDeleteNamespace = doDeleteNamespace;

            EventManager.subscribe(EventManager.EventType.PRE_DELETE_NAMESPACE, function (context) {
                var toDeleteNamespace = context.namespace;
                scope.toDeleteNamespace = toDeleteNamespace;

                //1. check operator has master permission
                checkPermission(toDeleteNamespace).then(function () {

                    if (toDeleteNamespace.isLinkedNamespace) {
                        NamespaceService.getLinkedNamespaceUsage(toDeleteNamespace.baseInfo.appId, scope.env,
                            toDeleteNamespace.baseInfo.clusterName,
                            toDeleteNamespace.baseInfo.namespaceName
                        ).then(function (usage) {
                            scope.toDeleteNamespace.namespaceUsage = usage;
                            if (usage[0].instanceCount > 0 || usage[0].branchInstanceCount > 0) {
                                scope.toDeleteNamespace.forceDeleteButton = true;
                            }
                            showDeleteNamespaceConfirmDialog();
                        });
                    } else {
                        NamespaceService.getNamespaceUsage(toDeleteNamespace.baseInfo.appId,
                            toDeleteNamespace.baseInfo.namespaceName
                        ).then(function (usage) {
                            scope.toDeleteNamespace.namespaceUsage = usage;
                            if (usage.length > 0) {
                                scope.toDeleteNamespace.forceDeleteButton = true;
                            }
                            showDeleteNamespaceConfirmDialog();
                        });
                    }
                })

            });

            function checkPermission(namespace) {
                var d = $q.defer();

                UserService.load_user().then(function (currentUser) {

                    var isAppMasterUser = false;

                    PermissionService.get_app_role_users(namespace.baseInfo.appId)
                        .then(function (appRoleUsers) {

                            var masterUsers = [];

                            appRoleUsers.masterUsers.forEach(function (user) {
                                masterUsers.push(_.escape(user.userId));

                                if (currentUser.userId == user.userId) {
                                    isAppMasterUser = true;
                                }
                            });

                            scope.masterUsers = masterUsers;
                            scope.isAppMasterUser = isAppMasterUser;

                            if (!isAppMasterUser) {
                                toastr.error($translate.instant('Config.DeleteNamespaceNoPermissionFailedTips', {
                                    users: scope.masterUsers.join(", ")
                                }), $translate.instant('Config.DeleteNamespaceNoPermissionFailedTitle'));
                                d.reject();
                            } else {
                                d.resolve();
                            }
                        });
                });

                return d.promise;
            }

            function showDeleteNamespaceConfirmDialog() {
                AppUtil.showModal('#deleteNamespaceModal');
            }

            function doDeleteNamespace() {
                var toDeleteNamespace = scope.toDeleteNamespace;
                if(toDeleteNamespace.isLinkedNamespace){
                    NamespaceService.deleteLinkedNamespace(toDeleteNamespace.baseInfo.appId, scope.env,
                        toDeleteNamespace.baseInfo.clusterName,
                        toDeleteNamespace.baseInfo.namespaceName)
                    .then(function () {
                        toastr.success($translate.instant('Common.Deleted'));

                        setTimeout(function () {
                            $window.location.reload();
                        }, 1000);

                    }, function (result) {
                        AppUtil.showErrorMsg(result, $translate.instant('Common.DeleteFailed'));
                    })
                } else {
                    NamespaceService.deleteAppNamespace(toDeleteNamespace.baseInfo.appId,
                        toDeleteNamespace.baseInfo.namespaceName)
                    .then(function () {
                        toastr.success($translate.instant('Common.Deleted'));

                        setTimeout(function () {
                            $window.location.reload();
                        }, 1000);

                    }, function (result) {
                        AppUtil.showErrorMsg(result, $translate.instant('Common.DeleteFailed'));
                    })
                }
            }

        }
    }
}



