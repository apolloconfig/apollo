/*
 * Copyright 2022 Apollo Authors
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
user_module.controller('ConfigController',
    ['$scope', '$window', '$translate', 'toastr', 'AppUtil', 'ServerConfigService', 'PermissionService',
        ConfigController]);

function ConfigController($scope, $window, $translate, toastr, AppUtil, ServerConfigService, PermissionService) {

    $scope.serverConfig = {};
    $scope.createdConfigs = [];
    $scope.filterConfig = [];
    $scope.status = '1';
    $scope.searchKey = '';
    $scope.configEdit = configEdit;
    $scope.create = create;
    $scope.goback = goback;
    $scope.portalDB = portalDB;
    $scope.searchKeys = searchKeys;
    $scope.resetSearchKey = resetSearchKey;
    $scope.ConfigPage = 0;


    initPermission();

    getPortalDBConfig();

    function initPermission() {
        PermissionService.has_root_permission()
        .then(function (result) {
            $scope.isRootUser = result.hasPermission;
        })
    }

    function getPortalDBConfig() {
        ServerConfigService.findPortalDBConfig()
        .then(function (result) {
            if (!result || result.length === 0) {
                $scope.ConfigPage = $scope.ConfigPage - 1;
                return;
            }
            $scope.createdConfigs = [];
            $scope.filterConfig = [];
            result.forEach(function (user) {
                $scope.createdConfigs.push(user);
                $scope.filterConfig.push(user);
            });
        })
    }


    function configEdit (status,config) {
        $scope.status = status;

        $scope.serverConfig = {};
        if (config != null) {
            $scope.serverConfig = {
                key: config.key,
                value: config.value,
                comment: config.comment
            }
        }
    }

    function create() {
        ServerConfigService.create($scope.serverConfig).then(function (result) {
            toastr.success($translate.instant('ServiceConfig.Saved'));
            $scope.serverConfig = result;
            getPortalDBConfig();
            $scope.status = '1';
        }, function (result) {
            toastr.error(AppUtil.errorMsg(result), $translate.instant('ServiceConfig.SaveFailed'));
        });
    }


    function goback(){
        $scope.status = '1';

        getPortalDBConfig();
    }


    function portalDB(){
        $scope.status = '1';
        $scope.ConfigPage = 0;
        getPortalDBConfig();
    }

    function searchKeys() {
        $scope.searchKey = $scope.searchKey.toLowerCase();
        var filterConfig = []
        $scope.createdConfigs.forEach(function (item) {
            var keyName = item.key;
            if (keyName && keyName.toLowerCase().indexOf( $scope.searchKey) >= 0) {
                filterConfig.push(item);
            }
        });
        $scope.filterConfig = filterConfig
    }

    function resetSearchKey() {
        $scope.searchKey = ''
        searchKeys()
    }


}
