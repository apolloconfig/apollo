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
    $scope.filterConfig = [];
    $scope.status = '1';
    $scope.previous = previous;
    $scope.next = next;
    $scope.configEdit = configEdit;
    $scope.create = create;
    $scope.goback = goback;
    $scope.portalDB = portalDB;
    $scope.ConfigPage = 0;

    var pageSize = 10;

    initPermission();

    getPortalDBConfig();

    function initPermission() {
        PermissionService.has_root_permission()
        .then(function (result) {
            $scope.isRootUser = result.hasPermission;
        })
    }

    function getPortalDBConfig() {
        ServerConfigService.find_portalDBConfig($scope.ConfigPage, pageSize)
        .then(function (result) {
            if (!result || result.length === 0) {
                AppUtil.showWarningMsg("It's already the last page");
                $scope.ConfigPage = $scope.ConfigPage - 1;
                return;
            }
            $scope.filterConfig = [];
            result.forEach(function (user) {
                $scope.filterConfig.push(user);
            });
        })
    }


    function previous(){
        if($scope.ConfigPage == 0){
            AppUtil.showWarningMsg("It's already the first page");
        }else{
            $scope.ConfigPage = $scope.ConfigPage - 1;
            getPortalDBConfig();
        }
    }


    function next(){
        $scope.ConfigPage = $scope.ConfigPage + 1;

        getPortalDBConfig();
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

}
