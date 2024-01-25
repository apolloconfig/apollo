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
appService.service('AppService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    var app_resource = $resource(AppUtil.prefixPath() + '/apps/:appId', {}, {
        find_apps: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/apps'
        },
        find_app_by_owner: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/apps/by-owner'
        },
        load_navtree: {
            method: 'GET',
            isArray: false,
            url: AppUtil.prefixPath() + '/apps/:appId/navtree'
        },
        load_app: {
            method: 'GET',
            isArray: false
        },
        create_app: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/apps'
        },
        update_app: {
            method: 'PUT',
            url: AppUtil.prefixPath() + '/apps/:appId'
        },
        create_app_remote: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/apps/envs/:env'
        },
        find_miss_envs: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/apps/:appId/miss_envs'
        },
        create_missing_namespaces: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/apps/:appId/envs/:env/clusters/:clusterName/missing-namespaces'
        },
        find_missing_namespaces: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/apps/:appId/envs/:env/clusters/:clusterName/missing-namespaces'
        },
        delete_app: {
            method: 'DELETE',
            isArray: false
        },
        allow_app_master_assign_role: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/apps/:appId/system/master/:userId'
        },
        delete_app_master_assign_role: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/apps/:appId/system/master/:userId'
        },
        has_create_application_role: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/system/role/createApplication/:userId'
        }
    });
    return {
        find_apps: function (appIds) {
            if (!appIds) {
                appIds = '';
            }
            var d = $q.defer();
            app_resource.find_apps({appIds: appIds}, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        find_app_by_owner: function (owner, page, size) {
            var d = $q.defer();
            app_resource.find_app_by_owner({
                                               owner: owner,
                                               page: page,
                                               size: size
                                           }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        load_nav_tree: function (appId) {
            var d = $q.defer();
            app_resource.load_navtree({
                                          appId: appId
                                      }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        create: function (app) {
            var d = $q.defer();
            app_resource.create_app({}, app, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        update: function (app) {
            var d = $q.defer();
            app_resource.update_app({
                                        appId: app.appId
                                    }, app, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        create_remote: function (env, app) {
            var d = $q.defer();
            app_resource.create_app_remote({env: env}, app, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        load: function (appId) {
            var d = $q.defer();
            app_resource.load_app({
                                      appId: appId
                                  }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        find_miss_envs: function (appId) {
            var d = $q.defer();
            app_resource.find_miss_envs({
                                            appId: appId
                                        }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        create_missing_namespaces: function (appId, env, clusterName) {
            var d = $q.defer();
            app_resource.create_missing_namespaces({
                                            appId: appId,
                                            env: env,
                                            clusterName: clusterName
                                        }, null, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        find_missing_namespaces: function (appId, env, clusterName) {
            var d = $q.defer();
            app_resource.find_missing_namespaces({
                                            appId: appId,
                                            env: env,
                                            clusterName: clusterName
                                        }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        delete_app: function (appId) {
            var d = $q.defer();
            app_resource.delete_app({
                appId: appId
            }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        allow_app_master_assign_role: function (appId, userId) {
            var d = $q.defer();
            app_resource.allow_app_master_assign_role({
                appId: appId,
                userId: userId
            }, null, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        delete_app_master_assign_role: function (appId, userId) {
            var d = $q.defer();
            app_resource.delete_app_master_assign_role({
                appId: appId,
                userId: userId
            }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        has_create_application_role: function (userId) {
            var d = $q.defer();
            app_resource.has_create_application_role({
                userId: userId
            }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        }
    }
}]);
