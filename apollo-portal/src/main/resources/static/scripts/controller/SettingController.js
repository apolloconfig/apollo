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
setting_module.controller('SettingController',
    ['$scope', '$location', '$translate', 'toastr',
        'AppService', 'AppUtil', 'PermissionService',
        'OrganizationService',
        SettingController]);

function SettingController($scope, $location, $translate, toastr,
    AppService, AppUtil, PermissionService,
    OrganizationService) {

    var params = AppUtil.parseParams($location.$$url);
    var $orgWidget = $('#organization');

    $scope.pageContext = {
        appId: params.appid
    };
    $scope.display = {
        app: {
            edit: false
        }
    };

    $scope.submitBtnDisabled = false;
    $scope.userSelectWidgetId = 'toAssignMasterRoleUser';

    $scope.assignMasterRoleToUser = assignMasterRoleToUser;
    $scope.removeMasterRoleFromUser = removeMasterRoleFromUser;
    $scope.toggleEditStatus = toggleEditStatus;
    $scope.updateAppInfo = updateAppInfo;

    init();

    function init() {
        initOrganization();
        initPermission();
        initAdmins();
    }

    function initOrganization() {
        OrganizationService.find_organizations().then(function (result) {
            var organizations = [];
            result.forEach(function (item) {
                var org = {};
                org.id = item.orgId;
                org.text = item.orgName + '(' + item.orgId + ')';
                org.name = item.orgName;
                organizations.push(org);
            });
            $orgWidget.select2({
                placeholder: $translate.instant('Common.PleaseChooseDepartment'),
                width: '100%',
                data: organizations
            });
            initApplication();
        }, function (result) {
            toastr.error(AppUtil.errorMsg(result), "load organizations error");
        });
    }

    function initPermission() {
        PermissionService.has_assign_user_permission($scope.pageContext.appId)
            .then(function (result) {
                $scope.hasAssignUserPermission = result.hasPermission;

                PermissionService.has_open_manage_app_master_role_limit().then(function (value) {
                     if (!value.isManageAppMasterPermissionEnabled) {
                        $scope.hasManageAppMasterPermission = $scope.hasAssignUserPermission;
                        return;
                     }

                    PermissionService.has_manage_app_master_permission($scope.pageContext.appId).then(function (res) {
                        $scope.hasManageAppMasterPermission = res.hasPermission && $scope.hasAssignUserPermission;

                        PermissionService.has_root_permission().then(function (value) {
                            $scope.hasManageAppMasterPermission = value.hasPermission || $scope.hasManageAppMasterPermission;
                        });
                    });
                });
            });
    }

    function initAdmins() {
        PermissionService.get_app_role_users($scope.pageContext.appId)
            .then(function (result) {
                $scope.appRoleUsers = result;
                $scope.admins = [];
                $scope.appRoleUsers.masterUsers.forEach(function (user) {
                    $scope.admins.push(_.escape(user.userId));
                });

            });
    }

    function initApplication() {
        AppService.load($scope.pageContext.appId).then(function (app) {
            $scope.app = app;
            $scope.viewApp = _.clone(app);
            initAppForm(app);
            $('.project-setting .panel').removeClass('hidden');
        })

    }

    function initAppForm(app) {
        $orgWidget.val(app.orgId).trigger("change");

        var $ownerSelector = $('.ownerSelector');
        var defaultSelectedDOM = '<option value="' + _.escape(app.ownerName) + '" selected="selected">' + _.escape(app.ownerName)
            + '</option>';
        $ownerSelector.append(defaultSelectedDOM);
        $ownerSelector.trigger('change');
    }

    function assignMasterRoleToUser() {
        var user = $('.' + $scope.userSelectWidgetId).select2('data')[0];
        if (!user) {
            toastr.warning($translate.instant('App.Setting.PleaseChooseUser'));
            return;
        }
        var toAssignMasterRoleUser = user.id;
        $scope.submitBtnDisabled = true;
        PermissionService.assign_master_role($scope.pageContext.appId,
            toAssignMasterRoleUser)
            .then(function (result) {
                $scope.submitBtnDisabled = false;
                toastr.success($translate.instant('App.Setting.Added'));
                $scope.appRoleUsers.masterUsers.push({ userId: toAssignMasterRoleUser });
                $('.' + $scope.userSelectWidgetId).select2("val", "");
            }, function (result) {
                $scope.submitBtnDisabled = false;
                toastr.error(AppUtil.errorMsg(result), $translate.instant('App.Setting.AddFailed'));
            });
    }

    function removeMasterRoleFromUser(user) {
        if ($scope.appRoleUsers.masterUsers.length <= 1) {
            $('#warning').modal('show');
            return;
        }
        PermissionService.remove_master_role($scope.pageContext.appId, user)
            .then(function (result) {
                toastr.success($translate.instant('App.Setting.Deleted'));
                removeUserFromList($scope.appRoleUsers.masterUsers, user);
            }, function (result) {
                toastr.error(AppUtil.errorMsg(result), $translate.instant('App.Setting.DeleteFailed'));
            });
    }

    function removeUserFromList(list, user) {
        var index = 0;
        for (var i = 0; i < list.length; i++) {
            if (list[i].userId == user) {
                index = i;
                break;
            }
        }
        list.splice(index, 1);
    }

    function toggleEditStatus() {
        if ($scope.display.app.edit) {//cancel edit
            $scope.viewApp = _.clone($scope.app);
            initAppForm($scope.viewApp);
        } else {//edit

        }

        $scope.display.app.edit = !$scope.display.app.edit;

    }

    function updateAppInfo() {
        $scope.submitBtnDisabled = true;
        var app = $scope.viewApp;

        var selectedOrg = $orgWidget.select2('data')[0];

        if (!selectedOrg.id) {
            toastr.warning($translate.instant('Common.PleaseChooseDepartment'));
            return;
        }

        app.orgId = selectedOrg.id;
        app.orgName = selectedOrg.name;

        // owner
        var owner = $('.ownerSelector').select2('data')[0];
        if (!owner) {
            toastr.warning($translate.instant('Common.PleaseChooseOwner'));
            return;
        }
        app.ownerName = owner.id;

        AppService.update(app).then(function (app) {
            toastr.success($translate.instant('App.Setting.Modified'));
            initApplication();
            $scope.display.app.edit = false;
            $scope.submitBtnDisabled = false;
        }, function (result) {
            AppUtil.showErrorMsg(result);
            $scope.submitBtnDisabled = false;
        })

    }
}
