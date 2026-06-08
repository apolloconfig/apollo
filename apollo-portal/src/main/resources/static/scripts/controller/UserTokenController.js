/*
 * Copyright 2025 Apollo Authors
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
user_token_module.controller('UserTokenController',
    ['$scope', '$translate', 'toastr', 'AppUtil', 'UserTokenService', 'EnvService', 'AppService',
        UserTokenController]);

function UserTokenController($scope, $translate, toastr, AppUtil, UserTokenService, EnvService, AppService) {
    $scope.tokens = [];
    $scope.capabilities = {};
    $scope.envs = [];
    $scope.availableApps = [];
    $scope.selectedApps = [];
    $scope.createdTokenValue = '';
    $scope.operationSelection = {};
    $scope.envSelection = {};
    $scope.selectedAvailableAppIds = [];
    $scope.selectedSelectedAppIds = [];
    $scope.selectedToken = {};
    $scope.form = {};

    $scope.createToken = createToken;
    $scope.revokeToken = revokeToken;
    $scope.deleteToken = deleteToken;
    $scope.rotateToken = rotateToken;
    $scope.resetForm = resetForm;
    $scope.showTokenDetail = showTokenDetail;
    $scope.addSelectedApp = addSelectedApp;
    $scope.removeSelectedApp = removeSelectedApp;
    $scope.addAllApps = addAllApps;
    $scope.removeAllApps = removeAllApps;
    $scope.isAppSelected = isAppSelected;
    $scope.appLabel = appLabel;
    $scope.formatOperations = formatOperations;
    $scope.formatStringList = formatStringList;
    $scope.formatNamespaces = formatNamespaces;
    $scope.formatRateLimit = formatRateLimit;

    init();

    function init() {
        UserTokenService.capabilities().then(function (result) {
            $scope.capabilities = result;
            resetForm();
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.LoadCapabilitiesFailed'));
        });
        loadEnvs();
        loadAuthorizedApps();
        loadTokens();
    }

    function loadTokens() {
        UserTokenService.list().then(function (result) {
            $scope.tokens = result || [];
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.LoadFailed'));
        });
    }

    function loadEnvs() {
        EnvService.find_all_envs().then(function (result) {
            $scope.envs = result || [];
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.LoadEnvsFailed'));
        });
    }

    function loadAuthorizedApps() {
        AppService.find_app_by_self(0, 1000).then(function (result) {
            $scope.availableApps = result || [];
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.LoadAppsFailed'));
        });
    }

    function resetForm() {
        $scope.form = {
            name: '',
            expires: defaultExpires(),
            rateLimit: 0,
            namespaces: ''
        };
        $scope.envSelection = {};
        $scope.selectedApps = [];
        $scope.selectedAvailableAppIds = [];
        $scope.selectedSelectedAppIds = [];
        $scope.operationSelection = {};
        ($scope.capabilities.operations || []).forEach(function (operation) {
            $scope.operationSelection[operation] = true;
        });
    }

    function defaultExpires() {
        var days = $scope.capabilities.defaultExpireDays || 90;
        var date = new Date();
        date.setHours(0, 0, 0, 0);
        date.setDate(date.getDate() + days);
        return date;
    }

    function createToken() {
        var request = buildRequest();
        if (!request.name) {
            toastr.warning($translate.instant('UserToken.NameRequired'));
            return;
        }
        if ($scope.form.expires && !request.expires) {
            toastr.warning($translate.instant('UserToken.ExpiresInvalid'));
            return;
        }
        UserTokenService.create(request).then(function (result) {
            $scope.createdTokenValue = result.tokenValue;
            toastr.success($translate.instant('UserToken.Created'));
            loadTokens();
            resetForm();
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.CreateFailed'));
        });
    }

    function revokeToken(token) {
        if (!confirm($translate.instant('UserToken.RevokeConfirm'))) {
            return;
        }
        UserTokenService.revoke(token.id).then(function () {
            toastr.success($translate.instant('UserToken.Revoked'));
            loadTokens();
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.RevokeFailed'));
        });
    }

    function deleteToken(token) {
        if (!confirm($translate.instant('UserToken.DeleteConfirm'))) {
            return;
        }
        UserTokenService.deleteToken(token.id).then(function () {
            toastr.success($translate.instant('UserToken.Deleted'));
            loadTokens();
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.DeleteFailed'));
        });
    }

    function rotateToken(token) {
        if (!confirm($translate.instant('UserToken.RotateConfirm'))) {
            return;
        }
        UserTokenService.rotate(token.id).then(function (result) {
            $scope.createdTokenValue = result.tokenValue;
            toastr.success($translate.instant('UserToken.Rotated'));
            loadTokens();
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.RotateFailed'));
        });
    }

    function showTokenDetail(token) {
        $scope.selectedToken = token || {};
        $('#userTokenDetailModal').modal('show');
    }

    function buildRequest() {
        return {
            name: $scope.form.name,
            operations: selectedOperations(),
            appIds: selectedAppIds(),
            envs: selectedEnvs(),
            namespaces: parseNamespaces($scope.form.namespaces),
            rateLimit: Number($scope.form.rateLimit || 0),
            expires: resolveExpires($scope.form.expires)
        };
    }

    function resolveExpires(expires) {
        var date;
        if (!expires) {
            return null;
        }
        if (expires instanceof Date) {
            date = new Date(expires.getFullYear(), expires.getMonth(), expires.getDate());
        } else if (typeof expires === 'string') {
            var parts = expires.split('-');
            if (parts.length === 3) {
                date = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
            } else {
                date = new Date(expires);
            }
        } else {
            date = new Date(expires);
        }
        if (!date || isNaN(date.getTime())) {
            return null;
        }
        date.setHours(23, 59, 59, 999);
        return date.toISOString();
    }

    function selectedOperations() {
        var selected = [];
        Object.keys($scope.operationSelection).forEach(function (operation) {
            if ($scope.operationSelection[operation]) {
                selected.push(operation);
            }
        });
        return selected;
    }

    function selectedAppIds() {
        return $scope.selectedApps.map(function (app) {
            return getAppId(app);
        }).filter(function (appId) {
            return !!appId;
        });
    }

    function selectedEnvs() {
        return Object.keys($scope.envSelection).filter(function (env) {
            return $scope.envSelection[env];
        });
    }

    function addSelectedApp() {
        if (!$scope.selectedAvailableAppIds || $scope.selectedAvailableAppIds.length === 0) {
            return;
        }
        $scope.selectedAvailableAppIds.forEach(function (appId) {
            var app = findAvailableApp(appId);
            if (app && !isAppSelected(app)) {
                $scope.selectedApps.push(app);
            }
        });
        $scope.selectedAvailableAppIds = [];
    }

    function removeSelectedApp() {
        if (!$scope.selectedSelectedAppIds || $scope.selectedSelectedAppIds.length === 0) {
            return;
        }
        $scope.selectedApps = $scope.selectedApps.filter(function (app) {
            return $scope.selectedSelectedAppIds.indexOf(getAppId(app)) === -1;
        });
        $scope.selectedSelectedAppIds = [];
    }

    function addAllApps() {
        $scope.selectedApps = $scope.availableApps.slice();
        $scope.selectedAvailableAppIds = [];
        $scope.selectedSelectedAppIds = [];
    }

    function removeAllApps() {
        $scope.selectedApps = [];
        $scope.selectedAvailableAppIds = [];
        $scope.selectedSelectedAppIds = [];
    }

    function isAppSelected(app) {
        var appId = getAppId(app);
        return $scope.selectedApps.some(function (selectedApp) {
            return getAppId(selectedApp) === appId;
        });
    }

    function findAvailableApp(appId) {
        for (var i = 0; i < $scope.availableApps.length; i++) {
            if (getAppId($scope.availableApps[i]) === appId) {
                return $scope.availableApps[i];
            }
        }
        return null;
    }

    function appLabel(app) {
        var appId = getAppId(app);
        if (!app) {
            return '';
        }
        if (app.name && app.name !== appId) {
            return appId + ' / ' + app.name;
        }
        return appId;
    }

    function getAppId(app) {
        return app && (app.appId || app.id);
    }

    function parseNamespaces(text) {
        if (!text) {
            return [];
        }
        return text.split('\n').map(function (line) {
            var parts = line.split(',').map(function (item) {
                return item.trim();
            });
            return {
                appId: parts[0] || '',
                env: parts[1] || '',
                clusterName: parts[2] || '',
                namespaceName: parts[3] || ''
            };
        }).filter(function (item) {
            return item.appId || item.env || item.clusterName || item.namespaceName;
        });
    }

    function formatOperations(token) {
        if (!token.operations || token.operations.length === 0) {
            return $translate.instant('UserToken.AllCurrentPermissions');
        }
        return token.operations.join(', ');
    }

    function formatStringList(items) {
        if (!items || items.length === 0) {
            return $translate.instant('UserToken.All');
        }
        return items.join(', ');
    }

    function formatNamespaces(token) {
        var namespaces = token && token.namespaces;
        if (!namespaces || namespaces.length === 0) {
            return $translate.instant('UserToken.All');
        }
        return namespaces.map(function (namespace) {
            return [
                namespace.appId || '*',
                namespace.env || '*',
                namespace.clusterName || '*',
                namespace.namespaceName || '*'
            ].join(', ');
        }).join('\n');
    }

    function formatRateLimit(token) {
        var rateLimit = token && token.rateLimit;
        if (!rateLimit || rateLimit <= 0) {
            return $translate.instant('UserToken.Unlimited');
        }
        return rateLimit;
    }
}
