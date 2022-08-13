/*
 * Copyright 2022 Apollo Authors
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
directive_module.directive('itemmodal', itemModalDirective);

function itemModalDirective($translate, toastr, $sce, AppUtil, EventManager, ConfigService) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/item-modal.html',
        transclude: true,
        replace: true,
        scope: {
            appId: '=',
            env: '=',
            cluster: '=',
            toOperationNamespace: '=',
            item: '='
        },
        link: function (scope) {

            var TABLE_VIEW_OPER_TYPE = {
                CREATE: 'create',
                UPDATE: 'update'
            };

            scope.doItem = doItem;
            scope.collectSelectedClusters = collectSelectedClusters;
            scope.showHiddenChars = showHiddenChars;
            scope.detectJSON = detectJSON;

            $('#itemModal').on('show.bs.modal', function (e) {
                scope.showHiddenCharsContext = false;
                scope.hiddenCharCounter = 0;
                scope.valueWithHiddenChars = $sce.trustAsHtml('');
                scope.showJSONDetectContext = false;
                scope.jsonDetectResult = $sce.trustAsHtml('');
            });

            $("#valueEditor").textareafullscreen();

            function doItem() {

                if (!scope.item.value) {
                    scope.item.value = "";
                }

                if (scope.item.tableViewOperType == TABLE_VIEW_OPER_TYPE.CREATE) {

                    //check key unique
                    var hasRepeatKey = false;
                    scope.toOperationNamespace.items.forEach(function (item) {
                        if (!item.isDeleted && scope.item.key == item.item.key) {
                            toastr.error($translate.instant('ItemModal.KeyExists', { key: scope.item.key }));
                            hasRepeatKey = true;
                        }
                    });
                    if (hasRepeatKey) {
                        return;
                    }

                    scope.item.addItemBtnDisabled = true;

                    if (scope.toOperationNamespace.isBranch) {
                        ConfigService.create_item(scope.appId,
                            scope.env,
                            scope.toOperationNamespace.baseInfo.clusterName,
                            scope.toOperationNamespace.baseInfo.namespaceName,
                            scope.item).then(
                                function (result) {
                                    toastr.success($translate.instant('ItemModal.AddedTips'));
                                    scope.item.addItemBtnDisabled = false;
                                    AppUtil.hideModal('#itemModal');
                                    EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                                        {
                                            namespace: scope.toOperationNamespace
                                        });

                                }, function (result) {
                                    toastr.error(AppUtil.errorMsg(result), $translate.instant('ItemModal.AddFailed'));
                                    scope.item.addItemBtnDisabled = false;
                                });
                    } else {
                        if (selectedClusters.length == 0) {
                            toastr.error($translate.instant('ItemModal.PleaseChooseCluster'));
                            scope.item.addItemBtnDisabled = false;
                            return;
                        }

                        selectedClusters.forEach(function (cluster) {
                            ConfigService.create_item(scope.appId,
                                cluster.env,
                                cluster.name,
                                scope.toOperationNamespace.baseInfo.namespaceName,
                                scope.item).then(
                                    function (result) {
                                        scope.item.addItemBtnDisabled = false;
                                        AppUtil.hideModal('#itemModal');
                                        toastr.success(cluster.env + " , " + scope.item.key, $translate.instant('ItemModal.AddedTips'));
                                        if (cluster.env == scope.env &&
                                            cluster.name == scope.cluster) {

                                            EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                                                {
                                                    namespace: scope.toOperationNamespace
                                                });
                                        }
                                    }, function (result) {
                                        toastr.error(AppUtil.errorMsg(result), $translate.instant('ItemModal.AddFailed'));
                                        scope.item.addItemBtnDisabled = false;
                                    });
                        });
                    }

                } else {

                    if (!scope.item.comment) {
                        scope.item.comment = "";
                    }

                    ConfigService.update_item(scope.appId,
                        scope.env,
                        scope.toOperationNamespace.baseInfo.clusterName,
                        scope.toOperationNamespace.baseInfo.namespaceName,
                        scope.item).then(
                            function (result) {
                                EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                                    {
                                        namespace: scope.toOperationNamespace
                                    });

                                AppUtil.hideModal('#itemModal');

                                toastr.success($translate.instant('ItemModal.ModifiedTips'));
                            }, function (result) {
                                toastr.error(AppUtil.errorMsg(result), $translate.instant('ItemModal.ModifyFailed'));
                            });
                }

            }

            var selectedClusters = [];

            function collectSelectedClusters(data) {
                selectedClusters = data;
            }

            function detectJSON() {
                var value = scope.item.value;
                if (!value) {
                    scope.showJSONDetectContext = false;
                    return;
                }
                var res = "";
                try {
                    JSON.parse(value);
                    res = $translate.instant('Component.ConfigItem.ValidItemJSONValue')
                } catch(e) {
                    res = $translate.instant('Component.ConfigItem.InvalidItemJSONValue')
                }
                scope.showJSONDetectContext = true;
                scope.jsonDetectResult = $sce.trustAsHtml(res);
            }

            function showHiddenChars() {
                var value = scope.item.value;
                if (!value) {
                    return;
                }

                var hiddenCharCounter = 0, valueWithHiddenChars = _.escape(value);

                for (var i = 0; i < value.length; i++) {
                    var c = value[i];
                    if (isHiddenChar(c)) {
                        valueWithHiddenChars = valueWithHiddenChars.replace(c, viewHiddenChar);
                        hiddenCharCounter++;
                    }
                }

                scope.showHiddenCharsContext = true;
                scope.hiddenCharCounter = hiddenCharCounter;
                scope.valueWithHiddenChars = $sce.trustAsHtml(valueWithHiddenChars);

            }

            function isHiddenChar(c) {
                return c == '\t' || c == '\n' || c == ' ';
            }

            function viewHiddenChar(c) {

                if (c == '\t') {
                    return '<mark>#' + $translate.instant('ItemModal.Tabs') + '#</mark>';
                } else if (c == '\n') {
                    return '<mark>#' + $translate.instant('ItemModal.NewLine') + '#</mark>';
                } else if (c == ' ') {
                    return '<mark>#' + $translate.instant('ItemModal.Space') + '#</mark>';
                }

            }
        }
    }
}


