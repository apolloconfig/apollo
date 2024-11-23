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
open_manage_module.controller('OpenManageController',
    ['$scope', '$translate', 'toastr', 'AppUtil', 'OrganizationService', 'ConsumerService', 'PermissionService', 'EnvService',
        OpenManageController]);

function OpenManageController($scope, $translate, toastr, AppUtil, OrganizationService, ConsumerService, PermissionService, EnvService) {

    var $orgWidget = $('#organization');

    $scope.consumer = {};
    $scope.consumerRole = {
        type: 'NamespaceRole'
    };

    $scope.submitBtnDisabled = false;
    $scope.userSelectWidgetId = 'toAssignMasterRoleUser';
    $scope.consumerListPage = 0;
    $scope.consumerList = [];
    $scope.hasMoreconsumerList = false;
    $scope.toOperationConsumer={}

    $scope.getTokenByAppId = getTokenByAppId;
    $scope.createConsumer = createConsumer;
    $scope.assignRoleToConsumer = assignRoleToConsumer;
    $scope.getConsumerList = getConsumerList;
    $scope.preDeleteConsumer = preDeleteConsumer;
    $scope.deleteConsumer = deleteConsumer;
    $scope.preGrantPermission = preGrantPermission;
    $scope.toggleRateLimitEnabledInput = function() {
        if (!$scope.consumer.rateLimitEnabled) {
            $scope.consumer.rateLimit = 0;
        }
    };

    function init() {
        initOrganization();
        initPermission();
        initEnv();
        getConsumerList();
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
        }, function (result) {
            toastr.error(AppUtil.errorMsg(result), "load organizations error");
        });
    }

    function getConsumerList() {
        var size = 10;
        ConsumerService.getConsumerList($scope.consumerListPage, size)
        .then(function (result) {
            $scope.consumerListPage += 1;
            $scope.hasMoreconsumerList = result.length === size;

            if (!result || result.length === 0) {
                return;
            }
            result.forEach(function (app) {
                $scope.consumerList.push(app);
            });

        })
    }

    let toDeleteAppId = '';

    function preDeleteConsumer(app) {
        $scope.toOperationConsumer = app;
        toDeleteAppId = app.appId;
        $("#deleteConsumerDialog").modal("show");
    }

    function deleteConsumer(){
        ConsumerService.deleteConsumer(toDeleteAppId)
        .then(function () {
            toastr.success($translate.instant('Open.Manage.DeleteConsumer.Success'));
            $scope.consumerList = $scope.consumerList.filter(consumer => consumer.appId !== toDeleteAppId);
        },function (error) {
            toastr.error(AppUtil.errorMsg(error), $translate.instant('Open.Manage.DeleteConsumer.Error'));
        })
    }

    function initPermission() {
        PermissionService.has_root_permission()
            .then(function (result) {
                $scope.isRootUser = result.hasPermission;
            });
    }

    function initEnv() {
        EnvService.find_all_envs()
            .then(function (result) {
                $scope.envs = new Array();
                for (var iLoop = 0; iLoop < result.length; iLoop++) {
                    $scope.envs.push({ checked: false, env: result[iLoop] });
                    $scope.envsChecked = new Array();
                }

                $scope.switchSelect = function (item) {
                    item.checked = !item.checked;
                    $scope.envsChecked = new Array();
                    for (var iLoop = 0; iLoop < $scope.envs.length; iLoop++) {
                        var env = $scope.envs[iLoop];
                        if (env.checked) {
                            $scope.envsChecked.push(env.env);
                        }
                    }
                };
            });
    }

    function getTokenByAppId() {
        if (!$scope.consumer.appId) {
            toastr.warning($translate.instant('Open.Manage.PleaseEnterAppId'));
            return;
        }

        ConsumerService.getConsumerTokenByAppId($scope.consumer.appId)
            .then(function (consumerToken) {

                if (consumerToken.token) {
                    $scope.consumerToken = consumerToken;
                    $scope.consumerRole.token = consumerToken.token;
                } else {
                    $scope.consumerToken = {
                        token: $translate.instant('Open.Manage.AppNotCreated', { appId: $scope.consumer.appId })
                    };
                }
            }, function (response) {
                AppUtil.showErrorMsg(response);
            });
    }

    function createConsumer() {
        $scope.submitBtnDisabled = true;

        if (!$scope.consumer.appId) {
            toastr.warning($translate.instant('Open.Manage.PleaseEnterAppId'));
            $scope.submitBtnDisabled = false;
            return;
        }

        if ($scope.consumer.rateLimitEnabled) {
            if (!$scope.consumer.rateLimit || $scope.consumer.rateLimit < 1) {
                toastr.warning($translate.instant('Open.Manage.Consumer.RateLimitValue.Error'));
                $scope.submitBtnDisabled = false;
                return;
            }
        } else {
            $scope.consumer.rateLimit = 0;
        }

        var selectedOrg = $orgWidget.select2('data')[0];

        if (!selectedOrg.id) {
            toastr.warning($translate.instant('Common.PleaseChooseDepartment'));
            $scope.submitBtnDisabled = false;
            return;
        }

        $scope.consumer.orgId = selectedOrg.id;
        $scope.consumer.orgName = selectedOrg.name;

        // owner
        var owner = $('.ownerSelector').select2('data')[0];
        if (!owner) {
            toastr.warning($translate.instant('Common.PleaseChooseOwner'));
            $scope.submitBtnDisabled = false;
            return;
        }
        $scope.consumer.ownerName = owner.id;

        ConsumerService.createConsumer($scope.consumer)
            .then(function (consumerToken) {
                toastr.success($translate.instant('Common.Created'));
                $scope.consumerToken = consumerToken;
                $scope.consumerRole.token = consumerToken.token;
                $scope.submitBtnDisabled = false;
                $scope.consumer = {};
            }, function (response) {
                AppUtil.showErrorMsg(response, $translate.instant('Common.CreateFailed'));
                $scope.submitBtnDisabled = false;
            })

    }

    function preGrantPermission(app){
        $scope.consumer.appId = app.appId;
        getTokenByAppId();
        AppUtil.showModal('#grantPermissionModal');
    }

    function assignRoleToConsumer() {
        ConsumerService.assignRoleToConsumer($scope.consumerRole.token,
            $scope.consumerRole.type,
            $scope.consumerRole.appId,
            $scope.consumerRole.namespaceName,
            $scope.envsChecked)
            .then(function (consumerRoles) {
                toastr.success($translate.instant('Open.Manage.GrantSuccessfully'));
            }, function (response) {
                AppUtil.showErrorMsg(response, $translate.instant('Open.Manage.GrantFailed'));
            })
    }

    init();
}
