directive_module.directive('deletenamespacemodal', deleteNamespaceModalDirective);

function deleteNamespaceModalDirective($window, $q, $translate, toastr, AppUtil, EventManager,
    PermissionService, UserService, NamespaceService) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/delete-namespace-modal.html',
        transclude: true,
        replace: true,
        scope: {
            env: '='
        },
        link: function (scope) {



            scope.doDeleteNamespace = doDeleteNamespace;

            EventManager.subscribe(EventManager.EventType.PRE_DELETE_NAMESPACE, function (context) {
                var toDeleteNamespace = context.namespace;
                scope.toDeleteNamespace = toDeleteNamespace;

                //1. check operator has master permission
                checkPermission(toDeleteNamespace).then(function () {

                    //2. check namespace's master branch has not instances
                    if (!checkMasterInstance(toDeleteNamespace)) {
                        return;
                    }

                    //3. check namespace's gray branch has not instances
                    if (!checkBranchInstance(toDeleteNamespace)) {
                        return;
                    }

                    if (!toDeleteNamespace.isPublic || toDeleteNamespace.isLinkedNamespace) {
                        showDeleteNamespaceConfirmDialog();
                    } else {
                        //5. check public namespace has not associated namespace
                        checkPublicNamespace(toDeleteNamespace).then(function () {
                            showDeleteNamespaceConfirmDialog();
                        });
                    }
                })

            });

            function checkPermission(namespace) {
                var d = $q.defer();

                UserService.load_user().then(function (currentUser) {

                    var isAppMasterUser = false;

                    PermissionService.get_app_role_users(namespace.baseInfo.appId)
                        .then(function (appRoleUsers) {

                            var masterUsers = [];

                            appRoleUsers.masterUsers.forEach(function (user) {
                                masterUsers.push(_.escape(user.userId));

                                if (currentUser.userId == user.userId) {
                                    isAppMasterUser = true;
                                }
                            });

                            scope.masterUsers = masterUsers;
                            scope.isAppMasterUser = isAppMasterUser;

                            if (!isAppMasterUser) {
                                toastr.error($translate.instant('Config.DeleteNamespaceNoPermissionFailedTips', {
                                    users: scope.masterUsers.join(", ")
                                }), $translate.instant('Config.DeleteNamespaceNoPermissionFailedTitle'));
                                d.reject();
                            } else {
                                d.resolve();
                            }
                        });
                });

                return d.promise;
            }

            function checkMasterInstance(namespace) {
                if (namespace.instancesCount > 0) {
                    EventManager.emit(EventManager.EventType.DELETE_NAMESPACE_FAILED, {
                        namespace: namespace,
                        reason: 'master_instance'
                    });

                    return false;
                }

                return true;
            }

            function checkBranchInstance(namespace) {
                if (namespace.hasBranch && namespace.branch.latestReleaseInstances.total > 0) {
                    EventManager.emit(EventManager.EventType.DELETE_NAMESPACE_FAILED, {
                        namespace: namespace,
                        reason: 'branch_instance'
                    });

                    return false;
                }

                return true;
            }

            function checkPublicNamespace(namespace) {
                var d = $q.defer();

                var publicAppId = namespace.baseInfo.appId;
                NamespaceService.getPublicAppNamespaceAllNamespaces(scope.env,
                    namespace.baseInfo.namespaceName,
                    0, 20)
                    .then(function (associatedNamespaces) {
                        var otherAppAssociatedNamespaces = [];
                        associatedNamespaces.forEach(function (associatedNamespace) {
                            if (associatedNamespace.appId != publicAppId) {
                                otherAppAssociatedNamespaces.push(associatedNamespace);
                            }
                        });

                        if (otherAppAssociatedNamespaces.length) {
                            EventManager.emit(EventManager.EventType.DELETE_NAMESPACE_FAILED, {
                                namespace: namespace,
                                reason: 'public_namespace',
                                otherAppAssociatedNamespaces: otherAppAssociatedNamespaces
                            });
                            d.reject();
                        } else {
                            d.resolve();
                        }

                    });

                return d.promise;

            }

            function showDeleteNamespaceConfirmDialog() {
                AppUtil.showModal('#deleteNamespaceModal');
            }

            function doDeleteNamespace() {
                var toDeleteNamespace = scope.toDeleteNamespace;
                NamespaceService.deleteNamespace(toDeleteNamespace.baseInfo.appId, scope.env,
                    toDeleteNamespace.baseInfo.clusterName,
                    toDeleteNamespace.baseInfo.namespaceName)
                    .then(function () {
                        toastr.success($translate.instant('Common.Deleted'));

                        setTimeout(function () {
                            $window.location.reload();
                        }, 1000);

                    }, function (result) {
                        AppUtil.showErrorMsg(result, $translate.instant('Common.DeleteFailed'));
                    })

            }

        }
    }
}



