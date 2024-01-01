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
index_module.controller('IndexController', ['$scope', '$window', '$translate', 'toastr', 'AppUtil', 'AppService',
    'UserService', 'FavoriteService', 'NamespaceService',
    IndexController]
)


function IndexController($scope, $window, $translate, toastr, AppUtil, AppService, UserService, FavoriteService, NamespaceService) {

    $scope.userId = '';
    $scope.whichContent = '1';

    $scope.getUserCreatedApps = getUserCreatedApps;
    $scope.getUserFavorites = getUserFavorites;
    $scope.getPublicNamespaces = getPublicNamespaces;
    $scope.goToAppHomePage = goToAppHomePage;
    $scope.goToCreateAppPage = goToCreateAppPage;
    $scope.toggleOperationBtn = toggleOperationBtn;
    $scope.toTop = toTop;
    $scope.deleteFavorite = deleteFavorite;
    $scope.morePublicNamespace = morePublicNamespace;
    $scope.changeContent = changeContent;

    function initCreateApplicationPermission() {
        AppService.has_create_application_role($scope.userId).then(
            function (value) {
                $scope.hasCreateApplicationPermission = value.hasCreateApplicationPermission;
            },
            function (reason) {
                toastr.warning(AppUtil.errorMsg(reason), $translate.instant('Index.GetCreateAppRoleFailed'));
            }
        )
    }

    UserService.load_user().then(function (result) {
        $scope.userId = result.userId;

        $scope.createdAppPage = 0;
        $scope.createdApps = [];
        $scope.hasMoreCreatedApps = false;
        $scope.favoritesPage = 0;
        $scope.favorites = [];
        $scope.hasMoreFavorites = false;
        $scope.publicNamespacePage = 0;
        $scope.publicNamespaces = [];
        $scope.hasMorePublicNamespaces = false;
        $scope.allPublicNamespaces = [];
        $scope.visitedApps = [];

        initCreateApplicationPermission();

        getUserCreatedApps();

        getUserFavorites();

        getPublicNamespaces();

        initUserVisitedApps();

    });

    function getUserCreatedApps() {
        var size = 10;
        AppService.find_app_by_owner($scope.userId, $scope.createdAppPage, size)
            .then(function (result) {
                $scope.createdAppPage += 1;
                $scope.hasMoreCreatedApps = result.length == size;

                if (!result || result.length == 0) {
                    return;
                }
                result.forEach(function (app) {
                    $scope.createdApps.push(app);
                });

            })
    }

    function getUserFavorites() {
        var size = 11;
        FavoriteService.findFavorites($scope.userId, '', $scope.favoritesPage, size)
            .then(function (result) {
                $scope.favoritesPage += 1;
                $scope.hasMoreFavorites = result.length == size;

                if ($scope.favoritesPage == 1) {
                    $("#app-list").removeClass("hidden");
                }

                if (!result || result.length == 0) {
                    return;
                }
                var appIds = [];
                result.forEach(function (favorite) {
                    appIds.push(favorite.appId);
                });


                AppService.find_apps(appIds.join(","))
                    .then(function (apps) {
                        //sort
                        var appIdMapApp = {};
                        apps.forEach(function (app) {
                            appIdMapApp[app.appId] = app;
                        });
                        result.forEach(function (favorite) {
                            var app = appIdMapApp[favorite.appId];
                            if (!app) {
                                return;
                            }
                            app.favoriteId = favorite.id;
                            $scope.favorites.push(app);
                        });
                    });
            })
    }

    function getPublicNamespaces() {
        NamespaceService.find_public_namespaces()
            .then(function (result) {
                $scope.allPublicNamespaces = result;
                morePublicNamespace();
                var selectResult = [];
                angular.forEach(result,function (app) {
                    selectResult.push({
                        id: app.appId,
                        text: app.appId + ' / ' + app.name
                    })
                });
                $('#public-name-spaces-search-list').select2({
                    data: selectResult,
                });
                $('#public-name-spaces-search-list').on('select2:select', function () {
                    var selected = $('#public-name-spaces-search-list').select2('data');
                    if (selected && selected.length) {
                        goToAppHomePage(selected[0].id)
                    }
                });
            })
    }

    function initUserVisitedApps() {
        var VISITED_APPS_STORAGE_KEY = "VisitedAppsV2";
        var visitedAppsObject = JSON.parse(localStorage.getItem(VISITED_APPS_STORAGE_KEY));
        if (!visitedAppsObject) {
            visitedAppsObject = {};
        }

        var userVisitedApps = visitedAppsObject[$scope.userId];
        if (userVisitedApps && userVisitedApps.length > 0) {
            AppService.find_apps(userVisitedApps.join(","))
                .then(function (apps) {
                    //sort
                    var appIdMapApp = {};
                    apps.forEach(function (app) {
                        appIdMapApp[app.appId] = app;
                    });

                    userVisitedApps.forEach(function (appId) {
                        var app = appIdMapApp[appId];
                        if (app) {
                            $scope.visitedApps.push(app);
                        }
                    });
                });
        }

    }

    function goToCreateAppPage() {
        $window.location.href = AppUtil.prefixPath() + "/app.html";
    }

    function goToAppHomePage(appId) {
        $window.location.href = AppUtil.prefixPath() + "/config.html?#/appid=" + appId;
    }

    function toggleOperationBtn(app) {
        app.showOperationBtn = !app.showOperationBtn;
    }

    function toTop(favoriteId) {
        FavoriteService.toTop(favoriteId).then(function () {
            toastr.success($translate.instant('Index.Topped'));
            refreshFavorites();

        })
    }

    function deleteFavorite(favoriteId) {
        FavoriteService.deleteFavorite(favoriteId).then(function () {
            toastr.success($translate.instant('Index.CancelledFavorite'));
            refreshFavorites();
        })
    }

    function refreshFavorites() {
        $scope.favoritesPage = 0;
        $scope.favorites = [];
        $scope.hasMoreFavorites = true;

        getUserFavorites();
    }

    function morePublicNamespace() {
        var rest = $scope.allPublicNamespaces.length - $scope.publicNamespacePage * 10;
        if (rest <= 10) {
            for (var i = 0; i < rest; i++) {
                $scope.publicNamespaces.push($scope.allPublicNamespaces[$scope.publicNamespacePage * 10 + i])
            }
            $scope.hasMorePublicNamespaces = false;
        } else {
            for (var j = 0; j < 10; j++) {
                $scope.publicNamespaces.push($scope.allPublicNamespaces[$scope.publicNamespacePage * 10 + j])
            }
            $scope.hasMorePublicNamespaces = true;
        }
        $scope.publicNamespacePage += 1;
    }

    function changeContent(contentIndex) {
        $scope.whichContent = contentIndex;
    }

}
