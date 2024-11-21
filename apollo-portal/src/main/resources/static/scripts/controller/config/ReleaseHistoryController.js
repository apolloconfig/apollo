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
release_history_module.controller("ReleaseHistoryController",
    ['$scope', '$location', '$translate', 'AppUtil', 'EventManager',
        'ReleaseService', 'ConfigService', 'PermissionService', 'ReleaseHistoryService', releaseHistoryController
    ]);

function releaseHistoryController($scope, $location, $translate, AppUtil, EventManager,
    ReleaseService, ConfigService, PermissionService, ReleaseHistoryService) {

    var params = AppUtil.parseParams($location.$$url);
    $scope.pageContext = {
        appId: params.appid,
        env: params.env,
        clusterName: params.clusterName,
        namespaceName: params.namespaceName,
        releaseId: params.releaseId,
        releaseHistoryId: params.releaseHistoryId
    };
    var PAGE_SIZE = 10;
    var CONFIG_VIEW_TYPE = {
        DIFF: 'diff',
        ALL: 'all'
    };
    var selectedReleaseId = -1;

    $scope.namespace = {};
    $scope.page = 0;
    $scope.releaseHistories = [];
    $scope.hasLoadAll = false;
    $scope.selectedReleaseHistory = 0;
    $scope.isTextNamespace = false;
    // whether current user can view config
    $scope.isConfigHidden = false;

    $scope.showReleaseHistoryDetail = showReleaseHistoryDetail;
    $scope.rollback = rollback;
    $scope.preRollback = preRollback;
    $scope.switchConfigViewType = switchConfigViewType;
    $scope.findReleaseHistory = findReleaseHistory;
    $scope.showText = showText;
    $scope.showTextDiff = showTextDiff;

    EventManager.subscribe(EventManager.EventType.REFRESH_RELEASE_HISTORY, function () {
        location.reload(true);
    });

    init();

    function init() {

        findReleaseHistory();

        loadNamespace();
    }

    function preRollback() {
        EventManager.emit(EventManager.EventType.PRE_ROLLBACK_NAMESPACE, { namespace: $scope.namespace, toReleaseId: selectedReleaseId });
    }

    function rollback() {
        EventManager.emit(EventManager.EventType.ROLLBACK_NAMESPACE, { toReleaseId: selectedReleaseId });
    }

    function findReleaseHistory() {
        if ($scope.hasLoadAll) {
            return;
        }
        ReleaseHistoryService.findReleaseHistoryByNamespace($scope.pageContext.appId,
            $scope.pageContext.env,
            $scope.pageContext.clusterName,
            $scope.pageContext.namespaceName,
            $scope.page, PAGE_SIZE)
            .then(function (result) {
                if ($scope.page == 0) {
                    $(".release-history").removeClass('hidden');
                }

                if (!result || result.length < PAGE_SIZE) {
                    $scope.hasLoadAll = true;
                }

                if (result.length == 0) {
                    return;
                }

                $scope.releaseHistories = $scope.releaseHistories.concat(result);

                if ($scope.page == 0) {
                    var defaultToShowReleaseHistory = result[0];
                    $scope.releaseHistories.forEach(function (history) {
                        if ($scope.pageContext.releaseHistoryId == history.id) {
                            defaultToShowReleaseHistory = history;
                        } else if ($scope.pageContext.releaseId == history.releaseId) {
                            // text namespace doesn't support ALL view
                            if (!$scope.isTextNamespace) {
                                history.viewType = CONFIG_VIEW_TYPE.ALL;
                            }
                            defaultToShowReleaseHistory = history;
                        }
                    });

                    showReleaseHistoryDetail(defaultToShowReleaseHistory);
                }

                $scope.page = $scope.page + 1;

            }, function (result) {
                AppUtil.showErrorMsg(result, $translate.instant('Config.History.LoadingHistoryError'));
            });
    }

    function loadNamespace() {
        ConfigService.load_namespace($scope.pageContext.appId,
            $scope.pageContext.env,
            $scope.pageContext.clusterName,
            $scope.pageContext.namespaceName)
            .then(function (result) {
                $scope.namespace = result;
                $scope.isTextNamespace = result.format != "properties";
                if ($scope.isTextNamespace) {
                    fixTextNamespaceViewType();
                }
                $scope.isConfigHidden = result.isConfigHidden;
                initPermission();
            })
    }

    function showReleaseHistoryDetail(history) {

        $scope.history = history;
        $scope.selectedReleaseHistory = history.id;
        selectedReleaseId = history.releaseId;
        if (!history.viewType) {//default view type
            history.viewType = CONFIG_VIEW_TYPE.DIFF;
            getReleaseDiffConfiguration(history);
        }

    }

    function initPermission() {
        PermissionService.has_release_namespace_permission(
            $scope.pageContext.appId,
            $scope.namespace.baseInfo.namespaceName)
            .then(function (result) {
                if (!result.hasPermission) {
                    PermissionService.has_release_namespace_env_permission(
                        $scope.pageContext.appId,
                        $scope.pageContext.env,
                        $scope.namespace.baseInfo.namespaceName)
                        .then(function (result) {
                            $scope.namespace.hasReleasePermission = result.hasPermission;
                        });
                } else {
                    $scope.namespace.hasReleasePermission = result.hasPermission;
                }
            });
    }

    function fixTextNamespaceViewType() {
        $scope.releaseHistories.forEach(function (history) {
            // text namespace doesn't support ALL view
            if (history.viewType == CONFIG_VIEW_TYPE.ALL) {
                switchConfigViewType(history, CONFIG_VIEW_TYPE.DIFF);
            }
        });
    }

    function switchConfigViewType(history, viewType) {
        history.viewType = viewType;

        if (viewType == CONFIG_VIEW_TYPE.DIFF) {
            getReleaseDiffConfiguration(history);
        }

    }

    function getReleaseDiffConfiguration(history) {

        if (!history.changes) {

            //Set previous release id to master latest release id when branch first gray release.
            if (history.operation == 2 && history.previousReleaseId == 0) {
                history.previousReleaseId = history.operationContext.baseReleaseId;
            }

            ReleaseService.compare($scope.pageContext.env,
                history.previousReleaseId,
                history.releaseId)
                .then(function (result) {
                    history.changes = result.changes;
                })
        }
    }

    function showText(text) {
        $scope.enableTextDiff = false;
        $scope.text = text;
        AppUtil.showModal("#showTextModal");
    }

    function showTextDiff(oldStr, newStr) {
        $scope.enableTextDiff = true;
        $scope.oldStr = oldStr;
        $scope.newStr = newStr;
        AppUtil.showModal('#showTextModal');
    }
}

