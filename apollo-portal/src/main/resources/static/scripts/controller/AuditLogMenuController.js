audit_log_menu_module.controller('AuditLogMenuController',
    ['$scope', '$window', '$translate', 'toastr', 'AppService', 'AppUtil', 'EventManager', 'AuditLogService',
      auditLogMenuController]
)

function auditLogMenuController($scope, $window, $translate, toastr, AppService, AppUtil, EventManager, AuditLogService) {

      $scope.auditLogList = [];
      $scope.goToTraceDetailsPage = goToTraceDetailsPage;
      $scope.searchByOpNameAndDate = searchByOpNameAndDate;
      $scope.getMoreAuditLogs = getMoreAuditLogs;
      $scope.formatDate = formatDate;

      $scope.page = 0;
      let PAGE_SIZE = 10;

      $scope.opName = ''
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
                  $scope.auditLogList = $scope.auditLogList.concat(result)
            })
      }

      function searchByOpNameAndDate(opName, startDate, endDate) {
            $scope.auditLogList = [];
            $scope.page = 0;
            $scope.opName = opName;
            $scope.startDate = formatDate(startDate);
            $scope.endDate = formatDate(endDate);
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
                        $scope.auditLogList = $scope.auditLogList.concat(result)
                  })
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
            console.log("goToTraceDetailsPage" + traceId)
            $window.location.href =  AppUtil.prefixPath() + "/trace_detail.html?#traceId=" + traceId;
      }

      function formatDate(dateStr) {
            var date = new Date(dateStr);
            var formattedDate = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`;
            return formattedDate;
      }

}



