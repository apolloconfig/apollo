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
audit_log_menu_module.controller('AuditLogMenuController',
    ['$scope', '$window', '$translate', '$document', 'toastr', 'AppService', 'AppUtil', 'EventManager', 'AuditLogService',
        auditLogMenuController]
);

function auditLogMenuController($scope, $window, $translate, $document, toastr, AppService, AppUtil, EventManager, AuditLogService) {

    $scope.auditEnabled = false;

    $scope.auditLogList = [];
    $scope.goToTraceDetailsPage = goToTraceDetailsPage;
    $scope.searchByOpNameAndDate = searchByOpNameAndDate;
    $scope.getMoreAuditLogs = getMoreAuditLogs;

    $scope.page = 0;
    var PAGE_SIZE = 10;

    $scope.opName = '';
    $scope.startDate = null;
    $scope.endDate = null;
    $scope.startDateFmt = null;
    $scope.endDateFmt = null;

    $scope.hasLoadAll = false;

    $scope.options = [];
    $scope.showSearchDropdown = false;

    $scope.showOptions = function (query) {
        $scope.options = [];
        searchAuditLogs(query);
    };

    $scope.selectOption = function (option) {
        $scope.opName = option.opName;
        $scope.showSearchDropdown = false;
    };

    init();

    function init() {
        getAuditProperties();
        initSearchingMenu();
    }

    function getAuditProperties() {
        AuditLogService.get_properties().then(function (result) {
            $scope.auditEnabled = result.enabled;
        });
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
        if (startDate !== null) {
            $scope.startDateFmt = new Date(startDate).Format("yyyy-MM-dd hh:mm:ss.S");
        }
        if (endDate !== null) {
            $scope.endDateFmt = new Date(endDate).Format("yyyy-MM-dd hh:mm:ss.S");
        }
        $scope.auditLogList = [];
        $scope.page = 0;
        $scope.opName = opName;
        $scope.startDate = startDate;
        $scope.endDate = endDate;
        AuditLogService.find_logs_by_opName(
            $scope.opName,
            $scope.startDateFmt,
            $scope.endDateFmt,
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
        if ($scope.opName === '') {
            AuditLogService.find_all_logs($scope.page, PAGE_SIZE).then(function (result) {
                if (!result || result.length < PAGE_SIZE) {
                    $scope.hasLoadAll = true;
                }
                if (result.length === 0) {
                    return;
                }
                $scope.auditLogList = $scope.auditLogList.concat(result);
            });
        } else {
            AuditLogService.find_logs_by_opName(
                $scope.opName,
                $scope.startDateFmt,
                $scope.endDateFmt,
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

    function searchAuditLogs(query) {
        AuditLogService.search_by_name_or_type_or_operator(query, 0, 20).then(function (result) {
            result.forEach(function (log) {
                var optionDisplay = log.opName + '-(' + log.opType + ').by:' + log.operator;
                var option = {
                    id: log.id,
                    display: optionDisplay,
                    opName: log.opName
                };
                $scope.options.push(option);
            });
            $scope.showSearchDropdown = $scope.options.length > 0;
        });
    }

    function goToTraceDetailsPage(traceId) {
        $window.location.href = AppUtil.prefixPath() + "/audit_log_trace_detail.html?#traceId=" + traceId;
    }

    $document.on('click', function (event) {
        if (!$scope.showSearchDropdown) {
            return;
        }

        var target = angular.element(event.target);

        // 检查点击的目标是否是输入框或下拉栏，如果不是，则隐藏下拉栏
        if (!target.hasClass('form-control') && !target.hasClass('options-container')) {
            $scope.$apply(function () {
                $scope.showSearchDropdown = false;
            });
        }
    });
}



