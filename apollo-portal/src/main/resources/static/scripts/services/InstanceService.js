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
appService.service('InstanceService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    var resource = $resource('', {}, {
        find_instances_by_release: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/envs/:env/instances/by-release'
        },
        find_instances_by_namespace: {
            method: 'GET',
            isArray: false,
            url: AppUtil.prefixPath() + '/envs/:env/instances/by-namespace'
        },
        find_by_releases_not_in: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/envs/:env/instances/by-namespace-and-releases-not-in'
        },
        get_instance_count_by_namespace: {
            method: 'GET',
            isArray: false,
            url: AppUtil.prefixPath() + "/envs/:env/instances/by-namespace/count"
        }
    });

    var instanceService = {
        findInstancesByRelease: function (env, releaseId, page, size) {
            if (!size) {
                size = 20;
            }
            var d = $q.defer();
            resource.find_instances_by_release({
                                                   env: env,
                                                   releaseId: releaseId,
                                                   page: page,
                                                   size: size
                                               },
                                               function (result) {
                                                   d.resolve(result);
                                               }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        },
        findInstancesByNamespace: function (appId, env, clusterName, namespaceName, instanceAppId, page, size) {
            if (!size) {
                size = 20;
            }
            var d = $q.defer();
            var instanceAppIdRequest = instanceAppId;
            instanceService.lastInstanceAppIdRequest = instanceAppIdRequest;
            resource.find_instances_by_namespace({
                                                     env: env,
                                                     appId: appId,
                                                     clusterName: clusterName,
                                                     namespaceName: namespaceName,
                                                     instanceAppId: instanceAppId,
                                                     page: page,
                                                     size: size
                                                 },
                                                 function (result) {
                                                     if (instanceAppIdRequest
                                                         != instanceService.lastInstanceAppIdRequest) {
                                                         return;
                                                     }
                                                     d.resolve(result);
                                                 }, function (result) {
                    if (instanceAppIdRequest != instanceService.lastInstanceAppIdRequest) {
                        return;
                    }
                    d.reject(result);
                });
            return d.promise;
        },
        findByReleasesNotIn: function (appId, env, clusterName, namespaceName, releaseIds) {
            var d = $q.defer();
            resource.find_by_releases_not_in({
                                                 env: env,
                                                 appId: appId,
                                                 clusterName: clusterName,
                                                 namespaceName: namespaceName,
                                                 releaseIds: releaseIds
                                             },
                                             function (result) {
                                                 d.resolve(result);
                                             }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        },
        getInstanceCountByNamespace: function (appId, env, clusterName, namespaceName) {
            var d = $q.defer();
            resource.get_instance_count_by_namespace({
                                                         env: env,
                                                         appId: appId,
                                                         clusterName: clusterName,
                                                         namespaceName: namespaceName
                                                     },
                                                     function (result) {
                                                         d.resolve(result);
                                                     }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        }

    };

    return instanceService;
}]);
