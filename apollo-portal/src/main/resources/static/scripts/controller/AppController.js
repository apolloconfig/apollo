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
app_module.controller('CreateAppController',
    ['$scope', '$window', '$translate', 'toastr', 'AppService', 'AppUtil', 'OrganizationService', 'SystemRoleService', 'UserService',
        createAppController]);

function createAppController($scope, $window, $translate, toastr, AppService, AppUtil, OrganizationService, SystemRoleService, UserService) {

    $scope.app = {};
    $scope.submitBtnDisabled = false;

    $scope.create = create;

    init();

    function init() {
        initOrganization();
        initSystemRole();
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
            $('#organization').select2({
                placeholder: $translate.instant('Common.PleaseChooseDepartment'),
                width: '100%',
                data: organizations
            });
        }, function (result) {
            toastr.error(AppUtil.errorMsg(result), "load organizations error");
        });
    }

    function initSystemRole() {
        SystemRoleService.has_open_manage_app_master_role_limit().then(
            function (value) {
                $scope.isOpenManageAppMasterRoleLimit = value.isManageAppMasterPermissionEnabled;
                UserService.load_user().then(
                    function (value1) {
                        $scope.currentUser = value1;
                    },
                    function (reason) {
                        toastr.error(AppUtil.errorMsg(reason), "load current user info failed");
                    })
            },
            function (reason) {
                toastr.error(AppUtil.errorMsg(reason), "init system role of manageAppMaster failed");
            }
        );
    }

    function create() {
        $scope.submitBtnDisabled = true;

        var selectedOrg = $('#organization').select2('data')[0];

        if (!selectedOrg.id) {
            toastr.warning($translate.instant('Common.PleaseChooseDepartment'));
            $scope.submitBtnDisabled = false;
            return;
        }

        $scope.app.orgId = selectedOrg.id;
        $scope.app.orgName = selectedOrg.name;

        // owner
        var owner = $('.ownerSelector').select2('data')[0];
        if ($scope.isOpenManageAppMasterRoleLimit) {
            owner = { id: $scope.currentUser.userId };
        }
        if (!owner) {
            toastr.warning($translate.instant('Common.PleaseChooseOwner'));
            $scope.submitBtnDisabled = false;
            return;
        }
        $scope.app.ownerName = owner.id;

        //admins
        $scope.app.admins = [];
        var admins = $(".adminSelector").select2('data');
        if ($scope.isOpenManageAppMasterRoleLimit) {
            admins = [{ id: $scope.currentUser.userId }];
        }
        if (admins) {
            admins.forEach(function (admin) {
                $scope.app.admins.push(admin.id);
            })
        }

        AppService.create($scope.app).then(function (result) {
            toastr.success($translate.instant('Common.Created'));
            setInterval(function () {
                $scope.submitBtnDisabled = false;
                $window.location.href = AppUtil.prefixPath() + '/config.html?#appid=' + result.appId;
            }, 1000);
        }, function (result) {
            $scope.submitBtnDisabled = false;
            toastr.error(AppUtil.errorMsg(result), $translate.instant('Common.CreateFailed'));
        });
    }


    $(".J_ownerSelectorPanel").on("select2:select", ".ownerSelector", selectEventHandler);
    var $adminSelectorPanel = $(".J_adminSelectorPanel");
    $adminSelectorPanel.on("select2:select", ".adminSelector", selectEventHandler);
    $adminSelectorPanel.on("select2:unselect", ".adminSelector", selectEventHandler);

    function selectEventHandler() {
        $('.J_owner').remove();

        var owner = $('.ownerSelector').select2('data')[0];

        if (owner) {
            $(".adminSelector").parent().find(".select2-selection__rendered").prepend(
                '<li class="select2-selection__choice J_owner">'
                + _.escape(owner.text) + '</li>')
        }
    }
}
