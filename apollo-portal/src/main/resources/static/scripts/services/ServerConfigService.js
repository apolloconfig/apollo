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
appService.service('ServerConfigService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    let server_config_resource = $resource('', {}, {
        create_portal_db_config: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/server/portal-db/config'
        },
        create_config_db_config: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/server/envs/:env/config-db/config'
        },
        find_portal_db_config: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath()
                + '/server/portal-db/config/find-all-config'
        },
        find_config_db_config: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath()
                + '/server/envs/:env/config-db/config/find-all-config'
        }
    });
    return {
        createPortalDBConfig: function (serverConfig) {
            let d = $q.defer();
            server_config_resource.create_portal_db_config({}, serverConfig, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        createConfigDBConfig: function (env, serverConfig) {
            let d = $q.defer();
            server_config_resource.create_config_db_config({env:env}, serverConfig, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        findPortalDBConfig:function (){
            let d = $q.defer();
            server_config_resource.find_portal_db_config({
            }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        findConfigDBConfig:function (env){
            let d = $q.defer();
            server_config_resource.find_config_db_config({env: env}, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        }
    }
}]);
