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
appService.service('ReleaseHistoryService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    var resource = $resource('', {}, {
        find_release_history_by_namespace: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/releases/histories',
            isArray: true
        }
    });

    function findReleaseHistoryByNamespace(appId, env, clusterName, namespaceName, page, size) {
        var d = $q.defer();
        resource.find_release_history_by_namespace({
                                                       appId: appId,
                                                       env: env,
                                                       clusterName: clusterName,
                                                       namespaceName: namespaceName,
                                                       page: page,
                                                       size: size
                                                   }, function (result) {
            d.resolve(result);
        }, function (result) {
            d.reject(result);
        });
        return d.promise;
    }

    
    return {
        findReleaseHistoryByNamespace: findReleaseHistoryByNamespace
    }
}]);
