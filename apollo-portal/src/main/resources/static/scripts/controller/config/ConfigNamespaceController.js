application_module.controller("ConfigNamespaceController",
    ['$rootScope', '$scope', '$translate', 'toastr', 'AppUtil', 'EventManager', 'ConfigService',
        'PermissionService', 'UserService', 'NamespaceBranchService', 'NamespaceService',
        controller]);

function controller($rootScope, $scope, $translate, toastr, AppUtil, EventManager, ConfigService,
    PermissionService, UserService, NamespaceBranchService, NamespaceService) {

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
                refreshAllNamespaces();
            }

        });

    function refreshAllNamespaces() {
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
                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.LoadingAllNamespaceError'));
                });
    }

    function refreshSingleNamespace(namespace) {
        if ($rootScope.pageContext.env == '') {
            return;
        }

        ConfigService.load_namespace($rootScope.pageContext.appId,
            $rootScope.pageContext.env,
            $rootScope.pageContext.clusterName,
            namespace.baseInfo.namespaceName).then(
                function (result) {

                    $scope.namespaces.forEach(function (namespace, index) {
                        if (namespace.baseInfo.namespaceName == result.baseInfo.namespaceName) {
                            result.showNamespaceBody = true;
                            result.initialized = true;
                            result.show = namespace.show;
                            $scope.namespaces[index] = result;
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

    //修改配置
    function editItem(namespace, toEditItem) {
        if (!lockCheck(namespace)) {
            return;
        }

        $scope.item = _.clone(toEditItem);

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

    EventManager.subscribe(EventManager.EventType.DELETE_NAMESPACE_FAILED, function (context) {
        $scope.deleteNamespaceContext = context;

        if (context.reason == 'master_instance') {
            AppUtil.showModal('#deleteNamespaceDenyForMasterInstanceDialog');
        } else if (context.reason == 'branch_instance') {
            AppUtil.showModal('#deleteNamespaceDenyForBranchInstanceDialog');
        } else if (context.reason == 'public_namespace') {
            var otherAppAssociatedNamespaces = context.otherAppAssociatedNamespaces;
            var namespaceTips = [];
            otherAppAssociatedNamespaces.forEach(function (namespace) {
                var appId = namespace.appId;
                var clusterName = namespace.clusterName;
                var url = AppUtil.prefixPath() + '/config.html?#/appid=' + appId + '&env=' + $scope.pageContext.env + '&cluster='
                    + clusterName;

                namespaceTips.push("<a target='_blank' href=\'" + url + "\'>AppId = " + appId + ", Cluster = " + clusterName
                    + ", Namespace = " + namespace.namespaceName + "</a>");
            });

            $scope.deleteNamespaceContext.detailReason = $translate.instant('Config.DeleteNamespaceFailedTips') + "<br>" + namespaceTips.join("<br>");

            AppUtil.showModal('#deleteNamespaceDenyForPublicNamespaceDialog');
        }

    });

    EventManager.subscribe(EventManager.EventType.SYNTAX_CHECK_TEXT_FAILED, function (context) {
        $scope.syntaxCheckContext = context;

        AppUtil.showModal('#syntaxCheckFailedDialog');
    });

    new Clipboard('.clipboard');


}


