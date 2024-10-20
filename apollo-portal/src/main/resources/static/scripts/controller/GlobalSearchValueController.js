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

    $scope.allItemInfo = [];
    $scope.pageItemInfo = [];
    $scope.itemInfoSearchKey = '';
    $scope.itemInfoSearchValue = '';
    $scope.needToBeHighlightedKey = '';
    $scope.needToBeHighlightedValue = '';
    $scope.isShowHighlightKeyword = [];
    $scope.isAllItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword = [];
    $scope.isAllItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword = [];
    $scope.isAllItemInfoDisplayValueInARow = [];
    $scope.isPageItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword = [];
    $scope.isPageItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword = [];
    $scope.isPageItemInfoDisplayValueInARow = [];
    $scope.currentPage = 1;
    $scope.pageSize = '10';
    $scope.totalItems = 0;
    $scope.totalPages = 0;
    $scope.pagesArray = [];
    $scope.tempKey = '';
    $scope.tempValue = '';

    $scope.getItemInfoByKeyAndValue = getItemInfoByKeyAndValue;
    $scope.highlightKeyword = highlightKeyword;
    $scope.jumpToTheEditingPage = jumpToTheEditingPage;
    $scope.isShowAllValue = isShowAllValue;
    $scope.convertPageSizeToInt = convertPageSizeToInt;
    $scope.changePage = changePage;
    $scope.getPagesArray = getPagesArray;
    $scope.determineDisplayKeyOrValueWithoutShowHighlightKeyword = determineDisplayKeyOrValueWithoutShowHighlightKeyword;
    $scope.determineDisplayValueInARow = determineDisplayValueInARow;

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
        $scope.currentPage = 1;
        $scope.itemInfoSearchKey = itemInfoSearchKey || '';
        $scope.itemInfoSearchValue = itemInfoSearchValue || '';
        $scope.allItemInfo = [];
        $scope.pageItemInfo = [];
        $scope.isAllItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword = [];
        $scope.isAllItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword = [];
        $scope.isAllItemInfoDisplayValueInARow = [];
        $scope.isPageItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword = [];
        $scope.isPageItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword = [];
        $scope.isPageItemInfoDisplayValueInARow = [];
        $scope.tempKey = itemInfoSearchKey || '';
        $scope.tempValue = itemInfoSearchValue || '';
        $scope.isShowHighlightKeyword = [];
        GlobalSearchValueService.findItemInfoByKeyAndValue($scope.itemInfoSearchKey, $scope.itemInfoSearchValue)
            .then(handleSuccess).catch(handleError);
        function handleSuccess(result) {
            let allItemInfo = [];
            let isAllItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword = [];
            let isAllItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword = [];
            let isAllItemInfoDisplayValueInARow = [];
            if(($scope.itemInfoSearchKey === '') && !($scope.itemInfoSearchValue === '')){
                $scope.needToBeHighlightedValue = $scope.itemInfoSearchValue;
                $scope.needToBeHighlightedKey = '';
                result.body.forEach((itemInfo, index) => {
                    allItemInfo.push(itemInfo);
                    isAllItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword[index] = '0';
                    isAllItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword[index] = determineDisplayKeyOrValueWithoutShowHighlightKeyword(itemInfo.value, itemInfoSearchValue);
                    isAllItemInfoDisplayValueInARow[index] = determineDisplayValueInARow(itemInfo.value, itemInfoSearchValue);
                });
            }else if(!($scope.itemInfoSearchKey === '') && ($scope.itemInfoSearchValue === '')){
                $scope.needToBeHighlightedKey = $scope.itemInfoSearchKey;
                $scope.needToBeHighlightedValue = '';
                result.body.forEach((itemInfo, index) => {
                    allItemInfo.push(itemInfo);
                    isAllItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword[index] = determineDisplayKeyOrValueWithoutShowHighlightKeyword(itemInfo.key, itemInfoSearchKey);
                    isAllItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword[index] = '0';
                });
            }else{
                $scope.needToBeHighlightedKey = $scope.itemInfoSearchKey;
                $scope.needToBeHighlightedValue = $scope.itemInfoSearchValue;
                result.body.forEach((itemInfo, index) => {
                    allItemInfo.push(itemInfo);
                    isAllItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword[index] = determineDisplayKeyOrValueWithoutShowHighlightKeyword(itemInfo.value, itemInfoSearchValue);
                    isAllItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword[index] = determineDisplayKeyOrValueWithoutShowHighlightKeyword(itemInfo.key, itemInfoSearchKey);
                    isAllItemInfoDisplayValueInARow[index] = determineDisplayValueInARow(itemInfo.value, itemInfoSearchValue);
                });
            }
            $scope.totalItems = allItemInfo.length;
            $scope.allItemInfo = allItemInfo;
            $scope.totalPages = Math.ceil($scope.totalItems / parseInt($scope.pageSize, 10));
            const startIndex = ($scope.currentPage - 1) * parseInt($scope.pageSize, 10);
            const endIndex = Math.min(startIndex + parseInt($scope.pageSize, 10), allItemInfo.length);
            $scope.pageItemInfo = allItemInfo.slice(startIndex, endIndex);
            $scope.isAllItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword = isAllItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword;
            $scope.isAllItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword = isAllItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword;
            $scope.isAllItemInfoDisplayValueInARow = isAllItemInfoDisplayValueInARow;
            $scope.isPageItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword = isAllItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword.slice(startIndex, endIndex);
            $scope.isPageItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword = isAllItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword.slice(startIndex, endIndex);
            $scope.isPageItemInfoDisplayValueInARow = isAllItemInfoDisplayValueInARow.slice(startIndex, endIndex);
            getPagesArray();
            if(result.hasMoreData){
                toastr.warning(result.message, $translate.instant('Item.GlobalSearch.Tips'));
            }
        }

        function handleError(error) {
            $scope.itemInfo = [];
            toastr.error(AppUtil.errorMsg(error), $translate.instant('Item.GlobalSearchSystemError'));
        }
    }

    function convertPageSizeToInt() {
        getItemInfoByKeyAndValue($scope.tempKey, $scope.tempValue);
    }

    function changePage(page) {
        if (page >= 1 && page <= $scope.totalPages) {
            $scope.currentPage = page;
            $scope.isShowHighlightKeyword = [];
            $scope.isPageItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword = [];
            $scope.isPageItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword = [];
            $scope.isPageItemInfoDisplayValueInARow = [];
            $scope.itemInfoSearchKey = $scope.tempKey;
            $scope.itemInfoSearchValue = $scope.tempValue;
            const startIndex = ($scope.currentPage - 1)* parseInt($scope.pageSize, 10);
            const endIndex = Math.min(startIndex + parseInt($scope.pageSize, 10), $scope.totalItems);
            $scope.pageItemInfo = $scope.allItemInfo.slice(startIndex, endIndex);
            $scope.isPageItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword = $scope.isAllItemInfoDirectlyDisplayValueWithoutShowHighlightKeyword.slice(startIndex, endIndex);
            $scope.isPageItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword = $scope.isAllItemInfoDirectlyDisplayKeyWithoutShowHighlightKeyword.slice(startIndex, endIndex);
            $scope.isPageItemInfoDisplayValueInARow = $scope.isAllItemInfoDisplayValueInARow.slice(startIndex, endIndex);
            getPagesArray();
        }
    }

    function getPagesArray() {
        const pageRange = 2;
        let pagesArray = [];
        let currentPage = $scope.currentPage;
        let totalPages = $scope.totalPages;
        if (totalPages <= (pageRange * 2) + 4) {
            for (let i = 1; i <= totalPages; i++) {
                pagesArray.push(i);
            }
        } else {
            if (currentPage <= (pageRange + 2)) {
                for (let i = 1; i <= pageRange * 2 + 2; i++) {
                    pagesArray.push(i);
                }
                pagesArray.push('...');
                pagesArray.push(totalPages);
            } else if (currentPage >= (totalPages - (pageRange + 1))) {
                for (let i = totalPages - pageRange * 2 - 1 ; i <= totalPages; i++) {
                    pagesArray.push(i);
                }
                pagesArray.unshift('...');
                pagesArray.unshift(1);
            } else {
                for (let i = (currentPage - pageRange); i <= currentPage + pageRange; i++) {
                    pagesArray.push(i);
                }
                pagesArray.unshift('...');
                pagesArray.unshift(1);
                pagesArray.push('...');
                pagesArray.push(totalPages);
            }
        }
        $scope.pagesArray = pagesArray;
    }

    function determineDisplayValueInARow(value, highlight) {
        var valueColumn = document.getElementById('valueColumn');
        var testElement = document.createElement('span');
        setupTestElement(testElement, valueColumn);
        testElement.innerText = value;
        document.body.appendChild(testElement);
        const position = determinePosition(value, highlight);
        let displayValue = '0';
        if (testElement.scrollWidth > testElement.offsetWidth) {
            displayValue = position;
        } else {
            if (testElement.scrollWidth === testElement.offsetWidth) {
                return '0';
            }
            switch (position) {
                case '1':
                    testElement.innerText = value + '...' + '| ' + $translate.instant('Global.Expand');
                    break;
                case '2':
                    testElement.innerText = '...' + value + '| ' + $translate.instant('Global.Expand');
                    break;
                case '3':
                    testElement.innerText = '...' + value + '...' + '| ' + $translate.instant('Global.Expand');
                    break;
                default:
                    return '0';
            }
            if (testElement.scrollWidth === testElement.offsetWidth) {
                displayValue = '0';
            } else {
                displayValue = position;
            }
        }
        document.body.removeChild(testElement);
        return displayValue;
    }

    function setupTestElement(element, valueColumn) {
        element.style.visibility = 'hidden';
        element.style.position = 'absolute';
        element.style.whiteSpace = 'nowrap';
        element.style.display = 'inline-block';
        element.style.fontFamily = '"Open Sans", sans-serif';
        const devicePixelRatio = window.devicePixelRatio;
        const zoomLevel = Math.round((window.outerWidth / window.innerWidth) * 100) / 100;
        element.style.fontSize = 13 * devicePixelRatio * zoomLevel + 'px';
        element.style.padding = 8 * devicePixelRatio * zoomLevel + 'px';
        element.style.width = valueColumn.offsetWidth * devicePixelRatio * zoomLevel + 'px';
    }

    function determinePosition(value, highlight) {
        const position = value.indexOf(highlight);
        if (position === -1) return '-1';
        if (position === 0) return '1';
        if (position + highlight.length === value.length) return '2';
        return "3";
    }

    function determineDisplayKeyOrValueWithoutShowHighlightKeyword(keyorvalue, highlight) {
        return keyorvalue === highlight ? '0' : '-1';
    }

    function jumpToTheEditingPage(appid,env,cluster){
        let url = AppUtil.prefixPath() + "/config.html#/appid=" + appid + "&" +"env=" + env + "&" + "cluster=" + cluster;
        window.open(url, '_blank');
    }

    function highlightKeyword(fulltext,keyword) {
        if (!keyword || keyword.length === 0) return fulltext;
        let regex = new RegExp("(" + keyword + ")", "g");
        return fulltext.replace(regex, '<span class="highlight" style="background: yellow;padding: 1px 4px;">$1</span>');
    }

    function isShowAllValue(index){
        $scope.isShowHighlightKeyword[index] = !$scope.isShowHighlightKeyword[index];
    }

}
