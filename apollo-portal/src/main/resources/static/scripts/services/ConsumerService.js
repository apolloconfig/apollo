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
appService.service('ConsumerService', ['$resource', '$q', 'AppUtil', 
                                       function ($resource, $q, AppUtil) {
    var resource = $resource('', {}, {
        create_consumer: {
            method: 'POST',
            isArray: false,
            url: AppUtil.prefixPath() + '/consumers'
        },
        get_consumer_token_by_appId: {
            method: 'GET',
            isArray: false,
            url: AppUtil.prefixPath() + '/consumer-tokens/by-appId'
        },
        assign_role_to_consumer: {
            method: 'POST',
            isArray: true,
            url: AppUtil.prefixPath() + '/consumers/:token/assign-role'
        },
        get_consumer_list: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/consumers'
        },
        delete_consumer: {
            method: 'DELETE',
            isArray: false,
            url: AppUtil.prefixPath() + '/consumers/by-appId'
        }

    });

                                           
    return {
        createConsumer: function (consumer) {
            return AppUtil.ajax(resource.create_consumer, {}, consumer);
        },
        getConsumerTokenByAppId: function (appId) {
            return AppUtil.ajax(resource.get_consumer_token_by_appId, {
                appId: appId
            });
        },
        assignRoleToConsumer: function (token, type, appId, namespaceName, envs) {
            return AppUtil.ajax(resource.assign_role_to_consumer, 
                                {
                                    token: token,
                                    type: type,
                                    envs: envs
                                },
                                {
                                    appId: appId,
                                    namespaceName: namespaceName
                                }
            )
        },
        getConsumerList: function (page, size){
            return AppUtil.ajax(resource.get_consumer_list,
                {
                    page: page,
                    size: size
                }
            )
        },
        deleteConsumer: function (appId){
            return AppUtil.ajax(resource.delete_consumer,
                {
                    appId: appId
                }
            )
        }
    }
}]);
