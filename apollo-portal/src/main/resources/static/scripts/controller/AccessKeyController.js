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
access_key_module.controller('AccessKeyController',
    ['$scope', '$location', '$translate', 'toastr',
        'AppService', 'AppUtil', 'PermissionService',
        'EnvService', 'UserService', 'AccessKeyService',
        AccessKeyController]);

function AccessKeyController($scope, $location, $translate, toastr,
                             AppService, AppUtil, PermissionService,
                             EnvService, UserService, AccessKeyService) {

    var params = AppUtil.parseParams($location.$$url);

    $scope.pageContext = {
        appId: params.appid
    };
    $scope.display = {
        app: {
            edit: false
        }
    };

    $scope.addAccessKeySelectedEnv = "";
    $scope.accessKeys = null;

    $scope.submitBtnDisabled = false;
    $scope.userSelectWidgetId = 'toAssignMasterRoleUser';

    $scope.create = create;
    $scope.remove = remove;
    $scope.enable = enable;
    $scope.disable = disable;

    init();

    function init() {
        initPermission();
        initAdmins();
        initApplication();
    }

    function initPermission() {
        PermissionService.has_assign_user_permission($scope.pageContext.appId)
            .then(function (result) {
                $scope.hasAssignUserPermission = result.hasPermission;

                if (result.hasPermission) {
                    initEnv();
                }
            });
    }

    function initEnv() {
        EnvService.find_all_envs()
        .then(function (result) {
            $scope.envs = result;
            initAccessKeys();
        });
    }

    function initAccessKeys() {
        $scope.accessKeys = {};
        for (var iLoop = 0; iLoop < $scope.envs.length; iLoop++) {
            loadAccessKeys($scope.envs[iLoop])
        }
    }

    function loadAccessKeys(env) {
        AccessKeyService.load_access_keys($scope.pageContext.appId, env)
            .then(function (result) {
                $scope.accessKeys[env] = result;
            }, function (result) {
                toastr.error(AppUtil.errorMsg(result), $translate.instant('AccessKey.LoadError', { env }));
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
            $('.project-setting .panel').removeClass('hidden');
        })
    }

    function create() {
        var env = $scope.addAccessKeySelectedEnv;
        UserService.load_user().then(function (result) {
            AccessKeyService.create_access_key($scope.pageContext.appId, env, result.userId)
                .then(function () {
                    toastr.success($translate.instant('AccessKey.Operator.CreateSuccess', {env}));
                    loadAccessKeys(env);
                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('AccessKey.Operator.CreateError', {env}));
                });
        });
    }

    function remove(id, env) {
        var confirmTips = $translate.instant('AccessKey.Operator.RemoveTips', {
            appId: $scope.pageContext.appId
        });
        if (confirm(confirmTips)) {
            AccessKeyService.remove_access_key($scope.pageContext.appId, env, id)
                .then(function () {
                    toastr.success($translate.instant('AccessKey.Operator.RemoveSuccess', {env}));
                    loadAccessKeys(env);
                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('AccessKey.Operator.RemoveError', {env}));
                });
        }
    }

    function enable(id, env, mode) {
        mode = (mode === 1) ? 1 : 0;
        var tipsPrefix = mode === 1 ? 'AccessKey.Operator.Observed' : 'AccessKey.Operator.Enabled';
        var confirmTips = $translate.instant(tipsPrefix + 'Tips', {
            appId: $scope.pageContext.appId
        });
        if (confirm(confirmTips)) {
            AccessKeyService.enable_access_key($scope.pageContext.appId, env, id, mode)
                .then(function () {
                    toastr.success($translate.instant(tipsPrefix + 'Success', {env}));
                    loadAccessKeys(env);
                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant(tipsPrefix + 'Error', {env}));
                });
        }
    }

    function disable(id, env) {
        var confirmTips = $translate.instant('AccessKey.Operator.DisabledTips', {
            appId: $scope.pageContext.appId
        });
        if (confirm(confirmTips)) {
            AccessKeyService.disable_access_key($scope.pageContext.appId, env, id)
                .then(function () {
                    toastr.success($translate.instant('AccessKey.Operator.DisabledSuccess', {env}));
                    loadAccessKeys(env);
                }, function (result) {
                    toastr.error(AppUtil.errorMsg(result), $translate.instant('AccessKey.Operator.DisabledError', {env}));
                });
        }
    }
}
