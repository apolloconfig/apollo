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
            $scope.searchKey = ''
            $scope.syncBtnDisabled = false;
            $scope.showCommentDiff = false;
            $scope.onlyShowDiffKeys = true;
            $scope.collectSelectedClusters = collectSelectedClusters;

            $scope.syncItemNextStep = syncItemNextStep;
            $scope.backToAppHomePage = backToAppHomePage;
            $scope.switchSelect = switchSelect;

            $scope.showTextDiff = showTextDiff;

            $scope.itemsKeyedByKey = {};
            $scope.allNamespaceValueEqualed = {};
            $scope.isTableDiff = true;
            $scope.isPropertiesFormat = true;

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
                if (!$scope.isTableDiff && $scope.syncData.syncToNamespaces.length > 2) {
                    toastr.warning($translate.instant('Config.Diff.TextDiffMostChooseTwoCluster'));
                    return;
                }
                const namespaceCnt = $scope.syncData.syncToNamespaces.length;
                let loadedNamespaceCnt = 0;
                $scope.syncData.syncToNamespaces.forEach(function (namespace) {
                    ConfigService.find_items(namespace.appId,
                        namespace.env,
                        namespace.clusterName,
                        namespace.namespaceName).then(function (result) {
                        loadedNamespaceCnt++;
                        let suffix = ''
                        if (namespace.namespaceName.includes('.')) {
                            suffix = namespace.namespaceName.match(/[^.]+$/)[0];
                            $scope.isPropertiesFormat = false
                        }
                        let res = [];
                        let propTextInfo = "";
                        let propTextArr = [];

                        result.forEach(function (originItem) {
                            if (originItem.key === "") {
                                return
                            }
                            // prop
                            if (suffix === '') {
                                res.push(originItem)
                                propTextArr.push(originItem)

                            } else {
                                namespace.originTextInfo = originItem.value
                                if (suffix === 'yml') {
                                    res = Obj2Prop(
                                        YAML.parse(originItem.value))
                                } else if (suffix === 'json') {
                                    res = Obj2Prop(
                                        JSON.parse(originItem.value))
                                } else if (suffix === 'xml') {
                                    const x2js = new X2JS();
                                    res = Obj2Prop(
                                        x2js.xml_str2json(originItem.value))
                                } else {
                                    //txt
                                    res.push(originItem)
                                }
                            }
                        });
                        // For prop textDiff,need parse prop to text
                        if (suffix === '') {
                            // to align key
                            propTextArr.sort((item1, item2) => item1.key.localeCompare(item2.key));
                            propTextArr.forEach(function (item) {
                                if (item.key) {
                                    //use string \n to display as new line
                                    var itemValue = item.value.replace(/\n/g, "\\n");

                                    propTextInfo += item.key + " = " + itemValue + "\n";
                                } else {
                                    propTextInfo += item.comment + "\n";
                                }
                            });
                            namespace.originTextInfo = propTextInfo
                        }
                        res.forEach(function (item) {
                            const itemsKeyedByClusterName = $scope.itemsKeyedByKey[item.key] || {};
                            itemsKeyedByClusterName[namespace.env + ':' + namespace.clusterName + ':' + namespace.namespaceName] = item;
                            $scope.itemsKeyedByKey[item.key] = itemsKeyedByClusterName;
                        })
                        //After loading all the compared namespaces, check whether the values are consistent
                        //itemsKeyedByKey struct : itemKey => namespace => item
                        if (loadedNamespaceCnt === namespaceCnt) {
                            Object.keys($scope.itemsKeyedByKey).forEach(
                                function (key) {
                                    let lastValue = null;
                                    let allEqualed = true;
                                    // some namespace lack key,determined as not allEqual
                                    if (Object.keys($scope.itemsKeyedByKey[key]).length !== namespaceCnt) {
                                        allEqualed = false;
                                    } else {
                                        // check key items allEqual
                                        Object.values($scope.itemsKeyedByKey[key]).forEach(
                                            function (item) {
                                                if (lastValue == null) {
                                                    lastValue = item.value;
                                                }
                                                if (lastValue !== item.value) {
                                                    allEqualed = false;
                                                }
                                            })
                                    }
                                    $scope.allNamespaceValueEqualed[key] = allEqualed;
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
                    syncItems: [],
                    firstClusterKey: "",
                };
                var namespaceName = $scope.pageContext.namespaceName;
                selectedClusters.forEach(function (cluster) {
                    if (cluster.checked) {
                        cluster.clusterName = cluster.name;
                        cluster.namespaceName = namespaceName;
                        cluster.compositedKey = cluster.env + ':' + cluster.clusterName + ':' + cluster.namespaceName;
                        syncData.syncToNamespaces.push(cluster);
                    }
                });
                syncData.firstClusterKey = selectedClusters[0].compositedKey;
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

            function showTextDiff(oldStr, newStr) {
                $scope.oldStr = oldStr;
                $scope.newStr = newStr;
                AppUtil.showModal('#showTextModal');
            }
        }]);
// transfer js obj to properties
function Obj2Prop(obj,prefix){
    let result = []
    const keys = Object.keys(obj)
    keys.forEach(function (key){
        let keyPrefix;
        if(obj[key] && typeof obj[key]=='object'){
            const currentPrefix = key.concat('.');
            keyPrefix = prefix? prefix.concat(currentPrefix) : currentPrefix
            result = result.concat(Obj2Prop(obj[key],keyPrefix))
        }else{
            keyPrefix = prefix? prefix.concat(key):key
            result.push({
                key:keyPrefix,
                value:(obj[key] || '')
            })
        }
    })
    return result

}
