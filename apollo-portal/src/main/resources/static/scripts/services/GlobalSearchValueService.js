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
appService.service('GlobalSearchValueService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    let global_search_resource = $resource('', {}, {
        get_item_Info_by_key_and_Value: {
            isArray: false,
            method: 'GET',
            url: AppUtil.prefixPath() + '/global-search/item-info/by-key-or-value',
            params: {
                key: 'key',
                value: 'value'
            }
        }
    });
    return {
        findItemInfoByKeyAndValue:function (key,value){
            let d = $q.defer();
            global_search_resource.get_item_Info_by_key_and_Value({key: key,value: value},function (result) {
                d.resolve(result);
            }, function (error) {
                d.reject(error);
            });
            return d.promise;
        }
    }
}]);
