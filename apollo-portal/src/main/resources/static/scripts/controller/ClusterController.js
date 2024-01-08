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
cluster_module.controller('ClusterController',
    ['$scope', '$location', '$window', '$translate', 'toastr', 'AppService', 'EnvService', 'ClusterService',
        'AppUtil',
        function ($scope, $location, $window, $translate, toastr, AppService, EnvService, ClusterService,
            AppUtil) {

            var params = AppUtil.parseParams($location.$$url);
            $scope.appId = params.appid;

            $scope.step = 1;

            $scope.submitBtnDisabled = false;

            EnvService.find_all_envs().then(function (result) {
                $scope.envs = [];
                result.forEach(function (env) {
                    $scope.envs.push({ name: env, checked: false });

                });
                $(".apollo-container").removeClass("hidden");
            }, function (result) {
                toastr.error(AppUtil.errorMsg(result), $translate.instant('Cluster.LoadingEnvironmentError'));
            });

            $scope.clusterName = '';
            $scope.clusterComment = '';

            $scope.switchChecked = function (env, $event) {
                env.checked = !env.checked;
                $event.stopPropagation();
            };

            $scope.toggleEnvCheckedStatus = function (env) {
                env.checked = !env.checked;
            };

            $scope.create = function () {

                var noEnvChecked = true;
                $scope.envs.forEach(function (env) {
                    if (env.checked) {
                        noEnvChecked = false;
                        $scope.submitBtnDisabled = true;
                        ClusterService.create_cluster($scope.appId, env.name,
                            {
                                name: $scope.clusterName,
                                comment: $scope.clusterComment,
                                appId: $scope.appId
                            }).then(function (result) {
                                toastr.success(env.name, $translate.instant('Cluster.ClusterCreated'));
                                $scope.step = 2;
                                $scope.submitBtnDisabled = false;
                            }, function (result) {
                                toastr.error(AppUtil.errorMsg(result), $translate.instant('Cluster.ClusterCreateFailed'));
                                $scope.submitBtnDisabled = false;
                            })
                    }
                });

                if (noEnvChecked) {
                    toastr.warning($translate.instant('Cluster.PleaseChooseEnvironment'));
                }

            };

        }]);
