/*
 * Copyright 2023 Apollo Authors
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
application_module.controller("ConfigNamespaceController",
    ['$rootScope', '$scope', '$translate', 'toastr', 'AppUtil', 'EventManager', 'ConfigService',
        'PermissionService', 'UserService', 'NamespaceBranchService', 'NamespaceService',
        controller]);

function controller($rootScope, $scope, $translate, toastr, AppUtil, EventManager, ConfigService,
    PermissionService, UserService, NamespaceBranchService, NamespaceService) {

    $scope.diffItem = diffItem;
    $scope.rollback = rollback;
    $scope.preDeleteItem = preDeleteItem;
    $scope.deleteItem = deleteItem;
    $scope.editItem = editItem;
    $scope.createItem = createItem;
    $scope.preRevokeItem = preRevokeItem;
    $scope.revokeItem = revokeItem;
    $scope.closeTip = closeTip;
    $scope.showText = showText;
    $scope.createBranch = createBranch;
    $scope.preCreateBranch = preCreateBranch;
    $scope.preDeleteBranch = preDeleteBranch;
    $scope.deleteBranch = deleteBranch;
    $scope.showNoModifyPermissionDialog = showNoModifyPermissionDialog;
    $scope.lockCheck = lockCheck;
    $scope.emergencyPublish = emergencyPublish;
    $scope.searchKey = '';
    $scope.onlyShowDiffKeys = true;
    $scope.itemsKeyedByKey = {};
    $scope.allNamespaceValueEqualed = {};
    $scope.versions=[];
    $scope.oldStr = '';
    $scope.newStr = '';
    init();

    function init() {
        initRole();
        initUser();
        initPublishInfo();
    }
    function initRole() {
        PermissionService.get_app_role_users($rootScope.pageContext.appId)
            .then(function (result) {
                var masterUsers = '';
                result.masterUsers.forEach(function (user) {
                    masterUsers += _.escape(user.userId) + ',';
                });
                $scope.masterUsers = masterUsers.substring(0, masterUsers.length - 1);
            }, function (result) {

            });
    }

    function initUser() {
        UserService.load_user().then(function (result) {
            $scope.currentUser = result.userId;
        });

    }

    function initPublishInfo() {
        NamespaceService.getNamespacePublishInfo($rootScope.pageContext.appId)
            .then(function (result) {
                if (!result) {
                    return;
                }
                $scope.hasNotPublishNamespace = false;
                var namespacePublishInfo = [];

                Object.keys(result).forEach(function (env) {
                    if (env.indexOf("$") >= 0) {
                        return;
                    }

                    var envPublishInfo = result[env];
                    Object.keys(envPublishInfo).forEach(function (cluster) {

                        var clusterPublishInfo = envPublishInfo[cluster];
                        if (clusterPublishInfo) {
                            $scope.hasNotPublishNamespace = true;

                            if (Object.keys(envPublishInfo).length > 1) {
                                namespacePublishInfo.push("[" + env + ", " + cluster + "]");
                            } else {
                                namespacePublishInfo.push("[" + env + "]");
                            }

                        }
                    })
                });

                $scope.namespacePublishInfo = namespacePublishInfo;
            });

    }

    EventManager.subscribe(EventManager.EventType.REFRESH_NAMESPACE,
        function (context) {
            if (context.namespace) {
                refreshSingleNamespace(context.namespace);
            } else {
                refreshAllNamespaces(context);
            }

        });

    function refreshAllNamespaces(context) {
        if ($rootScope.pageContext.env == '') {
            return;
        }

        ConfigService.load_all_namespaces($rootScope.pageContext.appId,
            $rootScope.pageContext.env,
            $rootScope.pageContext.clusterName).then(
                function (result) {

                    $scope.namespaces = result;
                    $('.config-item-container').removeClass('hide');

                    initPublishInfo();
                    //If there is a namespace parameter in the URL, expand the corresponding namespace directly
                    if (context && context.firstLoad && $rootScope.pageContext.namespaceName) {
                        refreshSingleNamespace({
                            baseInfo: {
                                namespaceName: $rootScope.pageContext.namespaceName
                            },
                            searchInfo: {
                                showSearchInput: true,
                                searchItemKey: $rootScope.pageContext.item,
                            }

                        });
                    }
                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.LoadingAllNamespaceError'));
                });
    }

    function refreshSingleNamespace(namespace) {
        if ($rootScope.pageContext.env == '') {
            return;
        }

        const showSearchItemInput = namespace.searchInfo ? namespace.searchInfo.showSearchInput : false;
        const searchItemKey = namespace.searchInfo ? namespace.searchInfo.searchItemKey : '';

        ConfigService.load_namespace($rootScope.pageContext.appId,
            $rootScope.pageContext.env,
            $rootScope.pageContext.clusterName,
            namespace.baseInfo.namespaceName).then(
                function (result) {

                    $scope.namespaces.forEach(function (namespace, index) {
                        if (namespace.baseInfo.namespaceName === result.baseInfo.namespaceName) {
                            result.showNamespaceBody = true;
                            result.initialized = true;
                            result.show = namespace.show;
                            $scope.namespaces[index] = result;
                            result.showSearchItemInput = showSearchItemInput;
                            result.searchItemKey = searchItemKey;
                        }
                    });

                    initPublishInfo();

                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.LoadingAllNamespaceError'));
                });
    }

    function rollback() {
        EventManager.emit(EventManager.EventType.ROLLBACK_NAMESPACE);
    }

    $scope.tableViewOperType = '', $scope.item = {};
    $scope.toOperationNamespace;

    var toDeleteItemId = 0;

    function preDeleteItem(namespace, item) {
        if (!lockCheck(namespace)) {
            return;
        }

        $scope.config = {};
        $scope.config.key = _.escape(item.key);
        $scope.config.value = _.escape(item.value);
        $scope.toOperationNamespace = namespace;
        toDeleteItemId = item.id;

        $("#deleteConfirmDialog").modal("show");
    }

    function deleteItem() {
        ConfigService.delete_item($rootScope.pageContext.appId,
            $rootScope.pageContext.env,
            $scope.toOperationNamespace.baseInfo.clusterName,
            $scope.toOperationNamespace.baseInfo.namespaceName,
            toDeleteItemId).then(
                function (result) {
                    toastr.success($translate.instant('Config.Deleted'));
                    EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                        {
                            namespace: $scope.toOperationNamespace
                        });
                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.DeleteFailed'));
                });
    }

    function preRevokeItem(namespace) {
        if (!lockCheck(namespace)) {
            return;
        }
        $scope.toOperationNamespace = namespace;
        toRevokeItemId = namespace.baseInfo.id;
        $("#revokeItemConfirmDialog").modal("show");
    }

    function revokeItem() {
        ConfigService.revoke_item($rootScope.pageContext.appId,
            $rootScope.pageContext.env,
            $scope.toOperationNamespace.baseInfo.clusterName,
            $scope.toOperationNamespace.baseInfo.namespaceName).then(
            function (result) {
                toastr.success($translate.instant('Revoke.RevokeSuccessfully'));
                EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                    {
                        namespace: $scope.toOperationNamespace
                    });

            }, function (result) {
                toastr.error(AppUtil.errorMsg(result), $translate.instant('Revoke.RevokeFailed'));
            }
        );
    }

    function diffItem(namespace,masterValue,branchValue){
        $scope.searchKey = '';
        $scope.onlyShowDiffKeys = true;
        $scope.itemsKeyedByKey = {};
        $scope.allNamespaceValueEqualed = {};
        $scope.versions = [];

        $scope.versions.push("master");
        $scope.versions.push("branch");
        $scope.oldStr = masterValue;
        $scope.newStr = branchValue;
        let suffix = '';
        if (namespace.baseInfo.namespaceName.includes('.')) {
            suffix = namespace.baseInfo.namespaceName.match(/[^.]+$/)[0];
        }
        let res1 = [];
        let res2 = [];
        if (suffix === 'yml' || suffix === 'yaml') {
            res1 = Obj2Prop(
                YAML.parse(masterValue));
            res2 = Obj2Prop(
                YAML.parse(branchValue))
        } else if (suffix === 'json') {
            res1 = Obj2Prop(
                JSON.parse(masterValue));
            res2 = Obj2Prop(
                JSON.parse(branchValue))
        } else if (suffix === 'xml') {
            const x2js = new X2JS();
            res1 = Obj2Prop(
                x2js.xml_str2json(masterValue));
            res2 = Obj2Prop(
                x2js.xml_str2json(branchValue))
        } else {
            //txt
            const masterItem = {};
            const branchItem = {};
            masterItem["key"] = "content";
            masterItem["value"] = masterValue;
            branchItem["key"] = "content";
            branchItem["value"] = branchValue;
            res1.push(masterItem);
            res2.push(branchItem)
        }

        res1.forEach(function (item) {
            const itemsKeyedByVersion = $scope.itemsKeyedByKey[item.key] || {};
            itemsKeyedByVersion["master"] = item;
            $scope.itemsKeyedByKey[item.key] = itemsKeyedByVersion;
        });

        res2.forEach(function (item) {
            const itemsKeyedByVersion = $scope.itemsKeyedByKey[item.key] || {};
            itemsKeyedByVersion["branch"] = item;
            $scope.itemsKeyedByKey[item.key] = itemsKeyedByVersion;
        });

        Object.keys($scope.itemsKeyedByKey).forEach(
            function (key) {
                let lastValue = null;
                let allEqualed = true;
                // some namespace lack key,determined as not allEqual
                if (Object.keys($scope.itemsKeyedByKey[key]).length !== 2) {
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

        AppUtil.showModal('#diffModal');
    }
    //修改配置
    function editItem(namespace, toEditItem) {
        if (!lockCheck(namespace)) {
            return;
        }

        $scope.item = _.clone(toEditItem);
        $scope.item.type = String($scope.item.type || 0)
        if (namespace.isBranch || namespace.isLinkedNamespace) {
            var existedItem = false;
            namespace.items.forEach(function (item) {
                if (!item.isDeleted && item.item.key == toEditItem.key) {
                    existedItem = true;
                }
            });
            if (!existedItem) {
                $scope.item.lineNum = 0;
                $scope.item.tableViewOperType = 'create';
            } else {
                $scope.item.tableViewOperType = 'update';
            }

        } else {
            $scope.item.tableViewOperType = 'update';
        }

        $scope.toOperationNamespace = namespace;

        AppUtil.showModal('#itemModal');
    }

    //新增配置
    function createItem(namespace) {
        if (!lockCheck(namespace)) {
            return;
        }

        $scope.item = {
            tableViewOperType: 'create'
        };
        $scope.item.type = '0';
        $scope.showNumberError = false;
        $scope.showJsonError = false;

        $scope.toOperationNamespace = namespace;
        AppUtil.showModal('#itemModal');
    }

    var selectedClusters = [];
    $scope.collectSelectedClusters = function (data) {
        selectedClusters = data;
    };

    function lockCheck(namespace) {
        if (namespace.lockOwner && $scope.currentUser != namespace.lockOwner) {
            $scope.lockOwner = namespace.lockOwner;
            $('#namespaceLockedDialog').modal('show');
            return false;
        }
        return true;
    }

    function closeTip(clusterName) {
        var hideTip = JSON.parse(localStorage.getItem("hideTip"));
        if (!hideTip) {
            hideTip = {};
            hideTip[$rootScope.pageContext.appId] = {};
        }

        if (!hideTip[$rootScope.pageContext.appId]) {
            hideTip[$rootScope.pageContext.appId] = {};
        }

        hideTip[$rootScope.pageContext.appId][clusterName] = true;

        $rootScope.hideTip = hideTip;

        localStorage.setItem("hideTip", JSON.stringify(hideTip));

    }

    function showText(text) {
        $scope.text = text;
        $('#showTextModal').modal('show');
    }

    function showNoModifyPermissionDialog() {
        $("#modifyNoPermissionDialog").modal('show');
    }

    var toCreateBranchNamespace = {};

    function preCreateBranch(namespace) {
        toCreateBranchNamespace = namespace;
        AppUtil.showModal("#createBranchTips");
    }

    function createBranch() {
        NamespaceBranchService.createBranch($rootScope.pageContext.appId,
            $rootScope.pageContext.env,
            $rootScope.pageContext.clusterName,
            toCreateBranchNamespace.baseInfo.namespaceName)
            .then(function (result) {
                toastr.success($translate.instant('Config.GrayscaleCreated'));
                EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                    {
                        namespace: toCreateBranchNamespace
                    });
            }, function (result) {
                toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.GrayscaleCreateFailed'));
            })

    }

    function preDeleteBranch(branch) {
        //normal delete
        branch.branchStatus = 0;
        $scope.toDeleteBranch = branch;
        AppUtil.showModal('#deleteBranchDialog');
    }

    function deleteBranch() {
        NamespaceBranchService.deleteBranch($rootScope.pageContext.appId,
            $rootScope.pageContext.env,
            $rootScope.pageContext.clusterName,
            $scope.toDeleteBranch.baseInfo.namespaceName,
            $scope.toDeleteBranch.baseInfo.clusterName
        )
            .then(function (result) {
                toastr.success($translate.instant('Config.BranchDeleted'));
                EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                    {
                        namespace: $scope.toDeleteBranch.parentNamespace
                    });
            }, function (result) {
                toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.BranchDeleteFailed'));
            })

    }

    EventManager.subscribe(EventManager.EventType.EMERGENCY_PUBLISH,
        function (context) {
            AppUtil.showModal("#emergencyPublishAlertDialog");
            $scope.emergencyPublishContext = context;
        });

    function emergencyPublish() {
        if ($scope.emergencyPublishContext.mergeAndPublish) {

            EventManager.emit(EventManager.EventType.MERGE_AND_PUBLISH_NAMESPACE,
                {
                    branch: $scope.emergencyPublishContext.namespace,
                    isEmergencyPublish: true
                });
        } else {
            EventManager.emit(EventManager.EventType.PUBLISH_NAMESPACE,
                {
                    namespace: $scope.emergencyPublishContext.namespace,
                    isEmergencyPublish: true
                });
        }

    }

    EventManager.subscribe(EventManager.EventType.SYNTAX_CHECK_TEXT_FAILED, function (context) {
        $scope.syntaxCheckContext = context;

        AppUtil.showModal('#syntaxCheckFailedDialog');
    });

    new Clipboard('.clipboard');


}
function Obj2Prop(obj,prefix){
    let result = []
    const keys = Object.keys(obj)
    keys.forEach(function (key){
        let keyPrefix;
        if(obj[key] && typeof obj[key] == 'object'){
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
