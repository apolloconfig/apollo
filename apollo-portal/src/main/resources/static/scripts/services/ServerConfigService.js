/*
 * Copyright 2022 Apollo Authors
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
appService.service('ServerConfigService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    var server_config_resource = $resource('', {}, {
        create_server_config: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/server/config'
        },
        get_server_config_info: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/server/config/:key'
        },
        find_portalDBConfig: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/server/config/findAll?offset=:offset&limit=:limit'
        }
    });
    return {
        create: function (serverConfig) {
            var d = $q.defer();
            server_config_resource.create_server_config({}, serverConfig, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        getServerConfigInfo: function (key) {
            var d = $q.defer();
            server_config_resource.get_server_config_info({
                key: key
            }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        find_portalDBConfig:function (offset, limit){
            var d = $q.defer();
            server_config_resource.find_portalDBConfig({
                offset: offset,
                limit: limit
            }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        }
    }
}]);
