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
user_module.controller('UserController',
    ['$scope', '$window', '$translate', 'toastr', 'AppUtil', 'UserService', 'PermissionService',
        UserController]);

function UserController($scope, $window, $translate, toastr, AppUtil, UserService, PermissionService) {

    $scope.user = {};
    $scope.createdUsers = [];
    $scope.filterUser = [];
    $scope.status = '1'
    $scope.searchKey = ''
    $scope.changeStatus = changeStatus
    $scope.searchUsers = searchUsers
    $scope.resetSearchUser = resetSearchUser

    initPermission();

    getCreatedUsers();

    function initPermission() {
        PermissionService.has_root_permission()
        .then(function (result) {
            $scope.isRootUser = result.hasPermission;
        })
    }

    function getCreatedUsers() {
        UserService.find_users("",true)
        .then(function (result) {
            if (!result || result.length === 0) {
                return;
            }
            $scope.createdUsers = [];
            $scope.filterUser = [];
            result.forEach(function (user) {
                $scope.createdUsers.push(user);
                $scope.filterUser.push(user);
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
        }
    }

    function searchUsers() {
        $scope.searchKey = $scope.searchKey.toLowerCase();
        var filterUser = []
        $scope.createdUsers.forEach(function (item) {
            var userLoginName = item.userId;
            if (userLoginName && userLoginName.toLowerCase().indexOf( $scope.searchKey) >= 0) {
                filterUser.push(item);
            }
        });
        $scope.filterUser = filterUser
    }

    function resetSearchUser() {
        $scope.searchKey = ''
        searchUsers()
    }

    $scope.changeUserEnabled = function (user) {
        var newUser={}
        if (user != null) {
            newUser = {
                username: user.userId,
                userDisplayName: user.name,
                email: user.email,
                enabled: user.enabled === 1 ? 0 : 1,
            }
        }
        UserService.change_user_enabled(newUser).then(function (result) {
            toastr.success($translate.instant('UserMange.Enabled.succeed'));
            getCreatedUsers()
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserMange.Enabled.failure'));
        })
    }

    $scope.createOrUpdateUser = function () {
        if ($scope.status === '2') {
            UserService.createOrUpdateUser(true, $scope.user).then(function (result) {
                toastr.success($translate.instant('UserMange.Created'));
                getCreatedUsers()
                changeStatus('1')
            }, function (result) {
                AppUtil.showErrorMsg(result, $translate.instant('UserMange.CreateFailed'));
            })
        } else {
            UserService.createOrUpdateUser(false,$scope.user).then(function (result) {
                toastr.success($translate.instant('UserMange.Edited'));
                getCreatedUsers()
                changeStatus('1')
            }, function (result) {
                AppUtil.showErrorMsg(result, $translate.instant('UserMange.EditFailed'));
            })
        }


    }
}
