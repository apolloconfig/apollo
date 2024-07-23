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
global_search_value_module.controller('GlobalSearchValueController',
    ['$scope', '$window', '$translate', 'toastr', 'AppUtil', 'GlobalSearchValueService', 'PermissionService', GlobalSearchValueController]);

function GlobalSearchValueController($scope, $window, $translate, toastr, AppUtil, GlobalSearchValueService, PermissionService) {

    $scope.itemInfo = [];
    $scope.itemInfoSearchKey = '';
    $scope.itemInfoSearchValue = '';
    $scope.needToBeHighlightedKey = '';
    $scope.needToBeHighlightedValue = '';
    $scope.isShowHighlightKeyword = [];
    $scope.isDirectlyDisplayKey = [];
    $scope.isDirectlyDisplayValue = [];
    $scope.getItemInfoByKeyAndValue = getItemInfoByKeyAndValue;
    $scope.highlightKeyword = highlightKeyword;
    $scope.jumpToTheEditingPage = jumpToTheEditingPage;
    $scope.showAllValue = showAllValue;
    $scope.closeAllValue = closeAllValue;




    init();
    function init() {
        initPermission();
    }

    function initPermission() {
        PermissionService.has_root_permission()
            .then(function (result) {
                $scope.isRootUser = result.hasPermission;
            });
    }

    function getItemInfoByKeyAndValue(itemInfoSearchKey, itemInfoSearchValue) {
        console.log('Function getIemInfoByKeyAndValue is called', arguments);
        $scope.itemInfoSearchKey = itemInfoSearchKey;
        $scope.itemInfoSearchValue = itemInfoSearchValue;
        console.log($scope.itemInfoSearchKey,"+",$scope.itemInfoSearchValue);
        if(($scope.itemInfoSearchKey == '' || $scope.itemInfoSearchKey === undefined) && ($scope.itemInfoSearchValue == '' || $scope.itemInfoSearchValue === undefined)){
            $scope.needToBeHighlightedValue = '';
            $scope.needToBeHighlightedKey = '';
            GlobalSearchValueService.findItemInfoByKeyAndValue($scope.itemInfoSearchKey,$scope.itemInfoSearchValue)
                .then(function (result) {
                    $scope.itemInfo = [];
                    $scope.isDirectlyDisplayValue = [];
                    $scope.isDirectlyDisplayKey = [];
                    var numCount = 0;
                    result.forEach(function (iteminfo) {
                        $scope.itemInfo.push(iteminfo);
                        $scope.isDirectlyDisplayValue[numCount] = "0";
                        $scope.isDirectlyDisplayKey[numCount] = "0";
                        numCount++;
                    });
                    console.log($scope.itemInfo);
                },function (result) {
                    $scope.itemInfo = [];
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('Item.GlobalSearchSystemError'));
                });
        }else if(($scope.itemInfoSearchKey == '' || $scope.itemInfoSearchKey === undefined) && !($scope.itemInfoSearchValue == '' || $scope.itemInfoSearchValue === undefined)){
            $scope.needToBeHighlightedValue = $scope.itemInfoSearchValue;
            $scope.needToBeHighlightedKey = '';
            GlobalSearchValueService.findItemInfoByKeyAndValue($scope.itemInfoSearchKey,$scope.itemInfoSearchValue)
                .then(function (result) {
                    $scope.itemInfo = [];
                    var numCount = 0;
                    $scope.isDirectlyDisplayValue = [];
                    $scope.isDirectlyDisplayKey = [];
                    result.forEach(function (iteminfo) {
                        $scope.itemInfo.push(iteminfo);
                        if(iteminfo.value == $scope.needToBeHighlightedValue){
                            $scope.isDirectlyDisplayValue[numCount] = "0";
                            $scope.isDirectlyDisplayKey[numCount] = "0";
                            numCount++;
                        }else{
                            var position = iteminfo.value.indexOf($scope.needToBeHighlightedValue);
                            if (position !== -1) {
                                console.log(position);
                                if (position === 0) {
                                    $scope.isDirectlyDisplayValue[numCount] = "1";
                                    $scope.isDirectlyDisplayKey[numCount] = "0";
                                } else if (position + $scope.needToBeHighlightedValue.length === iteminfo.value.length) {
                                    $scope.isDirectlyDisplayValue[numCount] = "2";
                                    $scope.isDirectlyDisplayKey[numCount] = "0";
                                } else {
                                    $scope.isDirectlyDisplayValue[numCount] = "3";
                                    $scope.isDirectlyDisplayKey[numCount] = "0";
                                }
                                numCount++;
                            } else {
                                $scope.isDirectlyDisplayValue[numCount] = "-1";
                                numCount++;
                            }
                        }
                    });
                    console.log($scope.itemInfo);
                },function (result) {
                    $scope.itemInfo = [];
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('Item.GlobalSearchSystemError'));
                });
        }else if(!($scope.itemInfoSearchKey == '' || $scope.itemInfoSearchKey === undefined) && ($scope.itemInfoSearchValue == '' || $scope.itemInfoSearchValue === undefined)){
            $scope.needToBeHighlightedKey = $scope.itemInfoSearchKey;
            $scope.needToBeHighlightedValue = '';
            GlobalSearchValueService.findItemInfoByKeyAndValue($scope.itemInfoSearchKey,$scope.itemInfoSearchValue)
                .then(function (result) {
                    $scope.itemInfo = [];
                    var numCount = 0;
                    $scope.isDirectlyDisplayValue = [];
                    $scope.isDirectlyDisplayKey = [];
                    result.forEach(function (iteminfo) {
                        $scope.itemInfo.push(iteminfo);
                        if(iteminfo.key == $scope.needToBeHighlightedKey){
                            $scope.isDirectlyDisplayValue[numCount] = "0";
                            $scope.isDirectlyDisplayKey[numCount] = "0";
                            numCount++;
                        }else{
                            $scope.isDirectlyDisplayValue[numCount] = "0";
                            $scope.isDirectlyDisplayKey[numCount] = "-1";
                            numCount++;
                        }
                    });
                    console.log($scope.itemInfo);
                },function (result) {
                    $scope.itemInfo = [];
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('Item.GlobalSearchSystemError'));
                });
        }else {
            $scope.needToBeHighlightedKey = $scope.itemInfoSearchKey;
            $scope.needToBeHighlightedValue = $scope.itemInfoSearchValue;
            GlobalSearchValueService.findItemInfoByKeyAndValue($scope.itemInfoSearchKey,$scope.itemInfoSearchValue)
                .then(function (result) {
                    $scope.itemInfo = [];
                    var numCount = 0;
                    $scope.isDirectlyDisplayValue = [];
                    $scope.isDirectlyDisplayKey = [];
                    result.forEach(function (iteminfo) {
                        $scope.itemInfo.push(iteminfo);
                        if(iteminfo.key == $scope.needToBeHighlightedKey){
                            $scope.isDirectlyDisplayKey[numCount] = "0";
                            if(iteminfo.value == $scope.needToBeHighlightedValue){
                                $scope.isDirectlyDisplayValue[numCount] = "0";
                                numCount++;
                            }else{
                                var position = iteminfo.value.indexOf($scope.needToBeHighlightedValue);
                                if (position !== -1) {
                                    if (position === 0) {
                                        $scope.isDirectlyDisplayValue[numCount] = "1";
                                    } else if (position + $scope.needToBeHighlightedValue.length === iteminfo.value.length) {
                                        $scope.isDirectlyDisplayValue[numCount] = "2";
                                    } else {
                                        $scope.isDirectlyDisplayValue[numCount] = "3";
                                    }
                                    numCount++;
                                } else {
                                    $scope.isDirectlyDisplayValue[numCount] = "-1";
                                    numCount++;
                                }
                            }
                        }else{
                            $scope.isDirectlyDisplayKey[numCount] = "-1";
                            if(iteminfo.value == $scope.needToBeHighlightedValue){
                                $scope.isDirectlyDisplayValue[numCount] = "0";
                                numCount++;
                            }else{
                                var position = iteminfo.value.indexOf($scope.needToBeHighlightedValue);
                                if (position !== -1) {
                                    if (position === 0) {
                                        $scope.isDirectlyDisplayValue[numCount] = "1";
                                    } else if (position + $scope.needToBeHighlightedValue.length === iteminfo.value.length) {
                                        $scope.isDirectlyDisplayValue[numCount] = "2";
                                    } else {
                                        $scope.isDirectlyDisplayValue[numCount] = "3";
                                    }
                                    numCount++;
                                } else {
                                    $scope.isDirectlyDisplayValue[numCount] = "-1";
                                    numCount++;
                                }
                            }
                        }
                    });
                    console.log($scope.itemInfo);
                },function (result) {
                    $scope.itemInfo = [];
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('Item.GlobalSearchSystemError'));
                });
        }

    }

    function jumpToTheEditingPage(appid,env,cluster){
        $window.location.href = AppUtil.prefixPath() + "/config.html#/appid=" + appid + "&" +"env=" + env + "&" + "cluster=" + cluster;
    }

    function highlightKeyword(fulltext,keyword) {
        if (!keyword || keyword.length === 0) return fulltext;
        var regex = new RegExp("(" + keyword + ")", "g");
        return fulltext.replace(regex, '<span class="highlight" style="background: yellow;padding: 1px 4px;">$1</span>');
    }

    function closeAllValue(index){
        $scope.isShowHighlightKeyword[index] = !$scope.isShowHighlightKeyword[index];
    }

    function showAllValue(index){
        $scope.isShowHighlightKeyword[index] = !$scope.isShowHighlightKeyword[index];
    }

}
