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
appService.service('AccessKeyService', ['$resource', '$q', 'AppUtil', 'UserService', function ($resource, $q, AppUtil, UserService) {
    var access_key_resource = $resource('', {}, {
        load_access_keys: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/accesskeys'
        },
        create_access_key: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/accesskeys'
        },
        remove_access_key: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/accesskeys/:id'
        },
        enable_access_key: {
            method: 'PUT',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/accesskeys/:id/activation'
        },
        disable_access_key: {
            method: 'PUT',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/accesskeys/:id/deactivation'
        }
    });

    var current_user_promise;

    function loadCurrentUserId() {
        if (!current_user_promise) {
            current_user_promise = UserService.load_user().then(function (user) {
                return user.userId;
            }, function (result) {
                current_user_promise = null;
                return $q.reject(result);
            });
        }
        return current_user_promise;
    }

    function resolveOperator(operator) {
        if (operator) {
            return $q.when(operator);
        }
        return loadCurrentUserId();
    }

    function withOperator(operator, callback) {
        return resolveOperator(operator).then(callback);
    }

    return {
        load_access_keys: function (appId, env) {
            var d = $q.defer();
            access_key_resource.load_access_keys({
                    appId: appId,
                    env: env
                },
                function (result) {
                    d.resolve(result);
                }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        },
        create_access_key: function (appId, env, user) {
            var d = $q.defer();
            withOperator(user, function (operator) {
                access_key_resource.create_access_key({
                    appId: appId,
                    env: env,
                    operator: operator
                }, {},
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }).catch(function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        remove_access_key: function (appId, env, id) {
            var d = $q.defer();
            withOperator(null, function (operator) {
                access_key_resource.remove_access_key({
                    appId: appId,
                    env: env,
                    id: id,
                    operator: operator
                },
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }).catch(function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        enable_access_key: function (appId, env, id, mode) {
            var d = $q.defer();
            withOperator(null, function (operator) {
                access_key_resource.enable_access_key({
                    appId: appId,
                    env: env,
                    id: id,
                    mode: mode,
                    operator: operator
                }, {},
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }).catch(function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        disable_access_key: function (appId, env, id) {
            var d = $q.defer();
            withOperator(null, function (operator) {
                access_key_resource.disable_access_key({
                    appId: appId,
                    env: env,
                    id: id,
                    operator: operator
                }, {},
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }).catch(function (result) {
                d.reject(result);
            });
            return d.promise;
        }
    }
}]);
