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
application_module.controller("ConfigBaseInfoController",
    ['$rootScope', '$scope', '$window', '$location', '$translate', 'toastr', 'EventManager', 'UserService',
        'AppService',
        'FavoriteService',
        'PermissionService',
        'AppUtil', ConfigBaseInfoController]);

function ConfigBaseInfoController($rootScope, $scope, $window, $location, $translate, toastr, EventManager, UserService, AppService,
    FavoriteService,
    PermissionService,
    AppUtil) {

    var urlParams = AppUtil.parseParams($location.$$url);
    var appId = urlParams.appid;

    if (!appId) {
        $window.location.href = AppUtil.prefixPath() + '/index.html';
        return;
    }

    initPage();

    function initPage() {
        $rootScope.hideTip = JSON.parse(localStorage.getItem("hideTip"));

        //load session storage to recovery scene
        var scene = JSON.parse(sessionStorage.getItem(appId));

        $rootScope.pageContext = {
            appId: appId,
            env: urlParams.env ? urlParams.env : (scene ? scene.env : ''),
            clusterName: urlParams.cluster ? urlParams.cluster : (scene ? scene.cluster : 'default'),
            namespaceName: urlParams.namespace,
            item: urlParams.item,
        };

        //storage page context to session storage
        sessionStorage.setItem(
            $rootScope.pageContext.appId,
            JSON.stringify({
                env: $rootScope.pageContext.env,
                cluster: $rootScope.pageContext.clusterName
            }));

        UserService.load_user().then(function (result) {
            $rootScope.pageContext.userId = result.userId;
            loadAppInfo();
            handleFavorite();
        }, function (result) {
            toastr.error(AppUtil.errorMsg(result),  $translate.instant('Config.GetUserInfoFailed'));
        });

        handlePermission();
    }

    function loadAppInfo() {

        $scope.notFoundApp = true;
        AppService.load($rootScope.pageContext.appId).then(function (result) {
            $scope.notFoundApp = false;

            $scope.appBaseInfo = result;
            $scope.appBaseInfo.orgInfo = result.orgName + '(' + result.orgId + ')';
            $scope.appBaseInfo.ownerInfo = result.ownerDisplayName + '(' + result.ownerName + ')';

            loadNavTree();
            recordVisitApp();
            findMissEnvs();

            $(".J_appFound").removeClass("hidden");
        }, function (result) {
            $(".J_appNotFound").removeClass("hidden");
        });
    }

    $scope.createAppInMissEnv = function () {
        var count = 0;
        $scope.missEnvs.forEach(function (env) {
            AppService.create_remote(env, $scope.appBaseInfo).then(function (result) {
                toastr.success(env, $translate.instant('Common.Created'));
                count++;
                if (count == $scope.missEnvs.length) {
                    location.reload(true);
                }
            }, function (result) {
                toastr.error(AppUtil.errorMsg(result), `${$translate.instant('Common.CreateFailed')}:${env}`);
                count++;
                if (count == $scope.missEnvs.length) {
                    location.reload(true);
                }
            });
        });
    };

    $scope.createMissingNamespaces = function () {
        AppService.create_missing_namespaces($rootScope.pageContext.appId, $rootScope.pageContext.env,
            $rootScope.pageContext.clusterName).then(function (result) {
                toastr.success($translate.instant('Common.Created'));
                location.reload(true);
            }, function (result) {
                toastr.error(AppUtil.errorMsg(result), $translate.instant('Common.CreateFailed'));
            }
            );
    };

    function findMissEnvs() {
        $scope.missEnvs = [];
        AppService.find_miss_envs($rootScope.pageContext.appId).then(function (result) {
            $scope.missEnvs = AppUtil.collectData(result);

            if ($scope.missEnvs.length > 0) {
                toastr.warning($translate.instant('Config.ProjectMissEnvInfos'));
            }

            $scope.findMissingNamespaces();
        });
    }

    EventManager.subscribe(EventManager.EventType.CHANGE_ENV_CLUSTER, function () {
        $scope.findMissingNamespaces();
    });

    $scope.findMissingNamespaces = function () {
        $scope.missingNamespaces = [];
        // only check missing private namespaces when app exists in current env
        if ($rootScope.pageContext.env && $scope.missEnvs.indexOf($rootScope.pageContext.env) === -1) {
            AppService.find_missing_namespaces($rootScope.pageContext.appId, $rootScope.pageContext.env,
                $rootScope.pageContext.clusterName).then(function (result) {
                    $scope.missingNamespaces = AppUtil.collectData(result);
                    if ($scope.missingNamespaces.length > 0) {
                        toastr.warning($translate.instant('Config.ProjectMissNamespaceInfos'));
                    }
                });
        }
    };

    function recordVisitApp() {
        //save user recent visited apps
        var VISITED_APPS_STORAGE_KEY = "VisitedAppsV2";
        var visitedAppsObject = JSON.parse(localStorage.getItem(VISITED_APPS_STORAGE_KEY));
        var hasSaved = false;

        if (!visitedAppsObject) {
            visitedAppsObject = {};
        }

        if (!visitedAppsObject[$rootScope.pageContext.userId]) {
            visitedAppsObject[$rootScope.pageContext.userId] = [];
        }

        var visitedApps = visitedAppsObject[$rootScope.pageContext.userId];
        if (visitedApps && visitedApps.length > 0) {
            visitedApps.forEach(function (app) {
                if (app == appId) {
                    hasSaved = true;
                    return;
                }
            });
        }

        var currentUserVisitedApps = visitedAppsObject[$rootScope.pageContext.userId];
        if (!hasSaved) {
            //if queue's length bigger than 6 will remove oldest app
            if (currentUserVisitedApps.length >= 6) {
                currentUserVisitedApps.splice(0, 1);
            }
            currentUserVisitedApps.push($rootScope.pageContext.appId);

            localStorage.setItem(VISITED_APPS_STORAGE_KEY,
                JSON.stringify(visitedAppsObject));
        }

    }

    function loadNavTree() {

        AppService.load_nav_tree($rootScope.pageContext.appId).then(function (result) {
            var navTree = [];
            var nodes = AppUtil.collectData(result);

            if (!nodes || nodes.length == 0) {
                toastr.error($translate.instant('Config.SystemError'));
                return;
            }
            //default first env if session storage is empty
            if (!$rootScope.pageContext.env) {
                $rootScope.pageContext.env = nodes[0].env;
            }

            EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE, {firstLoad: true});

            nodes.forEach(function (env) {
                if (!env.clusters || env.clusters.length == 0) {
                    return;
                }
                var node = {};
                node.text = env.env;

                var clusterNodes = [];

                //如果env下面只有一个default集群则不显示集群列表
                if (env.clusters && env.clusters.length == 1 && env.clusters[0].name
                    == 'default') {
                    if ($rootScope.pageContext.env == env.env) {
                        node.state = {};
                        node.state.selected = true;
                    }
                    node.selectable = true;

                } else {
                    node.selectable = false;
                    //cluster list
                    env.clusters.forEach(function (cluster) {
                        var clusterNode = {},
                            parentNode = [];

                        //default selection from session storage or first env & first cluster
                        if ($rootScope.pageContext.env == env.env && $rootScope.pageContext.clusterName
                            == cluster.name) {
                            clusterNode.state = {};
                            clusterNode.state.selected = true;
                        }

                        clusterNode.text = cluster.name;
                        parentNode.push(node.text);
                        clusterNode.tags = [$translate.instant('Common.Cluster')];
                        clusterNode.parentNode = parentNode;
                        clusterNode.comment = cluster.comment;
                        clusterNodes.push(clusterNode);

                    });
                }
                node.nodes = clusterNodes;
                navTree.push(node);
            });

            //init treeview
            $('#treeview').treeview({
                color: "#797979",
                showBorder: true,
                data: navTree,
                levels: 99,
                expandIcon: '',
                collapseIcon: '',
                showTags: true,
                onNodeSelected: function (event, data) {
                    if (!data.parentNode) {//first nav node
                        $rootScope.pageContext.env = data.text;
                        $rootScope.pageContext.clusterName =
                            'default';
                    } else {//second cluster node
                        $rootScope.pageContext.env =
                            data.parentNode[0];
                        $rootScope.pageContext.clusterName =
                            data.text;
                    }
                    //storage scene
                    sessionStorage.setItem(
                        $rootScope.pageContext.appId,
                        JSON.stringify({
                            env: $rootScope.pageContext.env,
                            cluster: $rootScope.pageContext.clusterName
                        }));

                    $window.location.href = AppUtil.prefixPath() + "/config.html#/appid="
                        + $rootScope.pageContext.appId
                        + "&env=" + $rootScope.pageContext.env
                        + "&cluster=" + $rootScope.pageContext.clusterName;

                    EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE);
                    EventManager.emit(EventManager.EventType.CHANGE_ENV_CLUSTER);
                    $rootScope.showSideBar = false;
                }
            });
            $('#treeview .node-treeview').hover(
                function () {
                    // get comment by nodeid
                    var commentValue = $('#treeview').treeview('getNode', $(this).data('nodeid')).comment;
                    if (typeof commentValue !== 'undefined') {
                        $(this).attr('title', commentValue);
                    }
                },
                function () {
                }
            );

            var envMapClusters = {};
            navTree.forEach(function (node) {
                if (node.nodes && node.nodes.length > 0) {

                    var clusterNames = [];
                    node.nodes.forEach(function (cluster) {
                        if (cluster.text != 'default') {
                            clusterNames.push(cluster.text);
                        }

                    });

                    envMapClusters[node.text] = clusterNames.join(",");

                }
            });

            $rootScope.envMapClusters = envMapClusters;

        }, function (result) {
            toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.SystemError'));
        });

    }

    function handleFavorite() {

        FavoriteService.findFavorites($rootScope.pageContext.userId,
            $rootScope.pageContext.appId)
            .then(function (result) {
                if (result && result.length) {
                    $scope.favoriteId = result[0].id;
                }

            });

        $scope.addFavorite = function () {
            var favorite = {
                userId: $rootScope.pageContext.userId,
                appId: $rootScope.pageContext.appId
            };

            FavoriteService.addFavorite(favorite)
                .then(function (result) {
                    $scope.favoriteId = result.id;
                    toastr.success($translate.instant('Config.FavoriteSuccessfully'));
                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.FavoriteFailed'));
                })
        };

        $scope.deleteFavorite = function () {
            FavoriteService.deleteFavorite($scope.favoriteId)
                .then(function (result) {
                    $scope.favoriteId = 0;
                    toastr.success($translate.instant('Config.CancelledFavorite'));
                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.CancelFavoriteFailed'));
                })
        };
    }

    function handlePermission() {
        //permission
        PermissionService.has_create_namespace_permission(appId).then(function (result) {
            $scope.hasCreateNamespacePermission = result.hasPermission;
        }, function (result) {

        });

        PermissionService.has_create_cluster_permission(appId).then(function (result) {
            $scope.hasCreateClusterPermission = result.hasPermission;
        }, function (result) {

        });


        PermissionService.has_assign_user_permission(appId).then(function (result) {
            $scope.hasAssignUserPermission = result.hasPermission;
        }, function (result) {

        });

        $scope.showMasterPermissionTips = function () {
            $("#masterNoPermissionDialog").modal('show');
        };
    }

    var VIEW_MODE_SWITCH_WIDTH = 1156;
    if (window.innerWidth <= VIEW_MODE_SWITCH_WIDTH) {
        $rootScope.viewMode = 2;
        $rootScope.showSideBar = false;
    } else {
        $rootScope.viewMode = 1;
    }

    $rootScope.adaptScreenSize = function () {
        if (window.innerWidth <= VIEW_MODE_SWITCH_WIDTH) {
            $rootScope.viewMode = 2;
        } else {
            $rootScope.viewMode = 1;
            $rootScope.showSideBar = false;
        }

    };

    $(window).resize(function () {
        $scope.$apply(function () {
            $rootScope.adaptScreenSize();
        });
    });

}

