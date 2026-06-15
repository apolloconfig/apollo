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
appService.service('UserTokenService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    const user_token_resource = $resource('', {}, {
        list: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/openapi/v1/user-tokens'
        },
        create: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/user-tokens'
        },
        revoke: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/user-tokens/:tokenId/revoke'
        },
        rotate: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/user-tokens/:tokenId/rotate'
        },
        deleteToken: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/openapi/v1/user-tokens/:tokenId'
        },
        capabilities: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/user-tokens/capabilities'
        },
        adminList: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/openapi/v1/user-tokens/admin'
        },
        adminRevoke: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/user-tokens/admin/:tokenId/revoke'
        },
        adminDeleteToken: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/openapi/v1/user-tokens/admin/:tokenId'
        }
    });

    function promise(action, params, body) {
        var d = $q.defer();
        action(params || {}, body || {},
            function (result) {
                d.resolve(result);
            },
            function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    return {
        list: function () {
            return promise(user_token_resource.list);
        },
        create: function (request) {
            return promise(user_token_resource.create, {}, request);
        },
        revoke: function (tokenId) {
            return promise(user_token_resource.revoke, {tokenId: tokenId});
        },
        deleteToken: function (tokenId) {
            return promise(user_token_resource.deleteToken, {tokenId: tokenId});
        },
        rotate: function (tokenId) {
            return promise(user_token_resource.rotate, {tokenId: tokenId});
        },
        capabilities: function () {
            return promise(user_token_resource.capabilities);
        },
        adminList: function (params) {
            return promise(user_token_resource.adminList, params || {});
        },
        adminRevoke: function (tokenId) {
            return promise(user_token_resource.adminRevoke, {tokenId: tokenId});
        },
        adminDeleteToken: function (tokenId) {
            return promise(user_token_resource.adminDeleteToken, {tokenId: tokenId});
        }
    }
}]);

appService.service('UserTokenFormatterService', ['$translate', function ($translate) {
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

    return {
        tokenStatus: tokenStatus,
        statusLabel: statusLabel,
        statusClass: statusClass,
        formatOperations: formatOperations,
        formatStringList: formatStringList,
        formatNamespaces: formatNamespaces,
        formatRateLimit: formatRateLimit
    };
}]);
