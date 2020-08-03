directive_module.directive('rollbackmodal', rollbackModalDirective);

function rollbackModalDirective($translate, AppUtil, EventManager, ReleaseService, toastr) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/rollback-modal.html',
        transclude: true,
        replace: true,
        scope: {
            appId: '=',
            env: '=',
            cluster: '='
        },
        link: function (scope) {

            scope.isRollbackTo = false;
            scope.showRollbackAlertDialog = showRollbackAlertDialog;

            EventManager.subscribe(EventManager.EventType.PRE_ROLLBACK_NAMESPACE,
                function (context) {
                    if (context.toReleaseId) {
                        preRollbackTo(context.namespace, context.toReleaseId);
                    } else {
                        preRollback(context.namespace);
                    }
                });

            EventManager.subscribe(EventManager.EventType.ROLLBACK_NAMESPACE,
                function (context) {
                    if (context.toReleaseId) {
                        rollbackTo(context.toReleaseId);
                    } else {
                        rollback();
                    }
                });

            function preRollback(namespace) {
                scope.toRollbackNamespace = namespace;
                //load latest two active releases
                ReleaseService.findActiveReleases(scope.appId,
                    scope.env,
                    scope.cluster,
                    scope.toRollbackNamespace.baseInfo.namespaceName,
                    0, 2)
                    .then(function (result) {
                        if (result.length <= 1) {
                            toastr.error($translate.instant('Rollback.NoRollbackList'));
                            return;
                        }
                        scope.toRollbackNamespace.firstRelease = result[0];
                        scope.toRollbackNamespace.secondRelease = result[1];

                        ReleaseService.compare(scope.env,
                            scope.toRollbackNamespace.firstRelease.id,
                            scope.toRollbackNamespace.secondRelease.id)
                            .then(function (result) {
                                scope.toRollbackNamespace.releaseCompareResult = result.changes;

                                AppUtil.showModal('#rollbackModal');
                            })
                    });
            }

            function preRollbackTo(namespace, toReleaseId) {
                scope.isRollbackTo = true;
                scope.toRollbackNamespace = namespace;
                scope.toRollbackNamespace.isPropertiesFormat = namespace.format == 'properties';

                ReleaseService.findLatestActiveRelease(scope.appId,
                    scope.env,
                    namespace.baseInfo.clusterName,
                    namespace.baseInfo.namespaceName)
                    .then(function (result) {
                        if (!result) {
                            toastr.error($translate.instant('Rollback.NoRollbackList'));
                            return;
                        }
                        scope.toRollbackNamespace.firstRelease = result;

                        ReleaseService.get(scope.env,
                            toReleaseId)
                            .then(function (result) {
                                scope.toRollbackNamespace.secondRelease = result;

                                if (scope.toRollbackNamespace.firstRelease.id == scope.toRollbackNamespace.secondRelease.id) {
                                    toastr.error($translate.instant('Rollback.SameAsCurrentRelease'));
                                    return;
                                }

                                ReleaseService.compare(scope.env,
                                    scope.toRollbackNamespace.firstRelease.id,
                                    scope.toRollbackNamespace.secondRelease.id)
                                    .then(function (result) {
                                        scope.toRollbackNamespace.releaseCompareResult = result.changes;

                                        AppUtil.showModal('#rollbackModal');
                                    })

                            })

                    })
            }

            function rollback() {
                scope.toRollbackNamespace.rollbackBtnDisabled = true;
                ReleaseService.rollback(scope.env,
                    scope.toRollbackNamespace.firstRelease.id)
                    .then(function (result) {
                        toastr.success($translate.instant('Rollback.RollbackSuccessfully'));
                        scope.toRollbackNamespace.rollbackBtnDisabled = false;
                        AppUtil.hideModal('#rollbackModal');
                        EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                            {
                                namespace: scope.toRollbackNamespace
                            });
                    }, function (result) {
                        scope.toRollbackNamespace.rollbackBtnDisabled = false;
                        AppUtil.showErrorMsg(result, $translate.instant('Rollback.RollbackFailed'));
                    })
            }

            function rollbackTo(toReleaseId) {
                scope.toRollbackNamespace.rollbackBtnDisabled = true;
                ReleaseService.rollbackTo(scope.env,
                    scope.toRollbackNamespace.firstRelease.id,
                    toReleaseId
                    )
                    .then(function (result) {
                        toastr.success($translate.instant('Rollback.RollbackSuccessfully'));
                        scope.toRollbackNamespace.rollbackBtnDisabled = false;
                        AppUtil.hideModal('#rollbackModal');
                        EventManager.emit(EventManager.EventType.REFRESH_RELEASE_HISTORY, {releaseId: toReleaseId});
                    }, function (result) {
                        scope.toRollbackNamespace.rollbackBtnDisabled = false;
                        AppUtil.showErrorMsg(result, $translate.instant('Rollback.RollbackFailed'));
                    })
            }

            function showRollbackAlertDialog() {
                AppUtil.hideModal("#rollbackModal");
                AppUtil.showModal("#rollbackAlertDialog");
            }
        }
    }
}


