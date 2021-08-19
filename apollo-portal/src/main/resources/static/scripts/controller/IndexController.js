/*
 * Copyright 2021 Apollo Authors
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
.directive('inputSelect', function() {
    return {
        link: function(scope, element, attr) {
            element.on('click', function(evt) {
                evt.target.parentElement.previousElementSibling.value = evt.target.textContent;
                scope.inValue = evt.target.textContent;
                if(evt.target.parentElement.getElementsByClassName('item-bg').length){
                    angular.element(evt.target.parentElement.getElementsByClassName('item-bg')[0]).removeClass('item-bg');
                }
                angular.element(evt.target).addClass('item-bg');
                angular.element(evt.target.parentElement).addClass('hidden-cls');
                return true;
            });
        }
    };
})
.directive('inputSearch', function() {
    return {
        link: function(scope, element, attr) {
            var isFocus = true;
            var isOver = false;
            element.on('focus', function(){
                angular.element(this.nextElementSibling).removeClass('hidden-cls');
            })
            element.on('keydown', function(evt){
                if(!this.nextElementSibling.children.length){
                    return false;
                }
                if(isFocus){
                    var currentLi = this.parentElement.getElementsByClassName('item-bg')[0];
                    if(38 === evt.keyCode && currentLi && currentLi.previousElementSibling){//向上
                        var currentLi = this.parentElement.getElementsByClassName('item-bg')[0],
                            liHeight = currentLi ? currentLi.clientHeight : '',
                            offTop = liHeight;
                        angular.element(currentLi).removeClass('item-bg');
                        angular.element(currentLi.previousElementSibling).addClass('item-bg');

                        for(var i = 0, len = this.nextElementSibling.children.length; i < len; i++){
                            if(this.nextElementSibling.children[i] === currentLi){
                                break;
                            }
                            offTop = offTop + liHeight;
                        }
                        offTop = Math.max(0, offTop - 2 * liHeight);
                        if(this.nextElementSibling.scrollTop > offTop){
                            this.nextElementSibling.scrollTop = offTop;
                        }
                    }else if(40 === evt.keyCode){//向下
                        var currentLi = this.parentElement.getElementsByClassName('item-bg')[0],
                            liHeight = currentLi ? currentLi.clientHeight : '',
                            offTop = liHeight;
                        if(!currentLi){
                            angular.element(this.nextElementSibling.firstElementChild).addClass('item-bg');
                            return true;
                        }
                        if(currentLi.nextElementSibling){
                            angular.element(currentLi).removeClass('item-bg');
                            angular.element(currentLi.nextElementSibling).addClass('item-bg');
                        }

                        for(var i = 0, len = this.nextElementSibling.children.length; i < len; i++){
                            if(this.nextElementSibling.children[i] === currentLi){
                                break;
                            }
                            offTop = offTop + liHeight;
                        }
                        if(this.nextElementSibling.scrollTop > offTop){
                            return false;
                        }
                        if(this.nextElementSibling.clientHeight < offTop && this.nextElementSibling.scrollTop + this.nextElementSibling.clientHeight - liHeight < offTop){
                            this.nextElementSibling.scrollTop = offTop - this.nextElementSibling.clientHeight + liHeight;
                        }

                    }else if(13 === evt.keyCode && currentLi){
                        var isHidden = angular.element(evt.target.nextElementSibling).hasClass('hidden-cls');
                        if(isHidden){
                            angular.element(evt.target.nextElementSibling).removeClass('hidden-cls');
                        }else{
                            evt.target.value = currentLi.innerText;
                            angular.element(currentLi.parentElement).addClass('hidden-cls');
                            scope.inValue = evt.target.value;
                        }
                    }
                }
            })
            element.on('input',function(evt){
                if(angular.element(this.nextElementSibling).hasClass('hidden-cls')){
                    angular.element(this.nextElementSibling).removeClass('hidden-cls')
                }
                if(this.nextElementSibling.children.length){
                    angular.element(this.nextElementSibling.getElementsByClassName('item-bg')[0]).removeClass('item-bg');
                    angular.element(this.nextElementSibling.children[0]).addClass('item-bg');
                }
                scope.$emit('selectInput', {
                    inputId: evt.target.id,
                    inputText: evt.target.value
                });
            });
            angular.element(element[0].nextElementSibling).on('mousemove', function(){
                isOver = true;
            })
            angular.element(element[0].nextElementSibling).on('mouseleave', function(){
                isOver = false;
            })
            element.on('blur',function(evt){
                if(!isOver){
                    angular.element(this.nextElementSibling).addClass('hidden-cls');
                }
            });
        }
    };
});

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

    $scope.inValue = '';
    $scope.inValue_display = '';
    $scope.initList = [];
    $scope.dataList = [];

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
        $scope.hasMoreCreatedApps = true;
        $scope.favoritesPage = 0;
        $scope.favorites = [];
        $scope.hasMoreFavorites = true;
        $scope.publicNamespacePage = 0;
        $scope.publicNamespaces = [];
        $scope.hasMorePublicNamespaces = true;
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
        var size = 10;
        NamespaceService.find_public_namespaces()
            .then(function (result) {
                $scope.allPublicNamespaces = result;
                $scope.dataList = $scope.allPublicNamespaces;
                $scope.initList = $scope.allPublicNamespaces;
                console.log("dataList",$scope.dataList);
                morePublicNamespace();
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
        }
        $scope.publicNamespacePage += 1;
    }

    function changeContent(contentIndex) {
        $scope.whichContent = contentIndex;
    }

    $scope.$on('selectInput', function (evt, inputObj) {
        if (inputObj.inputId === 'select-input-mark') {
            $scope.inValue = '';
            $scope.inValue_display = inputObj.inputText;
            $scope.fuzzyQuery();
        }
    });
    $scope.fuzzyQuery = function () {
        $scope.dataList = [];
        angular.forEach($scope.initList, function (item) {
            if (item.name.indexOf($scope.inValue_display) !== -1) {
                $scope.dataList.push(item);
            }
        });
        $scope.$apply();
        console.log($scope.dataList);
    };

}
