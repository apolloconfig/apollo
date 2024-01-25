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
appService.service('ClusterService', ['$resource', '$q', function ($resource, $q) {
    var cluster_resource = $resource('', {}, {
        create_cluster: {
            method: 'POST',
            url: 'apps/:appId/envs/:env/clusters'
        },
        load_cluster: {
            method: 'GET',
            url: 'apps/:appId/envs/:env/clusters/:clusterName'
        },
        delete_cluster: {
            method: 'DELETE',
            url: 'apps/:appId/envs/:env/clusters/:clusterName'
        }
    });
    return {
        create_cluster: function (appId, env, cluster) {
            var d = $q.defer();
            cluster_resource.create_cluster({
                                                appId: appId,
                                                env: env
                                            }, cluster,
                                            function (result) {
                                                d.resolve(result);
                                            }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        },
        load_cluster: function (appId, env, clusterName) {
            var d = $q.defer();
            cluster_resource.load_cluster({
                appId: appId,
                env: env,
                clusterName: clusterName
            },
            function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        delete_cluster: function (appId, env, clusterName) {
            var d = $q.defer();
            cluster_resource.delete_cluster({
                appId: appId,
                env: env,
                clusterName: clusterName
            },
            function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        }
    }
}]);
