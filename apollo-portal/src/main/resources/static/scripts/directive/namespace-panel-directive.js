directive_module.directive('apollonspanel', directive);

function directive($window, $translate, toastr, AppUtil, EventManager, PermissionService, NamespaceLockService,
    UserService, CommitService, ReleaseService, InstanceService, NamespaceBranchService, ConfigService) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/namespace-panel.html',
        transclude: true,
        replace: true,
        scope: {
            namespace: '=',
            appId: '=',
            env: '=',
            cluster: '=',
            user: '=',
            lockCheck: '=',
            createItem: '=',
            editItem: '=',
            preDeleteItem: '=',
            preRevokeItem: '=',
            showText: '=',
            showNoModifyPermissionDialog: '=',
            preCreateBranch: '=',
            preDeleteBranch: '=',
            showMergeAndPublishGrayTips: '=',
            showBody: "=?",
            lazyLoad: "=?"
        },
        link: function (scope) {

            //constants
            var namespace_view_type = {
                TEXT: 'text',
                TABLE: 'table',
                HISTORY: 'history',
                INSTANCE: 'instance',
                RULE: 'rule'
            };

            var namespace_instance_view_type = {
                LATEST_RELEASE: 'latest_release',
                NOT_LATEST_RELEASE: 'not_latest_release',
                ALL: 'all'
            };

            var operate_branch_storage_key = 'OperateBranch';

            scope.refreshNamespace = refreshNamespace;
            scope.switchView = switchView;
            scope.toggleItemSearchInput = toggleItemSearchInput;
            scope.searchItems = searchItems;
            scope.loadCommitHistory = loadCommitHistory;
            scope.toggleTextEditStatus = toggleTextEditStatus;
            scope.goToSyncPage = goToSyncPage;
            scope.goToDiffPage = goToDiffPage;
            scope.modifyByText = modifyByText;
            scope.syntaxCheck = syntaxCheck;
            scope.goToParentAppConfigPage = goToParentAppConfigPage;
            scope.switchInstanceViewType = switchInstanceViewType;
            scope.switchBranch = switchBranch;
            scope.loadInstanceInfo = loadInstanceInfo;
            scope.refreshInstancesInfo = refreshInstancesInfo;
            scope.deleteRuleItem = deleteRuleItem;
            scope.rollback = rollback;

            scope.publish = publish;
            scope.mergeAndPublish = mergeAndPublish;
            scope.addRuleItem = addRuleItem;
            scope.editRuleItem = editRuleItem;

            scope.deleteNamespace = deleteNamespace;

            var subscriberId = EventManager.subscribe(EventManager.EventType.UPDATE_GRAY_RELEASE_RULES,
                function (context) {
                    useRules(context.branch);
                }, scope.namespace.baseInfo.namespaceName);

            scope.$on('$destroy', function () {
                EventManager.unsubscribe(EventManager.EventType.UPDATE_GRAY_RELEASE_RULES,
                    subscriberId, scope.namespace.baseInfo.namespaceName);
            });

            preInit(scope.namespace);

            if (!scope.lazyLoad || scope.namespace.initialized) {
                init();
            }

            function preInit(namespace) {
                scope.showNamespaceBody = false;
                namespace.isLinkedNamespace =
                    namespace.isPublic ? namespace.parentAppId != namespace.baseInfo.appId : false;
                //namespace view name hide suffix
                namespace.viewName = namespace.baseInfo.namespaceName.replace(".xml", "").replace(
                    ".properties", "").replace(".json", "").replace(".yml", "")
                    .replace(".yaml", "").replace(".txt", "");
            }

            function init() {
                initNamespace(scope.namespace);
                initOther();
                scope.namespace.initialized = true;
            }

            function refreshNamespace() {
                EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                    { namespace: scope.namespace });
            }

            function initNamespace(namespace, viewType) {
                namespace.hasBranch = false;
                namespace.isBranch = false;
                namespace.displayControl = {
                    currentOperateBranch: 'master',
                    showSearchInput: false,
                    show: scope.showBody
                };
                scope.showNamespaceBody = namespace.showNamespaceBody ? true : scope.showBody;
                namespace.viewItems = namespace.items;
                namespace.isPropertiesFormat = namespace.format == 'properties';
                namespace.isSyntaxCheckable = namespace.format == 'yaml' || namespace.format == 'yml';
                namespace.isTextEditing = false;
                namespace.instanceViewType = namespace_instance_view_type.LATEST_RELEASE;
                namespace.latestReleaseInstancesPage = 0;
                namespace.allInstances = [];
                namespace.allInstancesPage = 0;
                namespace.commitChangeBtnDisabled = false;

                generateNamespaceId(namespace);
                initNamespaceBranch(namespace);
                initNamespaceViewName(namespace);
                initNamespaceLock(namespace);
                initNamespaceInstancesCount(namespace);
                initPermission(namespace);
                initLinkedNamespace(namespace);
                loadInstanceInfo(namespace);

                function initNamespaceBranch(namespace) {
                    NamespaceBranchService.findNamespaceBranch(scope.appId, scope.env,
                        namespace.baseInfo.clusterName,
                        namespace.baseInfo.namespaceName)
                        .then(function (result) {

                            if (!result.baseInfo) {
                                return;
                            }

                            //namespace has branch
                            namespace.hasBranch = true;
                            namespace.branchName = result.baseInfo.clusterName;
                            //init branch
                            namespace.branch = result;
                            namespace.branch.isBranch = true;
                            namespace.branch.parentNamespace = namespace;
                            namespace.branch.viewType = namespace_view_type.TABLE;
                            namespace.branch.isPropertiesFormat = namespace.format == 'properties';
                            namespace.branch.allInstances = [];//master namespace all instances
                            namespace.branch.latestReleaseInstances = [];
                            namespace.branch.latestReleaseInstancesPage = 0;
                            namespace.branch.instanceViewType = namespace_instance_view_type.LATEST_RELEASE;
                            namespace.branch.hasLoadInstances = false;
                            namespace.branch.displayControl = {
                                show: true
                            };

                            generateNamespaceId(namespace.branch);
                            initBranchItems(namespace.branch);
                            initRules(namespace.branch);
                            loadInstanceInfo(namespace.branch);
                            initNamespaceLock(namespace.branch);
                            initPermission(namespace);
                            initUserOperateBranchScene(namespace);
                        });

                    function initBranchItems(branch) {
                        branch.masterItems = [];
                        branch.branchItems = [];

                        var masterItemsMap = {};
                        branch.parentNamespace.items.forEach(function (item) {
                            if (item.item.key) {
                                masterItemsMap[item.item.key] = item;
                            }
                        });

                        var branchItemsMap = {};

                        var itemModifiedCnt = 0;
                        branch.items.forEach(function (item) {
                            var key = item.item.key;
                            var masterItem = masterItemsMap[key];

                            //modify master item and set item's masterReleaseValue
                            if (masterItem) {
                                item.masterItemExists = true;
                                if (masterItem.isModified) {
                                    item.masterReleaseValue = masterItem.oldValue;
                                } else {
                                    item.masterReleaseValue = masterItem.item.value;
                                }

                            } else {//delete branch item
                                item.masterItemExists = false;
                            }

                            //delete master item. ignore
                            if (item.isDeleted && masterItem) {
                                if (item.masterReleaseValue != item.oldValue) {
                                    itemModifiedCnt++;
                                    branch.branchItems.push(item);
                                }
                            } else {//branch's item
                                branchItemsMap[key] = item;

                                if (item.isModified) {
                                    itemModifiedCnt++;
                                }
                                branch.branchItems.push(item);
                            }

                        });
                        branch.itemModifiedCnt = itemModifiedCnt;

                        branch.parentNamespace.items.forEach(function (item) {
                            if (item.item.key) {
                                if (!branchItemsMap[item.item.key]) {
                                    branch.masterItems.push(item);
                                } else {
                                    item.hasBranchValue = true;
                                }
                            }
                        })

                    }
                }

                function generateNamespaceId(namespace) {
                    namespace.id = Math.random().toString(36).substr(2);
                }

                function initPermission(namespace) {

                    PermissionService.has_modify_namespace_permission(
                        scope.appId,
                        namespace.baseInfo.namespaceName)
                        .then(function (result) {
                            if (!result.hasPermission) {
                                PermissionService.has_modify_namespace_env_permission(
                                    scope.appId,
                                    scope.env,
                                    namespace.baseInfo.namespaceName
                                )
                                    .then(function (result) {
                                        //branch has same permission
                                        namespace.hasModifyPermission = result.hasPermission;
                                        if (namespace.branch) {
                                            namespace.branch.hasModifyPermission = result.hasPermission;
                                        }
                                    });
                            }
                            else {
                                //branch has same permission
                                namespace.hasModifyPermission = result.hasPermission;
                                if (namespace.branch) {
                                    namespace.branch.hasModifyPermission = result.hasPermission;
                                }
                            }
                        });

                    PermissionService.has_release_namespace_permission(
                        scope.appId,
                        namespace.baseInfo.namespaceName)
                        .then(function (result) {
                            if (!result.hasPermission) {
                                PermissionService.has_release_namespace_env_permission(
                                    scope.appId,
                                    scope.env,
                                    namespace.baseInfo.namespaceName
                                )
                                    .then(function (result) {
                                        //branch has same permission
                                        namespace.hasReleasePermission = result.hasPermission;
                                        if (namespace.branch) {
                                            namespace.branch.hasReleasePermission = result.hasPermission;
                                        }
                                    });
                            }
                            else {
                                //branch has same permission
                                namespace.hasReleasePermission = result.hasPermission;
                                if (namespace.branch) {
                                    namespace.branch.hasReleasePermission = result.hasPermission;
                                }
                            }
                        });
                }

                function initLinkedNamespace(namespace) {
                    if (!namespace.isPublic || !namespace.isLinkedNamespace || namespace.format != 'properties') {
                        return;
                    }
                    //load public namespace
                    ConfigService.load_public_namespace_for_associated_namespace(scope.env, scope.appId, scope.cluster,
                        namespace.baseInfo.namespaceName)
                        .then(function (result) {
                            var publicNamespace = result;
                            namespace.publicNamespace = publicNamespace;

                            var linkNamespaceItemKeys = [];
                            namespace.items.forEach(function (item) {
                                var key = item.item.key;
                                linkNamespaceItemKeys.push(key);
                            });

                            publicNamespace.viewItems = [];
                            publicNamespace.items.forEach(function (item) {
                                var key = item.item.key;

                                if (key) {
                                    publicNamespace.viewItems.push(item);
                                }

                                item.covered = linkNamespaceItemKeys.indexOf(key) >= 0;

                                if (item.isModified || item.isDeleted) {
                                    publicNamespace.isModified = true;
                                } else if (key) {
                                    publicNamespace.hasPublishedItem = true;
                                }
                            });

                        });

                }

                function initNamespaceViewName(namespace) {
                    if (!viewType) {
                        if (namespace.isPropertiesFormat) {
                            switchView(namespace, namespace_view_type.TABLE);
                        } else {
                            switchView(namespace, namespace_view_type.TEXT);
                        }
                    } else if (viewType == namespace_view_type.TABLE) {
                        namespace.viewType = namespace_view_type.TABLE;
                    }
                }

                function initNamespaceLock(namespace) {
                    NamespaceLockService.get_namespace_lock(scope.appId, scope.env,
                        namespace.baseInfo.clusterName,
                        namespace.baseInfo.namespaceName)
                        .then(function (result) {
                            namespace.lockOwner = result.lockOwner;
                            namespace.isEmergencyPublishAllowed = result.isEmergencyPublishAllowed;
                        });

                }

                function initUserOperateBranchScene(namespace) {
                    var operateBranchStorage = JSON.parse(localStorage.getItem(operate_branch_storage_key));
                    var namespaceId = [scope.appId, scope.env, scope.cluster, namespace.baseInfo.namespaceName].join(
                        "+");
                    if (!operateBranchStorage) {
                        operateBranchStorage = {};
                    }
                    if (!operateBranchStorage[namespaceId]) {
                        operateBranchStorage[namespaceId] = namespace.branchName;
                    }

                    localStorage.setItem(operate_branch_storage_key, JSON.stringify(operateBranchStorage));

                    switchBranch(operateBranchStorage[namespaceId], false);

                }

            }

            function initNamespaceInstancesCount(namespace) {
                InstanceService.getInstanceCountByNamespace(scope.appId,
                    scope.env,
                    scope.cluster,
                    namespace.baseInfo.namespaceName)
                    .then(function (result) {
                        namespace.instancesCount = result.num;
                    })
            }

            function initOther() {

                UserService.load_user().then(function (result) {
                    scope.currentUser = result.userId;
                });

                PermissionService.has_assign_user_permission(scope.appId)
                    .then(function (result) {
                        scope.hasAssignUserPermission = result.hasPermission;
                    }, function (result) {

                    });
            }

            function switchBranch(branchName, forceShowBody) {
                if (branchName != 'master') {
                    initRules(scope.namespace.branch);
                }
                if (forceShowBody) {
                    scope.showNamespaceBody = true;
                }

                scope.namespace.displayControl.currentOperateBranch = branchName;

                //save to local storage
                var operateBranchStorage = JSON.parse(localStorage.getItem(operate_branch_storage_key));
                if (!operateBranchStorage) {
                    return;
                }
                var namespaceId = [scope.appId, scope.env, scope.cluster, scope.namespace.baseInfo.namespaceName].join(
                    "+");
                operateBranchStorage[namespaceId] = branchName;
                localStorage.setItem(operate_branch_storage_key, JSON.stringify(operateBranchStorage));

            }

            function switchView(namespace, viewType) {
                namespace.viewType = viewType;
                if (namespace_view_type.TEXT == viewType) {
                    namespace.text = parseModel2Text(namespace);
                } else if (namespace_view_type.TABLE == viewType) {

                } else if (namespace_view_type.HISTORY == viewType) {
                    loadCommitHistory(namespace);
                } else if (namespace_view_type.INSTANCE == viewType) {
                    refreshInstancesInfo(namespace);
                }
            }

            function switchInstanceViewType(namespace, type) {
                namespace.instanceViewType = type;
                loadInstanceInfo(namespace);
            }

            function loadCommitHistory(namespace) {
                if (!namespace.commits) {
                    namespace.commits = [];
                    namespace.commitPage = 0;
                }

                var size = 10;
                CommitService.find_commits(scope.appId,
                    scope.env,
                    namespace.baseInfo.clusterName,
                    namespace.baseInfo.namespaceName,
                    namespace.commitPage,
                    size)
                    .then(function (result) {
                        if (result.length < size) {
                            namespace.hasLoadAllCommit = true;
                        }

                        for (var i = 0; i < result.length; i++) {
                            //to json
                            result[i].changeSets = JSON.parse(result[i].changeSets);
                            namespace.commits.push(result[i]);
                        }
                        namespace.commitPage += 1;
                    }, function (result) {
                        toastr.error(AppUtil.errorMsg(result), $translate.instant('ApolloNsPanel.LoadingHistoryError'));
                    });
            }

            function loadInstanceInfo(namespace) {

                var size = 20;
                if (namespace.isBranch) {
                    size = 2000;
                }

                var type = namespace.instanceViewType;

                if (namespace_instance_view_type.LATEST_RELEASE == type) {
                    if (!namespace.latestRelease) {
                        ReleaseService.findLatestActiveRelease(scope.appId,
                            scope.env,
                            namespace.baseInfo.clusterName,
                            namespace.baseInfo.namespaceName)
                            .then(function (result) {
                                namespace.isLatestReleaseLoaded = true;

                                if (!result) {
                                    namespace.latestReleaseInstances = {};
                                    namespace.latestReleaseInstances.total = 0;
                                    return;
                                }
                                namespace.latestRelease = result;
                                InstanceService.findInstancesByRelease(scope.env,
                                    namespace.latestRelease.id,
                                    namespace.latestReleaseInstancesPage,
                                    size)
                                    .then(function (result) {
                                        namespace.latestReleaseInstances = result;
                                        namespace.latestReleaseInstancesPage++;
                                    })
                            });
                    } else {
                        InstanceService.findInstancesByRelease(scope.env,
                            namespace.latestRelease.id,
                            namespace.latestReleaseInstancesPage,
                            size)
                            .then(function (result) {
                                if (result && result.content.length) {
                                    namespace.latestReleaseInstancesPage++;
                                    result.content.forEach(function (instance) {
                                        namespace.latestReleaseInstances.content.push(
                                            instance);
                                    })
                                }

                            })
                    }

                } else if (namespace_instance_view_type.NOT_LATEST_RELEASE == type) {
                    if (!namespace.latestRelease) {
                        return;
                    }
                    InstanceService.findByReleasesNotIn(scope.appId,
                        scope.env,
                        scope.cluster,
                        namespace.baseInfo.namespaceName,
                        namespace.latestRelease.id)
                        .then(function (result) {
                            if (!result || result.length == 0) {
                                return
                            }

                            var groupedInstances = {},
                                notLatestReleases = [];

                            result.forEach(function (instance) {
                                var configs = instance.configs;
                                if (configs && configs.length > 0) {
                                    configs.forEach(function (instanceConfig) {
                                        var release = instanceConfig.release;
                                        //filter dirty data
                                        if (!release) {
                                            return;
                                        }
                                        if (!groupedInstances[release.id]) {
                                            groupedInstances[release.id] = [];
                                            notLatestReleases.push(release);
                                        }
                                        groupedInstances[release.id].push(instance);
                                    })
                                }
                            });

                            namespace.notLatestReleases = notLatestReleases;
                            namespace.notLatestReleaseInstances = groupedInstances;
                        })

                } else {
                    InstanceService.findInstancesByNamespace(scope.appId,
                        scope.env,
                        scope.cluster,
                        namespace.baseInfo.namespaceName,
                        '',
                        namespace.allInstancesPage)
                        .then(function (result) {
                            if (result && result.content.length) {
                                namespace.allInstancesPage++;
                                result.content.forEach(function (instance) {
                                    namespace.allInstances.push(instance);
                                })
                            }
                        });
                }

            }

            function refreshInstancesInfo(namespace) {

                namespace.instanceViewType = namespace_instance_view_type.LATEST_RELEASE;

                namespace.latestReleaseInstancesPage = 0;
                namespace.latestReleaseInstances = [];
                namespace.latestRelease = undefined;

                if (!namespace.isBranch) {
                    namespace.notLatestReleaseNames = [];
                    namespace.notLatestReleaseInstances = {};

                    namespace.allInstancesPage = 0;
                    namespace.allInstances = [];
                }

                initNamespaceInstancesCount(namespace);
                loadInstanceInfo(namespace);
            }

            function initRules(branch) {

                NamespaceBranchService.findBranchGrayRules(scope.appId,
                    scope.env,
                    scope.cluster,
                    scope.namespace.baseInfo.namespaceName,
                    branch.baseInfo.clusterName)
                    .then(function (result) {

                        if (result.appId) {
                            branch.rules = result;
                        }

                    }, function (result) {
                        toastr.error(AppUtil.errorMsg(result), $translate.instant('ApolloNsPanel.LoadingGrayscaleError'));
                    });

            }

            function addRuleItem(branch) {
                var newRuleItem = {
                    clientAppId: !branch.parentNamespace.isPublic ? branch.baseInfo.appId : '',
                    clientIpList: [],
                    draftIpList: [],
                    isNew: true
                };

                branch.editingRuleItem = newRuleItem;

                EventManager.emit(EventManager.EventType.EDIT_GRAY_RELEASE_RULES, {
                    branch: branch
                });
            }

            function editRuleItem(branch, ruleItem) {
                ruleItem.isNew = false;
                ruleItem.draftIpList = _.clone(ruleItem.clientIpList);
                branch.editingRuleItem = ruleItem;

                EventManager.emit(EventManager.EventType.EDIT_GRAY_RELEASE_RULES, {
                    branch: branch
                });
            }

            function deleteRuleItem(branch, ruleItem) {
                branch.rules.ruleItems.forEach(function (item, index) {
                    if (item.clientAppId == ruleItem.clientAppId) {
                        branch.rules.ruleItems.splice(index, 1);
                        toastr.success($translate.instant('ApolloNsPanel.Deleted'));
                    }
                });

                useRules(branch);
            }

            function useRules(branch) {
                NamespaceBranchService.updateBranchGrayRules(scope.appId,
                    scope.env,
                    scope.cluster,
                    scope.namespace.baseInfo.namespaceName,
                    branch.baseInfo.clusterName,
                    branch.rules
                )
                    .then(function (result) {
                        toastr.success($translate.instant('ApolloNsPanel.GrayscaleModified'));

                        //show tips if branch has not release configs
                        if (branch.itemModifiedCnt) {
                            AppUtil.showModal("#updateRuleTips");
                        }

                        setTimeout(function () {
                            refreshInstancesInfo(branch);
                        }, 1500);

                    }, function (result) {
                        AppUtil.showErrorMsg(result, $translate.instant('ApolloNsPanel.GrayscaleModifyFailed'));
                    })
            }

            function toggleTextEditStatus(namespace) {
                if (!scope.lockCheck(namespace)) {
                    return;
                }
                namespace.isTextEditing = !namespace.isTextEditing;
                if (namespace.isTextEditing) {//切换为编辑状态
                    namespace.commited = false;
                    namespace.backupText = namespace.text;
                    namespace.editText = parseModel2Text(namespace);

                } else {
                    if (!namespace.commited) {//取消编辑,则复原
                        namespace.text = namespace.backupText;
                    }
                }
            }

            function goToSyncPage(namespace) {
                if (!scope.lockCheck(namespace)) {
                    return false;
                }
                $window.location.href =
                AppUtil.prefixPath() + "/config/sync.html?#/appid=" + scope.appId + "&env="
                    + scope.env + "&clusterName="
                    + scope.cluster
                    + "&namespaceName=" + namespace.baseInfo.namespaceName;
            }

            function goToDiffPage(namespace) {
                $window.location.href =
                AppUtil.prefixPath() + "/config/diff.html?#/appid=" + scope.appId + "&env="
                    + scope.env + "&clusterName="
                    + scope.cluster
                    + "&namespaceName=" + namespace.baseInfo.namespaceName;
            }

            function modifyByText(namespace) {
                var model = {
                    configText: namespace.editText,
                    namespaceId: namespace.baseInfo.id,
                    format: namespace.format
                };

                //prevent repeat submit
                if (namespace.commitChangeBtnDisabled) {
                    return;
                }
                namespace.commitChangeBtnDisabled = true;
                ConfigService.modify_items(scope.appId,
                    scope.env,
                    scope.cluster,
                    namespace.baseInfo.namespaceName,
                    model).then(
                        function (result) {
                            toastr.success($translate.instant('ApolloNsPanel.ModifiedTips'));
                            //refresh all namespace items
                            EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                                {
                                    namespace: namespace
                                });
                            return true;

                        }, function (result) {
                            toastr.error(AppUtil.errorMsg(result), $translate.instant('ApolloNsPanel.ModifyFailed'));
                            namespace.commitChangeBtnDisabled = false;
                            return false;
                        }
                    );
                namespace.commited = true;
                toggleTextEditStatus(namespace);
            }

            function syntaxCheck(namespace) {
                var model = {
                    configText: namespace.editText,
                    namespaceId: namespace.baseInfo.id,
                    format: namespace.format
                };
                ConfigService.syntax_check_text(scope.appId,
                    scope.env,
                    scope.cluster,
                    namespace.baseInfo.namespaceName,
                    model).then(
                        function (result) {
                            toastr.success($translate.instant('ApolloNsPanel.GrammarIsRight'));

                        }, function (result) {
                            EventManager.emit(EventManager.EventType.SYNTAX_CHECK_TEXT_FAILED, {
                                syntaxCheckMessage: AppUtil.pureErrorMsg(result)
                            });
                        }
                    );
            }

            function goToParentAppConfigPage(namespace) {
                $window.location.href = AppUtil.prefixPath() + "/config.html?#/appid=" + namespace.parentAppId;
                $window.location.reload();
            }

            function parseModel2Text(namespace) {

                if (namespace.items.length == 0) {
                    namespace.itemCnt = 0;
                    return "";
                }

                //文件模式
                if (!namespace.isPropertiesFormat) {
                    return parseNotPropertiesText(namespace);
                } else {
                    return parsePropertiesText(namespace);
                }

            }

            function parseNotPropertiesText(namespace) {
                var text = namespace.items[0].item.value;
                var lineNum = text.split("\n").length;
                namespace.itemCnt = lineNum;
                return text;
            }

            function parsePropertiesText(namespace) {
                var result = "";
                var itemCnt = 0;
                namespace.items.forEach(function (item) {
                    //deleted key
                    if (!item.item.dataChangeLastModifiedBy) {
                        return;
                    }
                    if (item.item.key) {
                        //use string \n to display as new line
                        var itemValue = item.item.value.replace(/\n/g, "\\n");

                        result +=
                            item.item.key + " = " + itemValue + "\n";
                    } else {
                        result += item.item.comment + "\n";
                    }
                    itemCnt++;
                });

                namespace.itemCnt = itemCnt;
                return result;
            }

            function toggleItemSearchInput(namespace) {
                namespace.displayControl.showSearchInput = !namespace.displayControl.showSearchInput;
            }

            function searchItems(namespace) {
                var searchKey = namespace.searchKey.toLowerCase();
                var items = [];
                namespace.items.forEach(function (item) {
                    var key = item.item.key;
                    if (key && key.toLowerCase().indexOf(searchKey) >= 0) {
                        items.push(item);
                    }
                });
                namespace.viewItems = items;
            }

            //normal release and gray release
            function publish(namespace) {

                if (!namespace.hasReleasePermission) {
                    AppUtil.showModal('#releaseNoPermissionDialog');
                    return;
                } else if (namespace.lockOwner && scope.user == namespace.lockOwner) {
                    //can not publish if config modified by himself
                    EventManager.emit(EventManager.EventType.PUBLISH_DENY, {
                        namespace: namespace,
                        mergeAndPublish: false
                    });
                    return;
                }

                if (namespace.isBranch) {
                    namespace.mergeAndPublish = false;
                }

                EventManager.emit(EventManager.EventType.PUBLISH_NAMESPACE,
                    {
                        namespace: namespace
                    });
            }

            function mergeAndPublish(branch) {
                var parentNamespace = branch.parentNamespace;
                if (!parentNamespace.hasReleasePermission) {
                    AppUtil.showModal('#releaseNoPermissionDialog');
                } else if (parentNamespace.itemModifiedCnt > 0) {
                    AppUtil.showModal('#mergeAndReleaseDenyDialog');
                } else if (branch.lockOwner && scope.user == branch.lockOwner) {
                    EventManager.emit(EventManager.EventType.PUBLISH_DENY, {
                        namespace: branch,
                        mergeAndPublish: true
                    });
                } else {
                    EventManager.emit(EventManager.EventType.MERGE_AND_PUBLISH_NAMESPACE, { branch: branch });
                }
            }

            function rollback(namespace) {
                EventManager.emit(EventManager.EventType.PRE_ROLLBACK_NAMESPACE, { namespace: namespace });
            }

            function deleteNamespace(namespace) {
                EventManager.emit(EventManager.EventType.PRE_DELETE_NAMESPACE, { namespace: namespace });
            }

            //theme: https://github.com/ajaxorg/ace-builds/tree/ba3b91e04a5aa559d56ac70964f9054baa0f4caf/src-min
            scope.aceConfig = {
                $blockScrolling: Infinity,
                showPrintMargin: false,
                theme: 'eclipse',
                mode: scope.namespace.format === 'yml' ? 'yaml' : (scope.namespace.format === 'txt' ? undefined : scope.namespace.format),
                onLoad: function (_editor) {
                    _editor.$blockScrolling = Infinity;
                    _editor.setOptions({
                        fontSize: 13,
                        minLines: 10,
                        maxLines: 20
                    })
                }
            };

            setTimeout(function () {
                scope.namespace.show = true;
            }, 70);


        }
    }
}

