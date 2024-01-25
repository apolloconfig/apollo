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
appService.service('ExportService', ['$resource', '$q', function ($resource, $q) {
    var resource = $resource('', {}, {
        importConfig: {
            method: 'POST',
            url: '/import',
            headers: {'Content-Type': undefined},
        }
    });
    return {
        importConfig: function (envs, file) {
            var form = new FormData();
            form.append('file', file);
            var d = $q.defer();
            resource.importConfig({
                data: form,
                envs: envs,
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
