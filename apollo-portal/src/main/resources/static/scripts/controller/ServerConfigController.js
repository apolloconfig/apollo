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
server_config_manage_module.controller('ServerConfigController',
    ['$scope', '$window', '$translate', 'toastr', 'AppUtil', 'ServerConfigService', 'PermissionService',
        'EnvService', ServerConfigController]);

function ServerConfigController($scope, $window, $translate, toastr, AppUtil, ServerConfigService, PermissionService, EnvService) {

    $scope.serverConfig = {};
    $scope.portalDBConfigs = [];
    $scope.portalDBFilterConfigs = [];
    $scope.configDBConfigs = [];
    $scope.configDBFilterConfigs = [];
    $scope.envs = [];
    $scope.selectedEnv = '';
    $scope.displayModule = 'home';
    $scope.portalDBConfigSearchKey = '';
    $scope.configDBConfigSearchKey = '';
    $scope.configEdit = configEdit;
    $scope.createPortalDBConfig = createPortalDBConfig;
    $scope.createConfigDBConfig = createConfigDBConfig;
    $scope.gobackPortalDBTabs = gobackPortalDBTabs;
    $scope.gobackConfigDBTabs = gobackConfigDBTabs;
    $scope.portalDBConfigFilter = portalDBConfigFilter;
    $scope.configDBConfigFilter = configDBConfigFilter;
    $scope.resetPortalDBConfigSearchKey = resetPortalDBConfigSearchKey;
    $scope.resetConfigDBConfigSearchKey = resetConfigDBConfigSearchKey;
    $scope.switchConfigDBEnvs = switchConfigDBEnvs;

    $scope.allowSwitchingTabs = true;

    init();
    function init() {
        initPermission();
        getPortalDBConfig();
        initEnv();
    }
    function initEnv() {
        EnvService.find_all_envs().then(function (result) {
            $scope.envs = result;
            $scope.selectedEnv = result[0];
            getConfigDBConfig();
        });
    }
    function initPermission() {
        PermissionService.has_root_permission()
        .then(function (result) {
            $scope.isRootUser = result.hasPermission;
        });
    }

    function getPortalDBConfig() {
        ServerConfigService.findPortalDBConfig()
        .then(function (result) {
            $scope.portalDBConfigs = [];
            $scope.portalDBFilterConfigs = [];
            result.forEach(function (user) {
                $scope.portalDBConfigs.push(user);
                $scope.portalDBFilterConfigs.push(user);
            });
        },function (result) {
            $scope.portalDBConfigs = [];
            $scope.portalDBFilterConfigs = [];
            toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.SystemError'));
        });
    }

    function getConfigDBConfig() {
        ServerConfigService.findConfigDBConfig($scope.selectedEnv)
        .then(function (result) {
            $scope.configDBConfigs = [];
            $scope.configDBFilterConfigs = [];
            result.forEach(function (user) {
                $scope.configDBConfigs.push(user);
                $scope.configDBFilterConfigs.push(user);
            });
        },function (result) {
            $scope.configDBConfigs = [];
            $scope.configDBFilterConfigs = [];
            toastr.error(AppUtil.errorMsg(result), $translate.instant('Config.SystemError'));
        });
    }

    function configEdit (displayModule,config) {
        $scope.displayModule = displayModule;
        $scope.allowSwitchingTabs = false;

        $scope.serverConfig = {};
        if (config != null) {
            $scope.serverConfig = {
                key: config.key,
                value: config.value,
                comment: config.comment
            };
        }
    }

    function switchConfigDBEnvs(env) {
        $scope.selectedEnv = env;
        getConfigDBConfig();
    }

    function createPortalDBConfig() {
        ServerConfigService.createPortalDBConfig($scope.serverConfig).then(function (result) {
            toastr.success($translate.instant('ServiceConfig.Saved'));
            getPortalDBConfig();
            $scope.displayModule = 'home';
            $scope.allowSwitchingTabs = true;
        }, function (result) {
            toastr.error(AppUtil.errorMsg(result), $translate.instant('ServiceConfig.SaveFailed'));
        });
    }

    function createConfigDBConfig() {
        ServerConfigService.createConfigDBConfig($scope.selectedEnv, $scope.serverConfig).then(function (result) {
            toastr.success($translate.instant('ServiceConfig.Saved'));
            getConfigDBConfig();
            $scope.displayModule = 'home';
            $scope.allowSwitchingTabs = true;
        }, function (result) {
            toastr.error(AppUtil.errorMsg(result), $translate.instant('ServiceConfig.SaveFailed'));
        });
    }


    function gobackPortalDBTabs(){
        $scope.displayModule = 'home';
        $scope.allowSwitchingTabs = true;
        getPortalDBConfig();
    }

    function gobackConfigDBTabs(){
        $scope.displayModule = 'home';
        $scope.allowSwitchingTabs = true;
        getConfigDBConfig();
    }

    function portalDBConfigFilter() {
        $scope.portalDBConfigSearchKey = $scope.portalDBConfigSearchKey.toLowerCase();
        let filterConfig = [];
        $scope.portalDBConfigs.forEach(function (item) {
            let keyName = item.key;
            if (keyName && keyName.toLowerCase().indexOf( $scope.portalDBConfigSearchKey) >= 0) {
                filterConfig.push(item);
            }
        });
        $scope.portalDBFilterConfigs = filterConfig;
    }

    function resetPortalDBConfigSearchKey() {
        $scope.portalDBConfigSearchKey = '';
        portalDBConfigFilter();
    }

    function configDBConfigFilter() {
        $scope.configDBConfigSearchKey = $scope.configDBConfigSearchKey.toLowerCase();
        let filterConfig = [];
        $scope.configDBConfigs.forEach(function (item) {
            let keyName = item.key;
            if (keyName && keyName.toLowerCase().indexOf( $scope.configDBConfigSearchKey) >= 0) {
                filterConfig.push(item);
            }
        });
        $scope.configDBFilterConfigs = filterConfig;
    }

    function resetConfigDBConfigSearchKey() {
        $scope.configDBConfigSearchKey = '';
        configDBConfigFilter();
    }
}
