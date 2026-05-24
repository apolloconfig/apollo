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
appService.service('ReleaseService', ['$resource', '$q','AppUtil', function ($resource, $q,AppUtil) {
    function toLegacyReleaseChange(change) {
        if (!change || change.entity) {
            return change;
        }
        return {
            type: change.changeType,
            entity: {
                firstEntity: {
                    key: change.key,
                    value: change.oldValue
                },
                secondEntity: {
                    key: change.key,
                    value: change.newValue
                }
            }
        };
    }

    function adaptReleaseDiff(result) {
        if (!result || !result.changes) {
            return result;
        }
        result.changes = result.changes.map(toLegacyReleaseChange);
        return result;
    }

    var resource = $resource('', {}, {
        get: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/releases/:releaseId'
        },
        find_active_releases: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/releases/active',
            isArray: true
        },
        compare: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/releases/comparison'
        },
        release: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/releases'
        },
        gray_release: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName/releases'
        },
        rollback: {
            method: 'PUT',
            url: AppUtil.prefixPath() + "/openapi/v1/envs/:env/releases/:releaseId/rollback"
        }
    });

    function createRelease(appId, env, clusterName, namespaceName, releaseTitle, comment, isEmergencyPublish) {
        var d = $q.defer();
        resource.release({
                             appId: appId,
                             env: env,
                             clusterName: clusterName,
                             namespaceName: namespaceName
                         }, {
                             releaseTitle: releaseTitle,
                             releaseComment: comment,
                             isEmergencyPublish: isEmergencyPublish
                         }, function (result) {
            d.resolve(adaptReleaseDiff(result));
        }, function (result) {
            d.reject(result);
        });
        return d.promise;
    }

    function createGrayRelease(appId, env, clusterName, namespaceName, branchName, releaseTitle, comment, isEmergencyPublish) {
        var d = $q.defer();
        resource.gray_release({
                                  appId: appId,
                                  env: env,
                                  clusterName: clusterName,
                                  namespaceName: namespaceName,
                                  branchName: branchName
                              }, {
                                  releaseTitle: releaseTitle,
                                  releaseComment: comment,
                                  isEmergencyPublish: isEmergencyPublish
                              }, function (result) {
            d.resolve(result);
        }, function (result) {
            d.reject(result);
        });
        return d.promise;
    }

    function get(env, releaseId) {
        var d = $q.defer();
        resource.get({
                         env: env,
                         releaseId: releaseId
                         }, function (result) {
            d.resolve(adaptReleaseDiff(result));
        }, function (result) {
            d.reject(result);
        });
        return d.promise;
    }

    function findActiveReleases(appId, env, clusterName, namespaceName, page, size) {
        var d = $q.defer();
        resource.find_active_releases({
                                          appId: appId,
                                          env: env,
                                          clusterName: clusterName,
                                          namespaceName: namespaceName,
                                          page: page,
                                          size: size
                                      }, function (result) {
            d.resolve(result);
        }, function (result) {
            d.reject(result);
        });
        return d.promise;
    }

    function findLatestActiveRelease(appId, env, clusterName, namespaceName) {
        var d = $q.defer();
        resource.find_active_releases({
                                          appId: appId,
                                          env: env,
                                          clusterName: clusterName,
                                          namespaceName: namespaceName,
                                          page: 0,
                                          size: 1
                                      }, function (result) {
            if (result && result.length) {
                d.resolve(result[0]);
            }

            d.resolve(undefined);

        }, function (result) {
            d.reject(result);
        });
        return d.promise;

    }

    function compare(env, baseReleaseId, toCompareReleaseId) {
        var d = $q.defer();
        resource.compare({
                             env: env,
                             baseReleaseId: baseReleaseId,
                             toCompareReleaseId: toCompareReleaseId
                         }, function (result) {
            d.resolve(adaptReleaseDiff(result));
        }, function (result) {
            d.reject(result);
        });
        return d.promise;
    }

    function rollback(env, releaseId) {
        var d = $q.defer();
        resource.rollback({
                              env: env,
                              releaseId: releaseId
                          }, {}, function (result) {
                              d.resolve(result);
                          }, function (result) {
                              d.reject(result);
                          }
        );
        return d.promise;

    }

    function rollbackTo(env, releaseId, toReleaseId) {
        var d = $q.defer();
        resource.rollback({
                              env: env,
                              releaseId: releaseId,
                              toReleaseId: toReleaseId
                          }, {}, function (result) {
                              d.resolve(result);
                          }, function (result) {
                              d.reject(result);
                          }
        );
        return d.promise;

    }

    return {
        publish: createRelease,
        grayPublish: createGrayRelease,
        get: get,
        findActiveReleases: findActiveReleases,
        findLatestActiveRelease: findLatestActiveRelease,
        compare: compare,
        rollback: rollback,
        rollbackTo: rollbackTo
    }
}]);
