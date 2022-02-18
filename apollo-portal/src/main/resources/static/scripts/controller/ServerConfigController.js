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
server_config_module.controller('ServerConfigController',
    ['$scope', '$window', '$translate', 'toastr', 'ServerConfigService', 'AppUtil', 'PermissionService',
        function ($scope, $window, $translate, toastr, ServerConfigService, AppUtil, PermissionService) {

            $scope.serverConfig = {};
            $scope.saveBtnDisabled = true;

            initPermission();

            function initPermission() {
                PermissionService.has_root_permission()
                .then(function (result) {
                    $scope.isRootUser = result.hasPermission;
                })
            }

            $scope.create = function () {
                ServerConfigService.create($scope.serverConfig).then(function (result) {
                    toastr.success($translate.instant('ServiceConfig.Saved'));
                    $scope.saveBtnDisabled = true;
                    $scope.serverConfig = result;
                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('ServiceConfig.SaveFailed'));
                });
            };

            $scope.getServerConfigInfo = function () {
                if (!$scope.serverConfig.key) {
                    toastr.warning($translate.instant('ServiceConfig.PleaseEnterKey'));
                    return;
                }

                ServerConfigService.getServerConfigInfo($scope.serverConfig.key).then(function (result) {
                    $scope.saveBtnDisabled = false;

                    if (!result.key) {
                        toastr.info($translate.instant('ServiceConfig.KeyNotExistsAndCreateTip', { key: $scope.serverConfig.key }));
                        return;
                    }

                    toastr.info($translate.instant('ServiceConfig.KeyExistsAndSaveTip', { key: $scope.serverConfig.key }));
                    $scope.serverConfig = result;
                }, function (result) {
                    AppUtil.showErrorMsg(result);
                })
            }

        }]);
