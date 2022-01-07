/*
 * Copyright 2021 Apollo Authors
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
diff_item_module.controller("DiffItemController",
    ['$scope', '$location', '$window', '$translate', 'toastr', 'AppService', 'AppUtil', 'ConfigService',
        function ($scope, $location, $window, $translate, toastr, AppService, AppUtil, ConfigService) {

            var params = AppUtil.parseParams($location.$$url);
            $scope.pageContext = {
                appId: params.appid,
                env: params.env,
                clusterName: params.clusterName,
                namespaceName: params.namespaceName
            };
            var sourceItems = [];

            $scope.diff = diff;
            $scope.syncBtnDisabled = false;
            $scope.showCommentDiff = false;
            $scope.onlyShowDiffKeys = true;

            $scope.collectSelectedClusters = collectSelectedClusters;

            $scope.syncItemNextStep = syncItemNextStep;
            $scope.backToAppHomePage = backToAppHomePage;
            $scope.switchSelect = switchSelect;

            $scope.showText = showText;

            $scope.itemsKeyedByKey = {};
            $scope.allNamespaceValueEqualed = {};

            $scope.syncData = {
                syncToNamespaces: [],
                syncItems: []
            };

            function diff() {
                $scope.syncData = parseSyncSourceData();
                if ($scope.syncData.syncToNamespaces.length < 2) {
                    toastr.warning($translate.instant('Config.Diff.PleaseChooseTwoCluster'));
                    return;
                }
                const namespaceCnt = $scope.syncData.syncToNamespaces.length;
                let loadedNamespaceCnt = 0;
                $scope.syncData.syncToNamespaces.forEach(function (namespace) {
                    ConfigService.find_items(namespace.appId,
                        namespace.env,
                        namespace.clusterName,
                        namespace.namespaceName).then(function (result) {
                            loadedNamespaceCnt ++;
                            result.forEach(function (item) {
                                if (item.key === "") {
                                    return
                                }
                                var itemsKeyedByClusterName = $scope.itemsKeyedByKey[item.key] || {};
                                itemsKeyedByClusterName[namespace.env + ':' + namespace.clusterName + ':' + namespace.namespaceName] = item;
                                $scope.itemsKeyedByKey[item.key] = itemsKeyedByClusterName;
                            });

                            //After loading all the compared namespaces, check whether the values are consistent
                            //itemsKeyedByKey struct : itemKey => namespace => item
                            if (loadedNamespaceCnt === namespaceCnt) {
                                Object.keys($scope.itemsKeyedByKey).forEach(function (key) {
                                    let lastValue = null;
                                    let allEqualed = true;
                                    Object.values($scope.itemsKeyedByKey[key]).forEach(function (item) {
                                        if (lastValue == null) {
                                            lastValue = item.value;
                                        }
                                        if (lastValue !== item.value) {
                                            allEqualed = false;
                                        }
                                    })
                                    $scope.allNamespaceValueEqualed[key]=allEqualed;
                                })
                            }
                        });
                });
                $scope.syncItemNextStep(1);
            }

            var selectedClusters = [];

            function collectSelectedClusters(data) {
                selectedClusters = data;
            }

            function parseSyncSourceData() {
                var syncData = {
                    syncToNamespaces: [],
                    syncItems: []
                };
                var namespaceName = $scope.pageContext.namespaceName;
                selectedClusters.forEach(function (cluster) {
                    if (cluster.checked) {
                        cluster.clusterName = cluster.name;
                        cluster.namespaceName = namespaceName;
                        syncData.syncToNamespaces.push(cluster);
                    }
                });

                return syncData;
            }

            ////// flow control ///////

            $scope.syncItemStep = 1;
            function syncItemNextStep(offset) {
                $scope.syncItemStep += offset;
            }

            function backToAppHomePage() {
                $window.location.href = AppUtil.prefixPath() + '/config.html?#appid=' + $scope.pageContext.appId;
            }

            function switchSelect(o) {
                o.checked = !o.checked;
            }

            function showText(text) {
                $scope.text = text;
                AppUtil.showModal('#showTextModal');
            }
        }]);
