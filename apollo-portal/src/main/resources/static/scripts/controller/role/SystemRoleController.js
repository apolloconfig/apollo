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
angular.module('systemRole', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar'])
    .controller('SystemRoleController',
        ['$scope', '$location', '$window', '$translate', 'toastr', 'AppService', 'UserService', 'AppUtil', 'EnvService',
            'PermissionService', 'SystemRoleService', function SystemRoleController($scope, $location, $window, $translate, toastr, AppService, UserService, AppUtil, EnvService,
                PermissionService, SystemRoleService) {

                $scope.addCreateApplicationBtnDisabled = false;
                $scope.deleteCreateApplicationBtnDisabled = false;

                $scope.modifySystemRoleWidgetId = 'modifySystemRoleWidgetId';
                $scope.modifyManageAppMasterRoleWidgetId = 'modifyManageAppMasterRoleWidgetId';

                $scope.hasCreateApplicationPermissionUserList = [];

                $scope.operateManageAppMasterRoleBtn = true;

                $scope.app = {
                    appId: "",
                    info: ""
                };

                initPermission();

                $scope.addCreateApplicationRoleToUser = function () {
                    var user = $('.' + $scope.modifySystemRoleWidgetId).select2('data')[0];
                    if (!user) {
                        toastr.warning($translate.instant('SystemRole.PleaseChooseUser'));
                        return;
                    }
                    SystemRoleService.add_create_application_role(user.id)
                        .then(
                            function (value) {
                                toastr.info($translate.instant('SystemRole.Added'));
                                getCreateApplicationRoleUsers();
                            },
                            function (reason) {
                                toastr.warning(AppUtil.errorMsg(reason), $translate.instant('SystemRole.AddFailed'));
                            }
                        );
                };

                $scope.deleteCreateApplicationRoleFromUser = function (userId) {
                    SystemRoleService.delete_create_application_role(userId)
                        .then(
                            function (value) {
                                toastr.info($translate.instant('SystemRole.Deleted'));
                                getCreateApplicationRoleUsers();
                            },
                            function (reason) {
                                toastr.warning(AppUtil.errorMsg(reason), $translate.instant('SystemRole.DeleteFailed'));
                            }
                        );
                };


                function getCreateApplicationRoleUsers() {
                    SystemRoleService.get_create_application_role_users()
                        .then(
                            function (result) {
                                $scope.hasCreateApplicationPermissionUserList = result;
                            },
                            function (reason) {
                                toastr.warning(AppUtil.errorMsg(reason), $translate.instant('SystemRole.GetCanCreateProjectUsersError'));
                            }
                        )
                }

                function initPermission() {
                    PermissionService.has_root_permission()
                        .then(function (result) {
                            $scope.isRootUser = result.hasPermission;
                            if ($scope.isRootUser) {
                                getCreateApplicationRoleUsers();
                            }
                        });
                }

                $scope.getAppInfo = function () {
                    if (!$scope.app.appId) {
                        toastr.warning($translate.instant('SystemRole.PleaseEnterAppId'));
                        $scope.operateManageAppMasterRoleBtn = true;
                        return;
                    }

                    $scope.app.info = "";

                    AppService.load($scope.app.appId).then(function (result) {
                        if (!result.appId) {
                            toastr.warning($translate.instant('SystemRole.AppIdNotFound', { appId: $scope.app.appId }));
                            $scope.operateManageAppMasterRoleBtn = true;
                            return;
                        }

                        $scope.app.info = $translate.instant('SystemRole.AppInfoContent', {
                            appName: result.name,
                            departmentName: result.orgName,
                            departmentId: result.orgId,
                            ownerName: result.ownerName
                        });

                        $scope.operateManageAppMasterRoleBtn = false;
                    }, function (result) {
                        AppUtil.showErrorMsg(result);
                        $scope.operateManageAppMasterRoleBt = true;
                    });
                };

                $scope.deleteAppMasterAssignRole = function () {
                    if (!$scope.app.appId) {
                        toastr.warning($translate.instant('SystemRole.PleaseEnterAppId'));
                        return;
                    }
                    var user = $('.' + $scope.modifyManageAppMasterRoleWidgetId).select2('data')[0];
                    if (!user) {
                        toastr.warning($translate.instant('SystemRole.PleaseChooseUser'));
                        return;
                    }
                    var confirmTips = $translate.instant('SystemRole.DeleteMasterAssignRoleTips', {
                        appId: $scope.app.appId,
                        userId: user.id
                    });
                    if (confirm(confirmTips)) {
                        AppService.delete_app_master_assign_role($scope.app.appId, user.id).then(function (result) {
                            var deletedTips = $translate.instant('SystemRole.DeletedMasterAssignRoleTips', {
                                appId: $scope.app.appId,
                                userId: user.id
                            });
                            toastr.success(deletedTips);
                            $scope.operateManageAppMasterRoleBtn = true;
                        }, function (result) {
                            AppUtil.showErrorMsg(result);
                        })
                    }
                };

                $scope.allowAppMasterAssignRole = function () {
                    if (!$scope.app.appId) {
                        toastr.warning($translate.instant('SystemRole.PleaseEnterAppId'));
                        return;
                    }
                    var user = $('.' + $scope.modifyManageAppMasterRoleWidgetId).select2('data')[0];
                    if (!user) {
                        toastr.warning($translate.instant('SystemRole.PleaseChooseUser'));
                        return;
                    }
                    var confirmTips = $translate.instant('SystemRole.AllowAppMasterAssignRoleTips', {
                        appId: $scope.app.appId,
                        userId: user.id
                    });
                    if (confirm(confirmTips)) {
                        AppService.allow_app_master_assign_role($scope.app.appId, user.id).then(function (result) {

                            var allowedTips = $translate.instant('SystemRole.AllowedAppMasterAssignRoleTips', {
                                appId: $scope.app.appId,
                                userId: user.id
                            });
                            toastr.success(allowedTips);
                            $scope.operateManageAppMasterRoleBtn = true;
                        }, function (result) {
                            AppUtil.showErrorMsg(result);
                        })
                    }
                };
            }]);
