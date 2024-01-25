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
appService.service('NamespaceLockService', ['$resource', '$q', function ($resource, $q) {
    var resource = $resource('', {}, {
        get_namespace_lock: {
            method: 'GET',
            url: 'apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/lock-info'
        }
    });

    return {
        get_namespace_lock: function (appId, env, clusterName, namespaceName) {
            var d = $q.defer();
            resource.get_namespace_lock({
                                            appId: appId,
                                            env: env,
                                            clusterName: clusterName,
                                            namespaceName: namespaceName
                                        }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        }
    }
}]);
