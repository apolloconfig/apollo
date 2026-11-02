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
delete_app_cluster_namespace_module.controller('DeleteAppClusterNamespaceController',
    ['$scope', '$translate', 'toastr', 'AppUtil', 'AppService', 'ClusterService', 'NamespaceService', 'PermissionService',
        DeleteAppClusterNamespaceController]);

function DeleteAppClusterNamespaceController($scope, $translate, toastr, AppUtil, AppService, ClusterService, NamespaceService, PermissionService) {

    $scope.app = {};
    $scope.deleteAppBtnDisabled = true;
    $scope.getAppInfo = getAppInfo;
    $scope.deleteApp = deleteApp;

    $scope.cluster = {};
    $scope.deleteClusterBtnDisabled = true;
    $scope.getClusterInfo = getClusterInfo;
    $scope.deleteCluster = deleteCluster;

    $scope.appNamespace = {};
    $scope.deleteAppNamespaceBtnDisabled = true;
    $scope.cleanUpAppNamespaceBtnDisabled = true;
    $scope.getAppNamespaceInfo = getAppNamespaceInfo;
    $scope.deleteAppNamespace = deleteAppNamespace;
    $scope.checkAppNamespaceDeletion = checkAppNamespaceDeletion;
    $scope.cleanUpAppNamespace = cleanUpAppNamespace;

    var ENV_DELETION_STATUS_KEYS = {
        REMAINING: 'Delete.AppNamespaceEnvRemaining',
        ABSENT: 'Delete.AppNamespaceEnvAbsent',
        UNREACHABLE: 'Delete.AppNamespaceEnvUnreachable',
        SKIPPED: 'Delete.AppNamespaceEnvSkipped'
    };

    // numbers the deletion checks, so that only the answer to the latest one is shown
    var latestDeletionCheck = 0;

    initPermission();

    function initPermission() {
        PermissionService.has_root_permission()
            .then(function (result) {
                $scope.isRootUser = result.hasPermission;
            })
    }

    function getAppInfo() {
        if (!$scope.app.appId) {
            toastr.warning($translate.instant('Delete.PleaseEnterAppId'));
            return;
        }

        $scope.app.info = "";

        AppService.load($scope.app.appId).then(function (result) {
            if (!result.appId) {
                toastr.warning($translate.instant('Delete.AppIdNotFound', { appId: $scope.app.appId }));
                $scope.deleteAppBtnDisabled = true;
                return;
            }

            $scope.app.info = $translate.instant('Delete.AppInfoContent', {
                appName: result.name,
                departmentName: result.orgName,
                departmentId: result.orgId,
                ownerName: result.ownerName
            });


            $scope.deleteAppBtnDisabled = false;
        }, function (result) {
            AppUtil.showErrorMsg(result);
        });
    }

    function deleteApp() {
        if (!$scope.app.appId) {
            toastr.warning($translate.instant('Delete.PleaseEnterAppId'));
            return;
        }
        if (confirm($translate.instant('Delete.ConfirmDeleteAppId', { appId: $scope.app.appId }))) {
            AppService.delete_app($scope.app.appId).then(function (result) {
                toastr.success($translate.instant('Delete.Deleted'));
                $scope.deleteAppBtnDisabled = true;
            }, function (result) {
                AppUtil.showErrorMsg(result);
            })
        }
    }

    function getClusterInfo() {
        if (!$scope.cluster.appId || !$scope.cluster.env || !$scope.cluster.name) {
            toastr.warning($translate.instant('Delete.PleaseEnterAppIdAndEnvAndCluster'));
            return;
        }

        $scope.cluster.info = "";

        ClusterService.load_cluster($scope.cluster.appId, $scope.cluster.env, $scope.cluster.name).then(function (result) {
            $scope.cluster.info = $translate.instant('Delete.ClusterInfoContent', {
                appId: result.appId,
                env: $scope.cluster.env,
                clusterName: result.name
            });

            $scope.deleteClusterBtnDisabled = false;
        }, function (result) {
            AppUtil.showErrorMsg(result);
        });
    }

    function deleteCluster() {
        if (!$scope.cluster.appId || !$scope.cluster.env || !$scope.cluster.name) {
            toastr.warning($translate.instant('Delete.PleaseEnterAppIdAndEnvAndCluster'));
            return;
        }
        var confirmTip = $translate.instant('Delete.ConfirmDeleteCluster', {
            appId: $scope.cluster.appId,
            env: $scope.cluster.env,
            clusterName: $scope.cluster.name
        });

        if (confirm(confirmTip)) {
            ClusterService.delete_cluster($scope.cluster.appId, $scope.cluster.env, $scope.cluster.name).then(function (result) {
                toastr.success($translate.instant('Delete.Deleted'));
                $scope.deleteClusterBtnDisabled = true;
            }, function (result) {
                AppUtil.showErrorMsg(result);
            })
        }
    }

    function getAppNamespaceInfo() {
        if (!$scope.appNamespace.appId || !$scope.appNamespace.name) {
            toastr.warning($translate.instant('Delete.PleaseEnterAppIdAndNamespace'));
            return;
        }

        $scope.appNamespace.info = "";
        $scope.appNamespace.notInPortal = false;
        $scope.appNamespace.deletionCheck = null;
        $scope.cleanUpAppNamespaceBtnDisabled = true;
        // a check that is still running no longer belongs to this lookup
        latestDeletionCheck++;

        NamespaceService.loadAppNamespace($scope.appNamespace.appId, $scope.appNamespace.name).then(function (result) {
            $scope.appNamespace.info = $translate.instant('Delete.AppNamespaceInfoContent', {
                appId: result.appId,
                namespace: result.name,
                isPublic: result.isPublic
            });

            $scope.deleteAppNamespaceBtnDisabled = false;
        }, function (result) {
            if (result.status === 400) {
                $scope.appNamespace.info = $translate.instant('Delete.AppNamespaceNotInPortal');
                $scope.appNamespace.notInPortal = true;
                $scope.deleteAppNamespaceBtnDisabled = true;
                return;
            }
            AppUtil.showErrorMsg(result);
        });
    }

    function checkAppNamespaceDeletion() {
        if (!$scope.appNamespace.appId || !$scope.appNamespace.name) {
            toastr.warning($translate.instant('Delete.PleaseEnterAppIdAndNamespace'));
            return;
        }
        loadAppNamespaceDeletionCheck($scope.appNamespace.appId, $scope.appNamespace.name);
    }

    function loadAppNamespaceDeletionCheck(appId, namespaceName) {
        $scope.appNamespace.deletionCheck = null;
        $scope.cleanUpAppNamespaceBtnDisabled = true;
        var check = ++latestDeletionCheck;

        NamespaceService.checkAppNamespaceDeletion(appId, namespaceName).then(function (result) {
            if (check !== latestDeletionCheck) {
                return;
            }
            var environments = [];
            var remainingEnvs = [];
            var uncheckedEnvs = [];
            angular.forEach(result.environments || [], function (environment) {
                var status = environment.status;
                if (status === 'REMAINING') {
                    remainingEnvs.push(environment.env);
                } else if (status === 'UNREACHABLE' || status === 'SKIPPED') {
                    uncheckedEnvs.push(environment.env);
                }
                environments.push($translate.instant(ENV_DELETION_STATUS_KEYS[status], {
                    env: environment.env,
                    message: environment.message
                }));
            });

            // the cleanup acts on the checked AppNamespace even if the inputs change later
            $scope.appNamespace.deletionCheck = {
                appId: appId,
                namespaceName: namespaceName,
                existsInPortal: !!result.existsInPortal,
                environments: environments,
                remainingEnvs: remainingEnvs,
                uncheckedEnvs: uncheckedEnvs
            };
            $scope.cleanUpAppNamespaceBtnDisabled = !!result.existsInPortal || remainingEnvs.length === 0;
        }, function (result) {
            if (check !== latestDeletionCheck) {
                return;
            }
            AppUtil.showErrorMsg(result);
        });
    }

    function cleanUpAppNamespace() {
        var deletionCheck = $scope.appNamespace.deletionCheck;
        // the server checks every environment again, so the confirmation also names the
        // environments that could not be checked, which are cleaned up if they recover meanwhile
        var confirmKey = deletionCheck.uncheckedEnvs.length > 0
            ? 'Delete.ConfirmCleanUpAppNamespaceWithUncheckedEnvs' : 'Delete.ConfirmCleanUpAppNamespace';
        var confirmTip = $translate.instant(confirmKey, {
            appId: deletionCheck.appId,
            namespace: deletionCheck.namespaceName,
            envs: deletionCheck.remainingEnvs.join(', '),
            uncheckedEnvs: deletionCheck.uncheckedEnvs.join(', ')
        });
        if (confirm(confirmTip)) {
            NamespaceService.cleanUpAppNamespace(deletionCheck.appId, deletionCheck.namespaceName).then(function (result) {
                toastr.success($translate.instant('Delete.CleanedUp'));
                loadAppNamespaceDeletionCheck(deletionCheck.appId, deletionCheck.namespaceName);
            }, function (result) {
                AppUtil.showErrorMsg(result);
                loadAppNamespaceDeletionCheck(deletionCheck.appId, deletionCheck.namespaceName);
            })
        }
    }

    function deleteAppNamespace() {
        if (!$scope.appNamespace.appId || !$scope.appNamespace.name) {
            toastr.warning($translate.instant('Delete.PleaseEnterAppIdAndNamespace'));
            return;
        }
        var confirmTip = $translate.instant('Delete.ConfirmDeleteNamespace', {
            appId: $scope.appNamespace.appId,
            namespace: $scope.appNamespace.name
        });
        if (confirm(confirmTip)) {
            NamespaceService.deleteAppNamespace($scope.appNamespace.appId, $scope.appNamespace.name).then(function (result) {
                toastr.success($translate.instant('Delete.Deleted'));
                $scope.deleteAppNamespaceBtnDisabled = true;
            }, function (result) {
                AppUtil.showErrorMsg(result);
            })
        }
    }
}
