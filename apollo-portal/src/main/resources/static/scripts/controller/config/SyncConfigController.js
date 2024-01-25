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
sync_item_module.controller("SyncItemController",
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

            $scope.syncBtnDisabled = false;
            $scope.viewItems = [];

            $scope.toggleItemsCheckedStatus = toggleItemsCheckedStatus;
            $scope.diff = diff;
            $scope.removeItem = removeItem;
            $scope.syncItems = syncItems;
            $scope.collectSelectedClusters = collectSelectedClusters;

            $scope.syncItemNextStep = syncItemNextStep;
            $scope.backToAppHomePage = backToAppHomePage;
            $scope.switchSelect = switchSelect;

            $scope.filter = filter;
            $scope.resetFilter = resetFilter;

            $scope.showText = showText;

            init();

            function init() {
                ////// load items //////
                ConfigService.find_items($scope.pageContext.appId, $scope.pageContext.env,
                    $scope.pageContext.clusterName,
                    $scope.pageContext.namespaceName,
                    "lastModifiedTime")
                    .then(function (result) {

                        sourceItems = [];
                        result.forEach(function (item) {
                            if (item.key) {
                                item.checked = false;
                                sourceItems.push(item);
                            }
                        });

                        $scope.viewItems = sourceItems;
                        $(".apollo-container").removeClass("hidden");
                    }, function (result) {
                        toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.Sync.LoadingItemsError'));
                    });
            }

            var itemAllSelected = false;

            function toggleItemsCheckedStatus() {
                itemAllSelected = !itemAllSelected;
                $scope.viewItems.forEach(function (item) {
                    item.checked = itemAllSelected;
                })
            }

            var syncData = {
                syncToNamespaces: [],
                syncItems: []
            };

            function diff() {
                parseSyncSourceData();
                if (syncData.syncItems.length == 0) {
                    toastr.warning($translate.instant('Config.Sync.PleaseChooseNeedSyncItems'));
                    return;
                }
                if (syncData.syncToNamespaces.length == 0) {
                    toastr.warning($translate.instant('Config.Sync.PleaseChooseCluster'));
                    return;
                }
                $scope.hasDiff = false;
                ConfigService.diff($scope.pageContext.namespaceName, syncData).then(
                    function (result) {

                        $scope.clusterDiffs = result;

                        $scope.clusterDiffs.forEach(function (clusterDiff) {
                            if (!$scope.hasDiff) {
                                $scope.hasDiff =
                                    clusterDiff.diffs.createItems.length
                                    + clusterDiff.diffs.updateItems.length
                                    > 0;
                            }

                            if (clusterDiff.diffs.updateItems.length > 0) {
                                //赋予同步前的值
                                ConfigService.find_items(clusterDiff.namespace.appId,
                                    clusterDiff.namespace.env,
                                    clusterDiff.namespace.clusterName,
                                    clusterDiff.namespace.namespaceName)
                                    .then(function (result) {
                                        var oldItemMap = {};
                                        result.forEach(function (item) {
                                            oldItemMap[item.key] = item.value;
                                        });
                                        clusterDiff.diffs.updateItems.forEach(function (item) {
                                            item.oldValue = oldItemMap[item.key];
                                        })
                                    });
                            }

                        });
                        $scope.syncItemNextStep(1);
                    }, function (result) {
                        toastr.error(AppUtil.errorMsg(result));
                    });
            }

            function removeItem(diff, type, toRemoveItem) {
                var syncDataResult = [],
                    diffSetResult = [],
                    diffSet;
                if (type == 'create') {
                    diffSet = diff.createItems;
                } else {
                    diffSet = diff.updateItems;
                }
                diffSet.forEach(function (item) {
                    if (item.key != toRemoveItem.key) {
                        diffSetResult.push(item);
                    }
                });
                if (type == 'create') {
                    diff.createItems = diffSetResult;
                } else {
                    diff.updateItems = diffSetResult;
                }

                syncData.syncItems.forEach(function (item) {
                    if (item.key != toRemoveItem.key) {
                        syncDataResult.push(item);
                    }
                });
                syncData.syncItems = syncDataResult;
            }

            function syncItems() {
                $scope.syncBtnDisabled = true;
                ConfigService.sync_items($scope.pageContext.appId,
                    $scope.pageContext.namespaceName,
                    syncData).then(function (result) {
                        $scope.syncItemStep += 1;
                        $scope.syncSuccess = true;
                        $scope.syncBtnDisabled = false;
                    }, function (result) {
                        $scope.syncSuccess = false;
                        $scope.syncBtnDisabled = false;
                        toastr.error(AppUtil.errorMsg(result));
                    });
            }

            var selectedClusters = [];

            function collectSelectedClusters(data) {
                selectedClusters = data;
            }

            function parseSyncSourceData() {
                syncData = {
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

                $scope.viewItems.forEach(function (item) {
                    if (item.checked) {
                        syncData.syncItems.push(item);
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

            function filter() {
                var beginTime = $scope.filterBeginTime;
                var endTime = $scope.filterEndTime;

                var result = [];
                sourceItems.forEach(function (item) {
                    var updateTime = new Date(item.dataChangeLastModifiedTime);
                    if ((!beginTime || updateTime > beginTime)
                        && (!endTime || updateTime < endTime)) {
                        result.push(item);
                    }
                });

                $scope.viewItems = result;
            }

            function resetFilter() {
                $scope.filterBeginTime = null;
                $scope.filterEndTime = null;
                filter();
            }

            function showText(text) {
                $scope.text = text;
                AppUtil.showModal('#showTextModal');
            }


        }]);

