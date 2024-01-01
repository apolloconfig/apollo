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
appService.service('UserService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    const user_resource = $resource('', {}, {
        load_user: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/user'
        },
        find_users: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/users?keyword=:keyword&includeInactiveUsers=:includeInactiveUsers&offset=:offset&limit=:limit'
        },
        change_user_enabled: {
            method: 'PUT',
            url: AppUtil.prefixPath() + '/users/enabled'
        },
        create_or_update_user: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/users?isCreate=:isCreate'
        }
    });
    return {
        load_user: function () {
            var finished = false;
            var d = $q.defer();
            user_resource.load_user({},
                                    function (result) {
                                        finished = true;
                                        d.resolve(result);
                                    },
                                    function (result) {
                                        finished = true;
                                        d.reject(result);
                                    });
            return d.promise;
        },
        find_users: function (keyword, includeInactiveUsers) {
            var d = $q.defer();
            user_resource.find_users({
                                         keyword: keyword,
                                         includeInactiveUsers: includeInactiveUsers
                                     },
                                     function (result) {
                                         d.resolve(result);
                                     },
                                     function (result) {
                                         d.reject(result);
                                     });
            return d.promise;
        },
        change_user_enabled: function (user) {
          var d = $q.defer();
          user_resource.change_user_enabled({}, user,
                                     function (result) {
                                         d.resolve(result)
                                     },
                                     function (result) {
                                         d.reject(result);
                                     });
          return d.promise;
        },
        createOrUpdateUser: function (isCreate, user) {
            var d = $q.defer();
            user_resource.create_or_update_user({
                                     isCreate: isCreate
                                     }, user,
                                     function (result) {
                                         d.resolve(result);
                                     },
                                     function (result) {
                                         d.reject(result);
                                     });
            return d.promise;
        }
    }
}]);
