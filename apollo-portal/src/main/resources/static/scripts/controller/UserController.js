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
user_module.controller('UserController',
    ['$scope', '$window', '$translate', 'toastr', 'AppUtil', 'UserService', 'PermissionService',
        UserController]);

function UserController($scope, $window, $translate, toastr, AppUtil, UserService, PermissionService) {

    $scope.user = {};
    $scope.createdUsersPage = 0;
    $scope.createdUsers = [];
    $scope.hasMoreCreatedUsers = false
    $scope.status = "1"
    $scope.changeStatus = changeStatus

    initPermission();

    getCreatedUsers()

    function initPermission() {
        PermissionService.has_root_permission()
        .then(function (result) {
            $scope.isRootUser = result.hasPermission;
        })
    }

    function getCreatedUsers() {
        var size = 10;
        UserService.find_users("", $scope.createdUsersPage, size)
            .then(function (result) {
                $scope.createdUsersPage += 1;
                $scope.hasMoreCreatedUsers = result.length === size;

                if (!result || result.length === 0) {
                    return;
                }
                result.forEach(function (app) {
                    $scope.createdUsers.push(app);
                });

            })
    }

    function changeStatus(status, user){
        $scope.status = status
        $scope.user = {}
        if (user != null) {
            $scope.user = {
                username: user.userId,
                userDisplayName: user.name,
                email: user.email,
                enabled: user.enabled,
            }
        } else {

        }
    }

    $scope.createOrUpdateUser = function () {
        UserService.createOrUpdateUser($scope.user).then(function (result) {
            toastr.success($translate.instant('UserMange.Created'));
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserMange.CreateFailed'));
        })

    }
}
