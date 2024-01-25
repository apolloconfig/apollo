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
appService.service('SystemInfoService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    var system_info_resource = $resource('', {}, {
        load_system_info: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/system-info'
        },
        check_health: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/system-info/health'
        }
    });
    return {
        load_system_info: function () {
            var d = $q.defer();
            system_info_resource.load_system_info({},
            function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        check_health: function (instanceId, host) {
            var d = $q.defer();
            system_info_resource.check_health({
                  instanceId: instanceId
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
