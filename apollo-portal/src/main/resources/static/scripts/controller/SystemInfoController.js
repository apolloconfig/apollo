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
system_info_module.controller('SystemInfoController',
                              ['$scope', 'toastr', 'AppUtil', 'AppService', 'ClusterService', 'NamespaceService', 'PermissionService', 'SystemInfoService',
                               SystemInfoController]);

function SystemInfoController($scope, toastr, AppUtil, AppService, ClusterService, NamespaceService, PermissionService, SystemInfoService) {

    $scope.systemInfo = {};
    $scope.check = check;

    initPermission();

    function initPermission() {
        PermissionService.has_root_permission()
            .then(function (result) {
                  $scope.isRootUser = result.hasPermission;

                  if (result.hasPermission) {
                      loadSystemInfo();
                  }
            })
    }

    function loadSystemInfo() {
        SystemInfoService.load_system_info().then(function (result) {
            $scope.systemInfo = result;
        }, function (result) {
            AppUtil.showErrorMsg(result);
        });
    }

    function check(instanceId, host) {
        SystemInfoService.check_health(instanceId, host).then(function (result) {
            var status = result.status.code;
            if (status === 'UP') {
                toastr.success(host + ' is healthy!');
            } else {
                toastr.error(host + ' is not healthy, please check ' + host + '/health for more information!');
            }
        }, function (result) {
            AppUtil.showErrorMsg(result);
        });
    }
}
