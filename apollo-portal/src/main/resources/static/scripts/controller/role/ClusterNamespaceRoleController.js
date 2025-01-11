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
role_module.controller('ClusterNamespaceRoleController',
    ['$scope', '$location', '$window', '$translate', 'toastr', 'AppService', 'UserService', 'AppUtil', 'EnvService',
      'PermissionService',
      function ($scope, $location, $window, $translate, toastr, AppService, UserService, AppUtil, EnvService,
          PermissionService) {

        var params = AppUtil.parseParams($location.$$url);
        $scope.pageContext = {
          appId: params.appid,
          env: params.env,
          clusterName: params.clusterName
        };

        $scope.modifyRoleSubmitBtnDisabled = false;
        $scope.releaseRoleSubmitBtnDisabled = false;

        $scope.releaseRoleWidgetId = 'releaseRoleWidgetId';
        $scope.modifyRoleWidgetId = 'modifyRoleWidgetId';

        PermissionService.init_cluster_ns_permission($scope.pageContext.appId, $scope.pageContext.env, $scope.pageContext.clusterName)
        .then(function (result) {

        }, function (result) {
          toastr.warning(AppUtil.errorMsg(result), $translate.instant('Cluster.Role.InitClusterPermissionError'));
        });

        PermissionService.has_assign_user_permission($scope.pageContext.appId)
        .then(function (result) {
          $scope.hasAssignUserPermission = result.hasPermission;
        }, function (result) {

        });

        PermissionService.get_cluster_ns_role_users($scope.pageContext.appId,
            $scope.pageContext.env, $scope.pageContext.clusterName)
        .then(function (result) {
          $scope.rolesAssignedUsers = result;
        }, function (result) {
          toastr.error(AppUtil.errorMsg(result), $translate.instant('Cluster.Role.GetGrantUserError'));
        });

        $scope.assignRoleToUser = function (roleType) {
          if ("ReleaseNamespacesInCluster" === roleType) {
            var user = $('.' + $scope.releaseRoleWidgetId).select2('data')[0];
            if (!user) {
              toastr.warning($translate.instant('Cluster.Role.PleaseChooseUser'));
              return;
            }
            $scope.releaseRoleSubmitBtnDisabled = true;
            var toAssignReleaseNamespacesInClusterRoleUser = user.id;

            var assignReleaseNamespacesInClusterRoleFunc = function (appId, env, clusterName, user) {
              return PermissionService.assign_release_cluster_ns_role(appId, env, clusterName, user);
            };

            assignReleaseNamespacesInClusterRoleFunc(
                $scope.pageContext.appId,
                $scope.pageContext.env,
                $scope.pageContext.clusterName,
                toAssignReleaseNamespacesInClusterRoleUser
            ).then(function () {
              toastr.success($translate.instant('Cluster.Role.Added'));
              $scope.releaseRoleSubmitBtnDisabled = false;
              $scope.rolesAssignedUsers.releaseRoleUsers.push({ userId: toAssignReleaseNamespacesInClusterRoleUser });

              $('.' + $scope.releaseRoleWidgetId).select2("val", "");
            }, function (result) {
              $scope.releaseRoleSubmitBtnDisabled = false;
              toastr.error(AppUtil.errorMsg(result), $translate.instant('Cluster.Role.AddFailed'));
            });
          } else if ("ModifyNamespacesInCluster" === roleType) {
            var user1 = $('.' + $scope.modifyRoleWidgetId).select2('data')[0];
            if (!user1) {
              toastr.warning($translate.instant('Cluster.Role.PleaseChooseUser'));
              return;
            }
            $scope.modifyRoleSubmitBtnDisabled = true;
            var toAssignModifyNamespacesInClusterRoleUser = user1.id;

            var assignModifyNamespacesInClusterRoleFunc = function (appId, env, clusterName, user) {
              return PermissionService.assign_modify_cluster_ns_role(appId, env, clusterName, user);
            };

            assignModifyNamespacesInClusterRoleFunc(
                $scope.pageContext.appId,
                $scope.pageContext.env,
                $scope.pageContext.clusterName,
                toAssignModifyNamespacesInClusterRoleUser
            ).then(function () {
              toastr.success($translate.instant('Cluster.Role.Added'));
              $scope.modifyRoleSubmitBtnDisabled = false;
              $scope.rolesAssignedUsers.modifyRoleUsers.push({ userId: toAssignModifyNamespacesInClusterRoleUser });

              $('.' + $scope.modifyRoleWidgetId).select2("val", "");
            }, function (result) {
              $scope.modifyRoleSubmitBtnDisabled = false;
              toastr.error(AppUtil.errorMsg(result), $translate.instant('Cluster.Role.AddFailed'));
            });
          }
        };

        $scope.removeUserRole = function (roleType, user) {
          if ("ReleaseNamespacesInCluster" === roleType) {
            var removeReleaseNamespacesInClusterRoleFunc = function (appId, env, clusterName, user) {
              return PermissionService.remove_release_cluster_ns_role(appId, env, clusterName, user);
            };
            removeReleaseNamespacesInClusterRoleFunc(
                $scope.pageContext.appId,
                $scope.pageContext.env,
                $scope.pageContext.clusterName,
                user
            ).then(function () {
              toastr.success($translate.instant('Cluster.Role.Deleted'));
              removeUserFromList($scope.rolesAssignedUsers.releaseRoleUsers, user);
            }, function (result) {
              toastr.error(AppUtil.errorMsg(result), $translate.instant('Namespace.Role.DeleteFailed'));
            });
          } else if ("ModifyNamespacesInCluster" === roleType) {
            var removeModifyNamespacesInClusterRoleFunc = function (appId, env, clusterName, user) {
              return PermissionService.remove_modify_cluster_ns_role(appId, env, clusterName, user);
            };

            removeModifyNamespacesInClusterRoleFunc(
                $scope.pageContext.appId,
                $scope.pageContext.env,
                $scope.pageContext.clusterName,
                user
            ).then(function () {
              toastr.success($translate.instant('Cluster.Role.Deleted'));
              removeUserFromList($scope.rolesAssignedUsers.modifyRoleUsers, user);
            }, function (result) {
              toastr.error(AppUtil.errorMsg(result), $translate.instant('Cluster.Role.DeleteFailed'));
            });
          }
        };

        function removeUserFromList(list, user) {
          var index = 0;
          for (var i = 0; i < list.length; i++) {
            if (list[i].userId === user) {
              index = i;
              break;
            }
          }
          list.splice(index, 1);
        }



      }]);
