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
audit_log_trace_detail_module.controller('AuditLogTraceDetailController',
    ['$scope', '$location', '$window', '$translate', 'toastr', 'AppService', 'AppUtil', 'EventManager', 'AuditLogService',
      auditLogTraceDetailController]
);
function auditLogTraceDetailController($scope, $location, $window, $translate, toastr, AppService, AppUtil, EventManager, AuditLogService) {
      var params = AppUtil.parseParams($location.$$url);
      $scope.traceId = params.traceId;

      $scope.traceDetails = [];
      $scope.showingDetail = {};
      $scope.dataInfluenceEntities = [];
      $scope.relatedDataInfluences = [];
      $scope.relatedDataInfluencePage = 0;
      $scope.relatedDataInfluenceHasLoadAll = true;
      var RelatedDataInfluencePageSize = 10;
      $scope.setShowingDetail = setShowingDetail;
      $scope.showText = showText;
      $scope.removeInClassFromLogDropDownExceptId = removeInClassFromLogDropDownExceptId;
      $scope.findMoreRelatedDataInfluence = findMoreRelatedDataInfluence;
      $scope.showRelatedDataInfluence = showRelatedDataInfluence;
      $scope.findOpNameBySpanId = findOpNameBySpanId;
      $scope.refreshDataInfluenceEntities = refreshDataInfluenceEntities;

      init();

      function init() {
            getTraceDetails();
      }

      function getTraceDetails() {
            AuditLogService.find_trace_details($scope.traceId).then(
                function (result) {
                      $scope.traceDetails = result;
                }
            );
      }

      function setShowingDetail(detail) {
            $scope.showingDetail = detail;
            refreshDataInfluenceEntities();
      }

      function removeInClassFromLogDropDownExceptId(id) {
            $scope.relatedDataInfluences = [];
            $scope.relatedDataInfluenceHasLoadAll = true;

            var elements = document.querySelectorAll('[id^="detail"]');

            elements.forEach(function (element) {
                  if(element.id !== 'detail'+id) {
                        element.classList.remove('in');
                  }

            });
      }

      function showRelatedDataInfluence(entityName, entityId, fieldName) {
            $scope.entityNameOfFindRelated = entityName;
            $scope.entityIdOfFindRelated = entityId;
            $scope.fieldNameOfFindRelated = fieldName;

            AuditLogService.find_dataInfluences_by_field(
                $scope.entityNameOfFindRelated,
                $scope.entityIdOfFindRelated,
                $scope.fieldNameOfFindRelated,
                $scope.relatedDataInfluencePage,
                RelatedDataInfluencePageSize
            ).then(function (result) {
                  if (!result || result.length < RelatedDataInfluencePageSize) {
                        $scope.relatedDataInfluenceHasLoadAll = true;
                  }
                  if (result.length === 0) {
                        return;
                  }
                  $scope.relatedDataInfluenceHasLoadAll = false;
                  $scope.relatedDataInfluences = result;
            });
      }

      function findMoreRelatedDataInfluence() {
            $scope.relatedDataInfluencePage = $scope.relatedDataInfluencePage + 1;
            AuditLogService.find_dataInfluences_by_field(
                $scope.entityNameOfFindRelated,
                $scope.entityIdOfFindRelated,
                $scope.fieldNameOfFindRelated,
                $scope.relatedDataInfluencePage,
                RelatedDataInfluencePageSize
            ).then(function (result) {
                  if (!result || result.length < RelatedDataInfluencePageSize) {
                        $scope.relatedDataInfluenceHasLoadAll = true;
                  }
                  if (result.length === 0) {
                        return;
                  }
                  $scope.relatedDataInfluences = $scope.relatedDataInfluences.concat(result);

            });
      }

      function findOpNameBySpanId(spanId) {
            var res = '';
            $scope.traceDetails.forEach(detail => {
                  if (detail.logDTO.spanId === spanId) {
                        res = detail.logDTO.opName;
                  }
            });
            return res;
      }
      
      function refreshDataInfluenceEntities() {
            var entityMap = new Map();
            $scope.showingDetail.dataInfluenceDTOList.forEach(dto => {
                  var key = `${dto.influenceEntityName}-${dto.influenceEntityId}`;
                  if (!entityMap.has(key)) {
                        entityMap.set(key, []);
                  }
                  entityMap.get(key).push(dto);
            });
            $scope.dataInfluenceEntities = Array.from(entityMap);
            console.log($scope.dataInfluenceEntities[0]);
      }

      function showText(text) {
            $scope.text = text;
            AppUtil.showModal("#showTextModal");
      }
}