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
appService.service('AuditLogService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
  var audit_resource = $resource('', {}, {
    get_properties: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/apollo/audit/properties',
      isArray: false
    },
    find_all_logs: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/apollo/audit/logs?page=:page&size=:size',
      isArray: true
    },
    find_logs_by_opName: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/apollo/audit/logs/opName?opName=:opName&page=:page&size=:size&startDate=:startDate&endDate=:endDate',
      isArray: true
    },
    find_trace_details: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/apollo/audit/trace?traceId=:traceId',
      isArray: true
    },
    find_dataInfluences_by_field: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/apollo/audit/logs/dataInfluences/field?entityName=:entityName&entityId=:entityId&fieldName=:fieldName&page=:page&size=:size',
      isArray: true
    },
    search_by_name_or_type_or_operator: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/apollo/audit/logs/by-name-or-type-or-operator?query=:query&page=:page&size=:size',
      isArray: true
    }
  });
  return {
    get_properties: function () {
      var d = $q.defer();
      audit_resource.get_properties({
          }, function (result) {
            d.resolve(result);
          }, function (result) {
            d.reject(result);
          }
      );
      return d.promise;
    },
    find_all_logs: function (page, size) {
      var d = $q.defer();
      audit_resource.find_all_logs({
        page: page,
        size: size
        }, function (result) {
          d.resolve(result);
        }, function (result) {
          d.reject(result);
        }
      );
      return d.promise;
    },
    find_logs_by_opName: function (opName, startDate, endDate, page, size) {
      var d = $q.defer();
      audit_resource.find_logs_by_opName({
            opName: opName,
            startDate: startDate,
            endDate: endDate,
            page: page,
            size: size
          }, function (result) {
            d.resolve(result);
          }, function (result) {
            d.reject(result);
          }
      );
      return d.promise;
    },
    find_trace_details: function (traceId) {
      var d = $q.defer();
      audit_resource.find_trace_details({
            traceId: traceId
          }, function (result) {
            d.resolve(result);
          }, function (result) {
            d.reject(result);
          }
      );
      return d.promise;
    },
    find_dataInfluences_by_field: function (entityName, entityId, fieldName, page, size) {
      var d = $q.defer();
      audit_resource.find_dataInfluences_by_field({
            entityName: entityName,
            entityId: entityId,
            fieldName: fieldName,
            page: page,
            size: size
          }, function (result) {
            d.resolve(result);
          }, function (result) {
            d.reject(result);
          }
      );
      return d.promise;
    },
    search_by_name_or_type_or_operator: function (query, page, size) {
      var d = $q.defer();
      audit_resource.search_by_name_or_type_or_operator({
            query: query,
            page: page,
            size: size
          }, function (result) {
            d.resolve(result);
          }, function (result) {
            d.reject(result);
          }
      );
      return d.promise;
    }
  };
}]);