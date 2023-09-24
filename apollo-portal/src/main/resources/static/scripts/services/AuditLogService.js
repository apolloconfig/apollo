
appService.service('AuditLogService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
  var audit_resource = $resource('', {}, {
    find_all_logs: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/apollo/audit/logs?page=:page&size=:size',
      isArray: true
    },
    find_logs_by_opName: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/apollo/audit/logs/opName/:opName?page=:page&size=:size&startDate=:startDate&endDate=:endDate',
      isArray: true
    },
    find_trace_details: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/apollo/audit/trace/:traceId',
      isArray: true
    },
    find_dataInfluences_by_field: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/apollo/audit/logs/dataInfluences/entityName/:entityName/entityId/:entityId/fieldName/:fieldName',
      isArray: true
    }
  });
  return {
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
      )
      return d.promise
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
      )
      return d.promise
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
      )
      return d.promise
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
      )
      return d.promise
    }
  }
}])