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
user_token_module.controller('AdminUserTokenController',
    ['$scope', '$translate', 'toastr', 'AppUtil', 'PermissionService', 'UserTokenService',
        AdminUserTokenController]);

function AdminUserTokenController($scope, $translate, toastr, AppUtil, PermissionService,
                                  UserTokenService) {
    $scope.tokens = [];
    $scope.filters = {
        userId: '',
        status: 'all'
    };
    $scope.selectedToken = {};
    $scope.isRootUser = false;

    $scope.loadTokens = loadTokens;
    $scope.resetFilters = resetFilters;
    $scope.showTokenDetail = showTokenDetail;
    $scope.revokeToken = revokeToken;
    $scope.deleteToken = deleteToken;
    $scope.tokenStatus = tokenStatus;
    $scope.statusLabel = statusLabel;
    $scope.statusClass = statusClass;
    $scope.formatOperations = formatOperations;
    $scope.formatStringList = formatStringList;
    $scope.formatNamespaces = formatNamespaces;
    $scope.formatRateLimit = formatRateLimit;

    init();

    function init() {
        PermissionService.has_root_permission().then(function (result) {
            $scope.isRootUser = result.hasPermission;
            if ($scope.isRootUser) {
                loadTokens();
            }
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.AdminLoadFailed'));
        });
    }

    function loadTokens() {
        var params = {
            status: $scope.filters.status || 'all'
        };
        if ($scope.filters.userId) {
            params.userId = $scope.filters.userId;
        }
        UserTokenService.adminList(params).then(function (result) {
            $scope.tokens = result || [];
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.AdminLoadFailed'));
        });
    }

    function resetFilters() {
        $scope.filters = {
            userId: '',
            status: 'all'
        };
        loadTokens();
    }

    function showTokenDetail(token) {
        $scope.selectedToken = token || {};
        $('#adminUserTokenDetailModal').modal('show');
    }

    function revokeToken(token) {
        if (!confirm($translate.instant('UserToken.AdminRevokeConfirm',
            {userId: token.userId, prefix: token.tokenPrefix}))) {
            return;
        }
        UserTokenService.adminRevoke(token.id).then(function () {
            toastr.success($translate.instant('UserToken.Revoked'));
            loadTokens();
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.AdminRevokeFailed'));
        });
    }

    function deleteToken(token) {
        if (!confirm($translate.instant('UserToken.AdminDeleteConfirm',
            {userId: token.userId, prefix: token.tokenPrefix}))) {
            return;
        }
        UserTokenService.adminDeleteToken(token.id).then(function () {
            toastr.success($translate.instant('UserToken.Deleted'));
            if ($scope.selectedToken && $scope.selectedToken.id === token.id) {
                $('#adminUserTokenDetailModal').modal('hide');
            }
            loadTokens();
        }, function (result) {
            AppUtil.showErrorMsg(result, $translate.instant('UserToken.AdminDeleteFailed'));
        });
    }

    function tokenStatus(token) {
        if (!token) {
            return '';
        }
        if (token.status) {
            return token.status;
        }
        if (token.revokedAt) {
            return 'revoked';
        }
        if (token.expires && new Date(token.expires).getTime() <= new Date().getTime()) {
            return 'expired';
        }
        return 'active';
    }

    function statusLabel(token) {
        var status = tokenStatus(token);
        if (status === 'revoked') {
            return $translate.instant('UserToken.RevokedStatus');
        }
        if (status === 'expired') {
            return $translate.instant('UserToken.ExpiredStatus');
        }
        return $translate.instant('UserToken.Active');
    }

    function statusClass(token) {
        var status = tokenStatus(token);
        if (status === 'revoked') {
            return 'label-default';
        }
        if (status === 'expired') {
            return 'label-warning';
        }
        return 'label-primary';
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
