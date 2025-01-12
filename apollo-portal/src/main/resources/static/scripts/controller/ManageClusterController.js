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
manage_cluster_module.controller('ManageClusterController',
    ['$scope', '$location', '$window', '$translate', 'toastr', 'AppService', 'EnvService', 'ClusterService',
      'AppUtil',
      function ($scope, $location, $window, $translate, toastr, AppService, EnvService, ClusterService,
          AppUtil) {

        var params = AppUtil.parseParams($location.$$url);
        $scope.appId = params.appid;

        $scope.envs = [];

        function loadClusters() {
          AppService.load_nav_tree($scope.appId).then(function (result) {
            var nodes = AppUtil.collectData(result);
            if (!nodes || nodes.length == 0) {
              toastr.error($translate.instant('Config.SystemError'));
              return;
            }
            nodes.forEach(function (node) {
              $scope.envs.push({ name: node.env, clusters: node.clusters });
            });
            console.log($scope.envs);
          });

        }

        loadClusters();

      }
    ]
);
