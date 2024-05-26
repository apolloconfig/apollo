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
directive_module.directive('importnamespacemodal', importNamespaceModalDirective);

function importNamespaceModalDirective($window, $q, $translate, $http, toastr, AppUtil, EventManager,
                                       PermissionService, UserService, NamespaceService) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/import-namespace-modal.html',
        transclude: true,
        replace: true,
        scope: {
            env: '='
        },
        link: function (scope) {

            scope.doImportNamespace = doImportNamespace;

            EventManager.subscribe(EventManager.EventType.PRE_IMPORT_NAMESPACE, function (context) {
                scope.toImportNamespace = context.namespace;

                showImportNamespaceConfirmDialog();

            });

            function showImportNamespaceConfirmDialog() {
                AppUtil.showModal('#importNamespaceModal');
            }

            function doImportNamespace() {
                var file = document.getElementById("fileUpload").files[0];

                if (file == null) {
                    toastr.warning($translate.instant('ConfigExport.UploadFileTip'))
                    return
                }

                var toImportNamespace = scope.toImportNamespace;
                var form = new FormData();
                form.append('file', file);
                $http({
                          method: 'POST',
                          url: AppUtil.prefixPath() + '/apps/' + toImportNamespace.baseInfo.appId + '/envs/' + scope.env + '/clusters/'
                               + toImportNamespace.baseInfo.clusterName
                               + '/namespaces/' + toImportNamespace.baseInfo.namespaceName + "/items/import",
                          data: form,
                          headers: {'Content-Type': undefined},
                          transformRequest: angular.identity
                      }).success(function (data) {
                    toastr.success(data, $translate.instant('ConfigExport.ImportSuccess'))

                    //refresh namespace
                    EventManager.emit(EventManager.EventType.REFRESH_NAMESPACE,
                                      {
                                          namespace: toImportNamespace
                                      });
                }).error(function (data) {
                    toastr.error(data, $translate.instant('ConfigExport.ImportFailed'))
                })
            }

        }
    }
}



