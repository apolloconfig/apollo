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
appService.service("ConfigService", ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    var OPENAPI_ITEM_PAGE_SIZE = 200;
    var PORTAL_USER_OPERATOR_PLACEHOLDER = 'portal-user';

    var config_source = $resource("", {}, {
        load_namespace: {
            method: 'GET',
            isArray: false,
            url: AppUtil.prefixPath() + '/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName'
        },
        load_public_namespace_for_associated_namespace: {
            method: 'GET',
            isArray: false,
            url: AppUtil.prefixPath() + '/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/associated-public-namespace'
        },
        load_all_namespaces: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/apps/:appId/envs/:env/clusters/:clusterName/namespaces'
        },
        find_items: {
            method: 'GET',
            isArray: false,
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items'
        },
        modify_items: {
            method: 'PUT',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items'
        },
        diff: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items/diff',
            isArray: true
        },
        sync_item: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items/synchronize',
            isArray: false
        },
        create_item: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items'
        },
        update_item: {
            method: 'PUT',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/encodedItems/:key'
        },
        delete_item: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/encodedItems/:key'
        },
        syntax_check_text: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items/validation'
        },
        revoke_item: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items/revocation'
        },
    });

    function encodeBase64PathSegment(value) {
        var stringValue = String(value);
        var binary = '';
        if (typeof TextEncoder !== 'undefined') {
            var bytes = new TextEncoder().encode(stringValue);
            for (var i = 0; i < bytes.length; i++) {
                binary += String.fromCharCode(bytes[i]);
            }
        } else {
            binary = encodeURIComponent(stringValue)
                .replace(/%([0-9A-Fa-f]{2})/g, function (match, hex) {
                    return String.fromCharCode(parseInt(hex, 16));
                });
        }
        return btoa(binary)
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=+$/, '');
    }

    function sortItems(items, orderBy) {
        if (orderBy === 'lastModifiedTime') {
            items.sort(function (item1, item2) {
                var time1 = new Date(item1.dataChangeLastModifiedTime || 0).getTime();
                var time2 = new Date(item2.dataChangeLastModifiedTime || 0).getTime();
                return time2 - time1;
            });
            return items;
        }
        items.sort(function (item1, item2) {
            return (item1.lineNum || 0) - (item2.lineNum || 0);
        });
        return items;
    }

    function toLegacyDiff(openItemDiff) {
        return {
            namespace: openItemDiff.namespace,
            diffs: {
                createItems: openItemDiff.createItems || [],
                updateItems: openItemDiff.updateItems || [],
                deleteItems: openItemDiff.deleteItems || []
            },
            extInfo: openItemDiff.message
        };
    }

    return {
        load_namespace: function (appId, env, clusterName, namespaceName) {
            var d = $q.defer();
            config_source.load_namespace({
                                             appId: appId,
                                             env: env,
                                             clusterName: clusterName,
                                             namespaceName: namespaceName
                                         }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        load_public_namespace_for_associated_namespace: function (env, appId, clusterName, namespaceName) {
            var d = $q.defer();
            config_source.load_public_namespace_for_associated_namespace({
                                                                             env: env,
                                                                             appId: appId,
                                                                             clusterName: clusterName,
                                                                             namespaceName: namespaceName
                                                                         }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        load_all_namespaces: function (appId, env, clusterName) {
            var d = $q.defer();
            config_source.load_all_namespaces({
                                                  appId: appId,
                                                  env: env,
                                                  clusterName: clusterName
                                              }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },

        find_items: function (appId, env, clusterName, namespaceName, orderBy) {
            var d = $q.defer();
            var items = [];
            var fetchPage = function (page) {
                config_source.find_items({
                                             appId: appId,
                                             env: env,
                                             clusterName: clusterName,
                                             namespaceName: namespaceName,
                                             page: page,
                                             size: OPENAPI_ITEM_PAGE_SIZE
                                         }, function (result) {
                    var content = result.content || [];
                    items = items.concat(content);
                    if (content.length === 0) {
                        d.resolve(sortItems(items, orderBy));
                        return;
                    }
                    if (items.length < (result.total || 0)) {
                        fetchPage(page + 1);
                        return;
                    }
                    d.resolve(sortItems(items, orderBy));
                }, function (result) {
                    d.reject(result);
                });
            };
            fetchPage(0);
            return d.promise;
        },

        modify_items: function (appId, env, clusterName, namespaceName, model) {
            var d = $q.defer();
            config_source.modify_items({
                                           appId: appId,
                                           env: env,
                                           clusterName: clusterName,
                                           namespaceName: namespaceName
                                       },
                                       model, function (result) {
                    d.resolve(result);

                }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        },

        diff: function (appId, env, clusterName, namespaceName, sourceData) {
            var d = $q.defer();
            config_source.diff({
                                   appId: appId,
                                   env: env,
                                   clusterName: clusterName,
                                   namespaceName: namespaceName
                               }, sourceData, function (result) {
                d.resolve(result.map(toLegacyDiff));
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },

        sync_items: function (appId, env, clusterName, namespaceName, sourceData) {
            var d = $q.defer();
            config_source.sync_item({
                                        appId: appId,
                                        env: env,
                                        clusterName: clusterName,
                                        namespaceName: namespaceName
                                    }, sourceData, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },

        create_item: function (appId, env, clusterName, namespaceName, item) {
            var d = $q.defer();
            config_source.create_item({
                                          appId: appId,
                                          env: env,
                                          clusterName: clusterName,
                                          namespaceName: namespaceName
                                      }, item, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },

        update_item: function (appId, env, clusterName, namespaceName, item) {
            var d = $q.defer();
            config_source.update_item({
                                          appId: appId,
                                          env: env,
                                          clusterName: clusterName,
                                          namespaceName: namespaceName,
                                          key: encodeBase64PathSegment(item.key),
                                          createIfNotExists: false
                                      }, item, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },

        delete_item: function (appId, env, clusterName, namespaceName, key) {
            var d = $q.defer();
            config_source.delete_item({
                                          appId: appId,
                                          env: env,
                                          clusterName: clusterName,
                                          namespaceName: namespaceName,
                                          key: encodeBase64PathSegment(key),
                                          operator: PORTAL_USER_OPERATOR_PLACEHOLDER
                                      }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },

        syntax_check_text: function (appId, env, clusterName, namespaceName, model) {
            var d = $q.defer();
            config_source.syntax_check_text({
                                           appId: appId,
                                           env: env,
                                           clusterName: clusterName,
                                           namespaceName: namespaceName
                                       },
                                       model, function (result) {
                    d.resolve(result);

                }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        },

        revoke_item:  function (appId, env, clusterName, namespaceName) {
            var d = $q.defer();
            config_source.revoke_item({
                  appId: appId,
                  env: env,
                  clusterName: clusterName,
                  namespaceName: namespaceName
                },{}, function (result) {
                  d.resolve(result);

                }, function (result) {
                  d.reject(result);
                });
            return d.promise;
        }
    }

}]);
