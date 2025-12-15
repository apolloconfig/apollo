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
config_export_module.controller('ConfigExportController',
                                ['$scope', '$location', '$window', '$http', '$translate', 'toastr', 'AppService',
                                 'EnvService',
                                 'ExportService',
                                 'ClusterService',
                                 'AppUtil',
                                 function ($scope, $location, $window, $http, $translate, toastr, AppService,
                                           EnvService,
                                           ExportService,
                                           ClusterService,
                                           AppUtil) {

                                     $scope.conflictAction = 'ignore';
                                     $scope.cluster = {};
                                     $scope.appConfigBtnDisabled = true;

                                     EnvService.find_all_envs().then(function (result) {
                                         $scope.exportEnvs = [];
                                         $scope.importEnvs = [];
                                         result.forEach(function (env) {
                                             $scope.exportEnvs.push({name: env, checked: false});
                                             $scope.importEnvs.push({name: env, checked: false});

                                         });
                                         $(".apollo-container").removeClass("hidden");
                                     }, function (result) {
                                         toastr.error(AppUtil.errorMsg(result),
                                                      $translate.instant('Cluster.LoadingEnvironmentError'));
                                     });

                                     $scope.switchChecked = function (env, $event) {
                                         env.checked = !env.checked;
                                         $event.stopPropagation();
                                     };

                                     $scope.toggleEnvCheckedStatus = function (env) {
                                         env.checked = !env.checked;
                                     };

                                     $scope.export = function () {
                                          var selectedEnvs = [];
                                          $scope.exportEnvs.forEach(function (env) {
                                              if (env.checked) {
                                                  selectedEnvs.push(env.name);
                                              }
                                          });

                                          if (selectedEnvs.length === 0) {
                                              toastr.warning($translate.instant('Cluster.PleaseChooseEnvironment'));
                                              return;
                                          }

                                          var selectedEnvStr = selectedEnvs.join(",");
                                          $window.location.href = AppUtil.prefixPath() + '/configs/export?envs=' + selectedEnvStr;

                                          toastr.success($translate.instant('ConfigExport.ExportSuccess'));
                                     };

                                     $scope.import = function () {
                                         var selectedEnvs = []
                                         $scope.importEnvs.forEach(function (env) {
                                             if (env.checked) {
                                                 selectedEnvs.push(env.name);
                                             }
                                         });

                                         if (selectedEnvs.length === 0) {
                                             toastr.warning($translate.instant('Cluster.PleaseChooseEnvironment'));
                                             return
                                         }

                                         var selectedEnvStr = selectedEnvs.join(",");
                                         var file = document.getElementById("envFileUpload").files[0];

                                         if (file == null) {
                                             toastr.warning($translate.instant('ConfigExport.UploadFileTip'))
                                             return
                                         }

                                         var form = new FormData();
                                         form.append('file', file);
                                         $http({
                                                   method: 'POST',
                                                   url: AppUtil.prefixPath() + '/configs/import?envs=' + selectedEnvStr + "&conflictAction="
                                                        + $scope.conflictAction,
                                                   data: form,
                                                   headers: {'Content-Type': undefined},
                                                   transformRequest: angular.identity
                                               }).success(function (data) {
                                             toastr.success(data, $translate.instant('ConfigExport.ImportSuccess'))
                                         }).error(function (data) {
                                             toastr.error(data, $translate.instant('ConfigExport.ImportFailed'))
                                         })
                                         toastr.info($translate.instant('ConfigExport.ImportingTip'))
                                     };

                                     $scope.getClusterInfo = function () {
                                         if (!$scope.cluster.appId || !$scope.cluster.env || !$scope.cluster.name) {
                                             $scope.appConfigBtnDisabled = true;
                                             toastr.warning($translate.instant('ConfigExport.PleaseEnterAppIdAndEnvAndCluster'));
                                             return;
                                         }
                                         $scope.cluster.info = "";
                                         ClusterService.load_cluster($scope.cluster.appId, $scope.cluster.env, $scope.cluster.name).then(function (result) {
                                             $scope.cluster.info = $translate.instant('ConfigExport.ClusterInfoContent', {
                                                 appId: result.appId,
                                                 env: $scope.cluster.env,
                                                 clusterName: result.name
                                             });
                                             $scope.appConfigBtnDisabled = false;
                                         }, function (result) {
                                             $scope.appConfigBtnDisabled = true;
                                             AppUtil.showErrorMsg(result);
                                         });
                                     };

                                     $scope.exportAppConfig = function () {
                                         if (!$scope.cluster.appId || !$scope.cluster.env || !$scope.cluster.name || !$scope.cluster.info) {
                                              toastr.warning($translate.instant('ConfigExport.PleaseEnterAppIdAndEnvAndCluster'));
                                              return;
                                         }

                                         var exportUrl = AppUtil.prefixPath() + '/apps/' + $scope.cluster.appId +
                                         '/envs/' + $scope.cluster.env + '/clusters/' + $scope.cluster.name + '/export';

                                         $http({
                                             method: 'HEAD',
                                             url: exportUrl
                                         }).then(function(response) {
                                             $window.location.href = exportUrl;
                                             setTimeout(function() {
                                                 toastr.success($translate.instant('ConfigExport.ExportSuccess'));
                                             }, 1000);
                                         }).catch(function(response) {
                                             if (response.status === 403) {
                                                 toastr.warning($translate.instant('ConfigExport.NoPermissionTip'));
                                                 return;
                                             }
                                             toastr.error($translate.instant('ConfigExport.ExportFailed'));
                                         });
                                     };

                                     $scope.importAppConfig = function () {
                                          if (!$scope.cluster.appId || !$scope.cluster.env || !$scope.cluster.name || !$scope.cluster.info) {
                                               toastr.warning($translate.instant('ConfigExport.PleaseEnterAppIdAndEnvAndCluster'));
                                               return;
                                          }

                                          var file = document.getElementById("appFileUpload").files[0];
                                           if (file == null) {
                                               toastr.warning($translate.instant('ConfigExport.UploadFileTip'));
                                               return;
                                           }

                                           var form = new FormData();
                                           form.append('file', file);
                                           $http({
                                                     method: 'POST',
                                                     url: AppUtil.prefixPath() + '/apps/' + $scope.cluster.appId + '/envs/' + $scope.cluster.env +
                                                     '/clusters/' + $scope.cluster.name + '/import?conflictAction=' + $scope.conflictAction,
                                                     data: form,
                                                     headers: {'Content-Type': undefined},
                                                     transformRequest: angular.identity
                                                 }).success(function (data) {
                                               toastr.success(data, $translate.instant('ConfigExport.ImportSuccess'));
                                           }).error(function (data, status) {
                                               if (status === 403) {
                                                    toastr.warning($translate.instant('ConfigExport.NoPermissionTip'));
                                                    return;
                                               }
                                               toastr.error(data, $translate.instant('ConfigExport.ImportFailed'));
                                           });
                                           toastr.info($translate.instant('ConfigExport.ImportingTip'));
                                      };
                                 }]);
