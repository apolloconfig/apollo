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
appService.service('EnvService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    var env_resource = $resource(AppUtil.prefixPath() + '/envs', {}, {
        find_all_envs:{
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/envs'
        }
    });
    return {
        find_all_envs: function () {
            var d = $q.defer();
            env_resource.find_all_envs({
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
