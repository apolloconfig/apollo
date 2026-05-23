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
appService.service("NamespaceService", ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    var namespace_source = $resource("", {}, {
        find_public_namespaces: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/openapi/v1/appnamespaces'
        },
        createNamespace: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/namespaces',
            isArray: false
        },
        createAppNamespace: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/appnamespaces',
            isArray: false
        },
        getNamespacePublishInfo: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/namespaces/releases/status'
        },
        deleteLinkedNamespace: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName'
        },
        getPublicAppNamespaceAllNamespaces: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/envs/:env/appnamespaces/:publicNamespaceName/instances',
            isArray: true
        },
        loadAppNamespace: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/appnamespaces/:namespaceName'
        },
        deleteAppNamespace: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/appnamespaces/:namespaceName'
        },
        getLinkedNamespaceUsage: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/usage',
            isArray: true
        },
        getNamespaceUsage: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/appnamespaces/:namespaceName/usage',
            isArray: true
        },
        findPublicNamespaceNames: {
            method: 'GET',
            isArray: true,
            url: AppUtil.prefixPath() + '/openapi/v1/appnamespaces'
        }
    });

    function normalizeAppNamespace(appNamespace) {
        var normalized = angular.copy(appNamespace || {});
        normalized.isPublic = !!normalized.isPublic;
        return normalized;
    }

    function normalizeAppNamespaces(appNamespaces) {
        var normalized = [];
        angular.forEach(appNamespaces || [], function (appNamespace) {
            normalized.push(normalizeAppNamespace(appNamespace));
        });
        return normalized;
    }

    function toOpenCreateNamespaceDTO(model, appId) {
        var namespace = model.namespace || {};
        return {
            appId: model.appId || namespace.appId || appId,
            env: model.env,
            clusterName: model.clusterName || namespace.clusterName,
            appNamespaceName: model.appNamespaceName || model.namespaceName || namespace.namespaceName
        };
    }

    function toOpenCreateNamespaceDTOs(namespaceCreationModels, appId) {
        var openNamespaceCreationModels = [];
        angular.forEach(namespaceCreationModels || [], function (model) {
            openNamespaceCreationModels.push(toOpenCreateNamespaceDTO(model, appId));
        });
        return openNamespaceCreationModels;
    }

    function toOpenAppNamespace(appNamespace, appendNamespacePrefix) {
        var openAppNamespace = angular.copy(appNamespace || {});
        openAppNamespace.appendNamespacePrefix = appendNamespacePrefix;
        return openAppNamespace;
    }

    function toLegacyItem(openItem) {
        var item = angular.copy(openItem || {});
        var extendInfo = item.extendInfo || {};
        delete item.extendInfo;
        if (extendInfo.namespaceId) {
            item.namespaceId = extendInfo.namespaceId;
        }
        return {
            item: item,
            isModified: !!extendInfo.isModified,
            isDeleted: !!extendInfo.isDeleted,
            isNewlyAdded: !!extendInfo.isNewlyAdded,
            oldValue: extendInfo.oldValue,
            newValue: extendInfo.newValue
        };
    }

    function toLegacyNamespace(openNamespace) {
        var namespace = angular.copy(openNamespace || {});
        var extendInfo = namespace.extendInfo || {};
        var items = [];
        angular.forEach(namespace.items || [], function (item) {
            items.push(toLegacyItem(item));
        });
        return {
            baseInfo: {
                appId: namespace.appId,
                clusterName: namespace.clusterName,
                namespaceName: namespace.namespaceName,
                dataChangeCreatedBy: namespace.dataChangeCreatedBy,
                dataChangeLastModifiedBy: namespace.dataChangeLastModifiedBy,
                dataChangeCreatedTime: namespace.dataChangeCreatedTime,
                dataChangeLastModifiedTime: namespace.dataChangeLastModifiedTime
            },
            itemModifiedCnt: extendInfo.itemModifiedCnt || 0,
            items: items,
            format: namespace.format,
            isPublic: !!namespace.isPublic,
            parentAppId: extendInfo.parentAppId,
            comment: namespace.comment,
            isConfigHidden: !!extendInfo.isConfigHidden
        };
    }

    function toLegacyNamespaces(openNamespaces) {
        var namespaces = [];
        angular.forEach(openNamespaces || [], function (namespace) {
            namespaces.push(toLegacyNamespace(namespace));
        });
        return namespaces;
    }

    function find_public_namespaces() {
        var d = $q.defer();
        namespace_source.find_public_namespaces({}, function (result) {
            d.resolve(normalizeAppNamespaces(result));
        }, function (result) {
            d.reject(result);
        });
        return d.promise;
    }

    function createNamespace(appId, namespaceCreationModel) {
        var d = $q.defer();
        namespace_source.createNamespace({}, toOpenCreateNamespaceDTOs(namespaceCreationModel, appId), function (result) {
            d.resolve(result);
        }, function (result) {
            d.reject(result);
        });
        return d.promise;
    }

    function createAppNamespace(appId, appnamespace, appendNamespacePrefix) {
        var d = $q.defer();
        namespace_source.createAppNamespace({
            appId: appId
        }, toOpenAppNamespace(appnamespace, appendNamespacePrefix), function (result) {
            d.resolve(normalizeAppNamespace(result));
        }, function (result) {
            d.reject(result);
        });
        return d.promise;
    }

    function getNamespacePublishInfo(appId) {
        var d = $q.defer();
        namespace_source.getNamespacePublishInfo({
                                                     appId: appId
                                                 }, function (result) {
            d.resolve(result);
        }, function (result) {
            d.reject(result);
        });

        return d.promise;
    }

    function deleteLinkedNamespace(appId, env, clusterName, namespaceName) {
        var d = $q.defer();
        namespace_source.deleteLinkedNamespace({
                appId: appId,
                env: env,
                clusterName: clusterName,
                namespaceName: namespaceName
            },
            function (result) {
                d.resolve(result);
            },
            function (result) {
                d.reject(result);
            });

        return d.promise;
    }

    function getPublicAppNamespaceAllNamespaces(env, publicNamespaceName, page, size) {
        var d = $q.defer();
        namespace_source.getPublicAppNamespaceAllNamespaces({
                                                                env: env,
                                                                publicNamespaceName: publicNamespaceName,
                                                                page: page,
                                                                size: size
                                                            }, function (result) {
            d.resolve(toLegacyNamespaces(result));
        }, function (result) {
            d.reject(result);
        });

        return d.promise;

    }

    function loadAppNamespace(appId, namespaceName) {
        var d = $q.defer();
        namespace_source.loadAppNamespace({
                                             appId: appId,
                                             namespaceName: namespaceName
                                         },
                                         function (result) {
                                             d.resolve(normalizeAppNamespace(result));
                                         },
                                         function (result) {
                                             d.reject(result);
                                         });

        return d.promise;
    }

    function deleteAppNamespace(appId, namespaceName) {
        var d = $q.defer();
        namespace_source.deleteAppNamespace({
                                             appId: appId,
                                             namespaceName: namespaceName
                                         },
                                         function (result) {
                                             d.resolve(result);
                                         },
                                         function (result) {
                                             d.reject(result);
                                         });

        return d.promise;
    }

    function getLinkedNamespaceUsage(appId, env, clusterName, namespaceName) {
        var d = $q.defer();
        namespace_source.getLinkedNamespaceUsage({
                appId: appId,
                env: env,
                clusterName: clusterName,
                namespaceName: namespaceName
            },
            function (result) {
                d.resolve(result);
            },
            function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function getNamespaceUsage(appId, namespaceName) {
        var d = $q.defer();
        namespace_source.getNamespaceUsage({
                appId: appId,
                namespaceName: namespaceName
            },
            function (result) {
                d.resolve(result);
            },
            function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function findPublicNamespaceNames() {
        var d = $q.defer();
        namespace_source.findPublicNamespaceNames({}, function (result) {
            d.resolve(normalizeAppNamespaces(result).map(function (appNamespace) {
                return appNamespace.name;
            }));
        }, function (result) {
            d.reject(result);
        });
        return d.promise;
    }

    return {
        find_public_namespaces: find_public_namespaces,
        createNamespace: createNamespace,
        createAppNamespace: createAppNamespace,
        getNamespacePublishInfo: getNamespacePublishInfo,
        deleteLinkedNamespace: deleteLinkedNamespace,
        getPublicAppNamespaceAllNamespaces: getPublicAppNamespaceAllNamespaces,
        loadAppNamespace: loadAppNamespace,
        deleteAppNamespace: deleteAppNamespace,
        getLinkedNamespaceUsage: getLinkedNamespaceUsage,
        getNamespaceUsage: getNamespaceUsage,
        findPublicNamespaceNames: findPublicNamespaceNames
    }

}]);
