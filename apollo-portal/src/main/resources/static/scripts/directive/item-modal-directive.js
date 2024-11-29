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
            scope.changeType = changeType;
            scope.validateItemValue = validateItemValue;
            scope.formatContent = formatContent;

            $('#itemModal').on('show.bs.modal', function (e) {
                scope.showHiddenCharsContext = false;
                scope.hiddenCharCounter = 0;
                scope.valueWithHiddenChars = $sce.trustAsHtml('');
            });

            $("#valueEditor").textareafullscreen();

            function validateItemValue() {
                if (scope.item.type === '1') {
                    //check whether the Number format is correct
                    let regNumber = /-[0-9]+(\\.[0-9]+)?|[0-9]+(\\.[0-9]+)?/;
                    if (regNumber.test(Number(scope.item.value)) === true && !(scope.item.value.trim() === '')) {
                        scope.showNumberError = false;
                    } else {
                        scope.showNumberError = true;
                    }
                } else if (scope.item.type === '3') {
                    detectJSON();
                } else {
                    scope.showNumberError = false;
                    scope.showJsonError = false;
                }
            }

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

            function changeType() {
                scope.showNumberError = false;
                scope.showJsonError = false;
                if (scope.item.type === '2') {
                    scope.item.lastValue = scope.item.value;
                    scope.item.value = 'false';
                } else {
                    if (scope.item.lastType === '2') {
                        scope.item.value = scope.item.lastValue;
                    } else {
                        // switch between 'String' 'Number' 'Json', the value is not changed.
                    }
                }
                scope.item.lastType = scope.item.type;
                validateItemValue();
            }

            function detectJSON() {
                var value = scope.item.value;
                if (!value) {
                    scope.showJsonError = true;
                    return;
                }
                try {
                    JSON.parse(value);
                    scope.showJsonError = false;
                } catch(e) {
                    scope.showJsonError = true;
                }
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
                return c == '\t' || c == '\n' || c == ' ' || c == '，';
            }

            function viewHiddenChar(c) {

                if (c == '\t') {
                    return '<mark>#' + $translate.instant('ItemModal.Tabs') + '#</mark>';
                } else if (c == '\n') {
                    return '<mark>#' + $translate.instant('ItemModal.NewLine') + '#</mark>';
                } else if (c == ' ') {
                    return '<mark>#' + $translate.instant('ItemModal.Space') + '#</mark>';
                } else if (c == '，') {
                    return '<mark>#' + $translate.instant('ItemModal.ChineseComma') + '#</mark>';
                }

            }

            // 格式化
            function formatContent() {
                if (scope.showJsonError) {
                    return;
                }
                var raw = scope.item.value;
                if (scope.item.type === '3') {
                    scope.item.value = JSON.stringify(JSON.parse(raw), null, 4);
                }
            }
        }
    }
}


