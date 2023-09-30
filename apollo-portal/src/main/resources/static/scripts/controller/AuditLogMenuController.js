/*
 * Copyright 2023 Apollo Authors
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
audit_log_menu_module.controller('AuditLogMenuController',
    ['$scope', '$window', '$translate', 'toastr', 'AppService', 'AppUtil', 'EventManager', 'AuditLogService',
      auditLogMenuController]
);

function auditLogMenuController($scope, $window, $translate, toastr, AppService, AppUtil, EventManager, AuditLogService) {

      $scope.auditLogList = [];
      $scope.goToTraceDetailsPage = goToTraceDetailsPage;
      $scope.searchByOpNameAndDate = searchByOpNameAndDate;
      $scope.getMoreAuditLogs = getMoreAuditLogs;
      $scope.formatDate = formatDate;

      $scope.page = 0;
      var PAGE_SIZE = 10;

      $scope.opName = '';
      $scope.startDate = null;
      $scope.endDate = null;

      $scope.hasLoadAll = false;

      init();

      function init() {
            initSearchingMenu();
      }

      function initSearchingMenu() {
            AuditLogService.find_all_logs($scope.page, PAGE_SIZE).then(function (result) {
                  if (!result || result.length < PAGE_SIZE) {
                        $scope.hasLoadAll = true;
                  }
                  if (result.length === 0) {
                        return;
                  }
                  $scope.auditLogList = $scope.auditLogList.concat(result);
            });
      }

      function searchByOpNameAndDate(opName, startDate, endDate) {
            $scope.auditLogList = [];
            $scope.page = 0;
            $scope.opName = opName;
            $scope.startDate = startDate;
            $scope.endDate = endDate;
            AuditLogService.find_logs_by_opName(
                $scope.opName,
                $scope.startDate,
                $scope.endDate,
                $scope.page,
                PAGE_SIZE
            ).then(function (result) {
                  if (!result || result.length < PAGE_SIZE) {
                        $scope.hasLoadAll = true;
                  }
                  if (result.length === 0) {
                        return;
                  }
                  $scope.auditLogList = $scope.auditLogList.concat(result);
            });
      }

      function getMoreAuditLogs() {
            $scope.page = $scope.page + 1;
            if($scope.opName === '') {
                  AuditLogService.find_all_logs($scope.page, PAGE_SIZE).then(function (result) {
                        if (!result || result.length < PAGE_SIZE) {
                              $scope.hasLoadAll = true;
                        }
                        if (result.length === 0) {
                              return;
                        }
                        $scope.auditLogList = $scope.auditLogList.concat(result);
                  });
            }else {
                  AuditLogService.find_logs_by_opName(
                      $scope.opName,
                      $scope.startTime,
                      $scope.endTime,
                      $scope.page,
                      PAGE_SIZE
                  ).then(function (result) {
                        if (!result || result.length < PAGE_SIZE) {
                              $scope.hasLoadAll = true;
                        }
                        if (result.length === 0) {
                              return;
                        }
                        $scope.auditLogList = $scope.auditLogList.concat(result);
                  });
            }
      }

      function goToTraceDetailsPage(traceId) {
            $window.location.href =  AppUtil.prefixPath() + "/trace_detail.html?#traceId=" + traceId;
      }

      function formatDate(dateStr) {
            var date = new Date(dateStr);
            const year = date.getFullYear();
            const month = (date.getMonth() + 1).toString().padStart(2, '0');
            const day = date.getDate().toString().padStart(2, '0');
            const hour = date.getHours().toString().padStart(2, '0');
            const minute = date.getMinutes().toString().padStart(2, '0');
            const second = date.getSeconds().toString().padStart(2, '0');
            return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
      }

}



