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
/** navbar */
directive_module.directive('apollonav',
    function ($compile, $window, $translate, toastr, AppUtil, AppService, EnvService,
        UserService, CommonService, PermissionService) {
        return {
            restrict: 'E',
            templateUrl: AppUtil.prefixPath() + '/views/common/nav.html',
            transclude: true,
            replace: true,
            link: function (scope, element, attrs) {

                CommonService.getPageSetting().then(function (setting) {
                    scope.pageSetting = setting;
                });

               // Looks like a trick to make xml/yml/json namespaces display right, but why?
               $(document).on('click', function () {
                   scope.$apply(function () {});
               });

               $translate('ApolloConfirmDialog.SearchPlaceHolder').then(function(placeholderLabel)  {
                   $('#app-search-list').select2({
                      placeholder: placeholderLabel,
                      ajax: {
                        url: AppUtil.prefixPath() + "/apps/search/by-appid-or-name",
                        dataType: 'json',
                        delay: 400,
                        data: function (params) {
                            return {
                                query: params.term || '',
                                page: params.page ? params.page - 1 : 0,
                                    size: 20
                                };
                            },
                            processResults: function (data) {
                                if (data && data.content) {
                                    var hasMore = data.content.length
                                        === data.size;
                                    var result = [];
                                    data.content.forEach(function (app) {
                                        result.push({
                                            id: app.appId,
                                            text: app.appId + ' / ' + app.name,
                                            orgId: app.orgId,
                                            orgName: app.orgName
                                        })
                                    });
                                    return {
                                        results: result,
                                        pagination: {
                                            more: hasMore
                                        }
                                    };
                                } else {
                                    return {
                                        results: [],
                                        pagination: {
                                            more: false
                                        }
                                    };
                                }

                            }
                        }
                    });

                    $('#app-search-list').on('select2:select', function () {
                        var selected = $('#app-search-list').select2('data');
                        if (selected && selected.length) {
                            jumpToConfigPage(selected[0].id, selected[0].name, selected[0].orgName, selected[0].orgId)
                        }
                    });
                });

                function jumpToConfigPage(selectedAppId, name, type, namespaceInfo) {
                    if ($window.location.href.indexOf("config.html") > -1) {
                        if (type && type.startsWith('SearchByItem')) {
                            var namespaceInfos = namespaceInfo.split('+');
                            var env = namespaceInfos[0];
                            var cluster = namespaceInfos[1];
                            var namespaceName = namespaceInfos[2];
                            var searchKey = type.split("+")[1];
                            $window.location.hash =
                                "appid=" + selectedAppId + "&env=" + env + "&cluster=" + cluster + "&namespace="
                                + namespaceName + "&item=" + searchKey;
                        } else {
                            $window.location.hash = "appid=" + selectedAppId;
                        }

                        $window.location.reload();
                    } else {
                        if (type && type.startsWith('SearchByItem')) {
                            var namespaceInfos = namespaceInfo.split('+');
                            var env = namespaceInfos[0];
                            var cluster = namespaceInfos[1];
                            var namespaceName = namespaceInfos[2];
                            var searchKey = type.split("+")[1];
                            $window.location.href =
                                AppUtil.prefixPath() + '/config.html?#appid=' + selectedAppId + "&env=" + env
                                + "&cluster=" + cluster + "&namespace=" + namespaceName + "&item=" + searchKey;
                        } else {
                            $window.location.href =  AppUtil.prefixPath() + '/config.html?#appid=' + selectedAppId;
                        }
                    }
                };

                UserService.load_user().then(function (result) {
                    scope.userName = result.userId;
                    scope.userDisplayName = result.name;
                }, function (result) {

                });

                PermissionService.has_root_permission().then(function (result) {
                    scope.hasRootPermission = result.hasPermission;
                })

                scope.changeLanguage = function (lang) {
                    $translate.use(lang)
                }
            }
        }

    });

/** env cluster selector*/
directive_module.directive('apolloclusterselector', function ($compile, $window, AppService, AppUtil, toastr) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/env-selector.html',
        transclude: true,
        replace: true,
        scope: {
            appId: '=apolloAppId',
            defaultAllChecked: '=apolloDefaultAllChecked',
            select: '=apolloSelect',
            defaultCheckedEnv: '=apolloDefaultCheckedEnv',
            defaultCheckedCluster: '=apolloDefaultCheckedCluster',
            notCheckedEnv: '=apolloNotCheckedEnv',
            notCheckedCluster: '=apolloNotCheckedCluster'
        },
        link: function (scope, element, attrs) {

            scope.$watch("defaultCheckedEnv", refreshClusterList);
            scope.$watch("defaultCheckedCluster", refreshClusterList);

            refreshClusterList();

            function refreshClusterList() {
                AppService.load_nav_tree(scope.appId).then(function (result) {
                    scope.clusters = [];
                    var envClusterInfo = AppUtil.collectData(result);
                    envClusterInfo.forEach(function (node) {
                        var env = node.env;
                        node.clusters.forEach(function (cluster) {
                            cluster.env = env;
                            //default checked
                            cluster.checked = scope.defaultAllChecked ||
                                (cluster.env == scope.defaultCheckedEnv && cluster.name
                                    == scope.defaultCheckedCluster);
                            //not checked
                            if (cluster.env == scope.notCheckedEnv && cluster.name == scope.notCheckedCluster) {
                                cluster.checked = false;
                            }

                            scope.clusters.push(cluster);
                        })
                    });
                    scope.select(collectSelectedClusters());
                });
            }

            scope.envAllSelected = scope.defaultAllChecked;

            scope.toggleEnvsCheckedStatus = function () {
                scope.envAllSelected = !scope.envAllSelected;
                scope.clusters.forEach(function (cluster) {
                    cluster.checked = scope.envAllSelected;
                });
                scope.select(collectSelectedClusters());
            };

            scope.switchSelect = function (o, $event) {
                o.checked = !o.checked;
                $event.stopPropagation();
                scope.select(collectSelectedClusters());
            };

            scope.toggleClusterCheckedStatus = function (cluster) {
                cluster.checked = !cluster.checked;
                scope.select(collectSelectedClusters());
            };

            function collectSelectedClusters() {
                var selectedClusters = [];
                scope.clusters.forEach(function (cluster) {
                    if (cluster.checked) {
                        cluster.clusterName = cluster.name;
                        selectedClusters.push(cluster);
                    }
                });
                return selectedClusters;
            }

        }
    }

});

/** 必填项*/
directive_module.directive('apollorequiredfield', function ($compile, $window) {
    return {
        restrict: 'E',
        template: '<strong style="color: red">*</strong>',
        transclude: true,
        replace: true
    }
});

/**  确认框 */
directive_module.directive('apolloconfirmdialog', function ($compile, $window, $sce,$translate,AppUtil) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/confirm-dialog.html',
        transclude: true,
        replace: true,
        scope: {
            dialogId: '=apolloDialogId',
            title: '=apolloTitle',
            detail: '=apolloDetail',
            showCancelBtn: '=apolloShowCancelBtn',
            doConfirm: '=apolloConfirm',
            extraClass: '=apolloExtraClass',
            confirmBtnText: '=?',
            cancel: '='
        },
        link: function (scope, element, attrs) {

            scope.$watch("detail", function () {
                scope.detailAsHtml = $sce.trustAsHtml(scope.detail);
            });

            if (!scope.confirmBtnText) {
                scope.confirmBtnText = $translate.instant('ApolloConfirmDialog.DefaultConfirmBtnName');
            }

            scope.confirm = function () {
                if (scope.doConfirm) {
                    scope.doConfirm();
                }
            };



        }
    }
});

/** entrance */
directive_module.directive('apolloentrance', function ($compile, $window,AppUtil) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/entrance.html',
        transclude: true,
        replace: true,
        scope: {
            imgSrc: '=apolloImgSrc',
            title: '=apolloTitle',
            href: '=apolloHref'
        },
        link: function (scope, element, attrs) {
        }
    }
});

/** entrance */
directive_module.directive('apollouserselector', function ($compile, $window,AppUtil) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/user-selector.html',
        transclude: true,
        replace: true,
        scope: {
            id: '=apolloId',
            disabled: '='
        },
        link: function (scope, element, attrs) {

            scope.$watch("id", initSelect2);

            var select2Options = {
                ajax: {
                    url: AppUtil.prefixPath() + '/users',
                    dataType: 'json',
                    delay: 250,
                    data: function (params) {
                        return {
                            keyword: params.term ? params.term : '',
                            limit: 100
                        }
                    },
                    processResults: function (data, params) {
                        var users = [];
                        data.forEach(function (user) {
                            users.push({
                                id: user.userId,
                                text: user.userId + " | " + user.name + " | " + user.email
                            })
                        });
                        return {
                            results: users
                        }

                    },
                    cache: true,
                    minimumInputLength: 5
                }
            };

            function initSelect2() {
                $('.' + scope.id).select2(select2Options);
            }


        }
    }
});

directive_module.directive('apollomultipleuserselector', function ($compile, $window,AppUtil) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/multiple-user-selector.html',
        transclude: true,
        replace: true,
        scope: {
            id: '=apolloId'
        },
        link: function (scope, element, attrs) {

            scope.$watch("id", initSelect2);

            var searchUsersAjax = {
                ajax: {
                    url: AppUtil.prefixPath() + '/users',
                    dataType: 'json',
                    delay: 250,
                    data: function (params) {
                        return {
                            keyword: params.term ? params.term : '',
                            limit: 100
                        }
                    },
                    processResults: function (data, params) {
                        var users = [];
                        data.forEach(function (user) {
                            users.push({
                                id: user.userId,
                                text: user.userId + " | " + user.name + " | " + user.email
                            })
                        });
                        return {
                            results: users
                        }

                    },
                    cache: true,
                    minimumInputLength: 5
                }
            };

            function initSelect2() {
                $('.' + scope.id).select2(searchUsersAjax);
            }
        }
    }
});


