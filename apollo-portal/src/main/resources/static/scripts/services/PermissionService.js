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
appService.service('PermissionService', ['$resource', '$q', 'AppUtil', 'UserService', function ($resource, $q, AppUtil, UserService) {
    var permission_resource = $resource('', {}, {
        init_app_namespace_permission: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/namespaces/:namespaceName/permission-init'
        },
        init_cluster_ns_permission: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/permission-init'
        },
        has_app_permission: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/permissions/:permissionType'
        },
        has_namespace_permission: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/namespaces/:namespaceName/permissions/:permissionType'
        },
        has_namespace_env_permission: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/namespaces/:namespaceName/permissions/:permissionType'
        },
        has_cluster_ns_permission: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/permissions/:permissionType'
        },
        has_root_permission:{
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/permissions/root'
        },
        get_namespace_role_users: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/namespaces/:namespaceName/role-users'
        },
        get_namespace_env_role_users: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/namespaces/:namespaceName/role-users'
        },
        assign_namespace_role_to_user: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/namespaces/:namespaceName/roles/:roleType'
        },
        assign_namespace_env_role_to_user: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/namespaces/:namespaceName/roles/:roleType'
        },
        remove_namespace_role_from_user: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/namespaces/:namespaceName/roles/:roleType'
        },
        remove_namespace_env_role_from_user: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/namespaces/:namespaceName/roles/:roleType'
        },
        get_app_role_users: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/role-users'
        },
        assign_app_role_to_user: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/roles/:roleType'
        },
        remove_app_role_from_user: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/roles/:roleType'
        },
        has_open_manage_app_master_role_limit: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/system/role/manage-app-master'
        },
        get_cluster_ns_role_users: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/role-users'
        },
        assign_cluster_ns_role_to_user: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/roles/:roleType'
        },
        remove_cluster_ns_role_from_user: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/roles/:roleType'
        }
    });

    var current_user_promise;

    function loadCurrentUserId() {
        if (!current_user_promise) {
            current_user_promise = UserService.load_user().then(function (user) {
                return user.userId;
            }, function (result) {
                current_user_promise = null;
                return $q.reject(result);
            });
        }
        return current_user_promise;
    }

    function attachCurrentUserId(params, callback, reject) {
        loadCurrentUserId().then(function (userId) {
            params.userId = userId;
            callback(params);
        }, reject);
    }

    function attachOperator(params, callback, reject) {
        loadCurrentUserId().then(function (operator) {
            params.operator = operator;
            callback(params);
        }, reject);
    }

    function initAppNamespacePermission(appId, namespace) {
        var d = $q.defer();
        attachOperator({
                appId: appId,
                namespaceName: namespace
            },
            function (params) {
                permission_resource.init_app_namespace_permission(params, {},
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function initClusterNsPermission(appId, env, clusterName) {
        var d = $q.defer();
        attachOperator({
                appId: appId,
                env: env,
                clusterName: clusterName
            },
            function (params) {
                permission_resource.init_cluster_ns_permission(params, {},
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function hasAppPermission(appId, permissionType) {
        var d = $q.defer();
        attachCurrentUserId({
                appId: appId,
                permissionType: permissionType
            },
            function (params) {
                permission_resource.has_app_permission(params,
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function hasNamespacePermission(appId, namespaceName, permissionType) {
        var d = $q.defer();
        attachCurrentUserId({
                appId: appId,
                namespaceName: namespaceName,
                permissionType: permissionType
            },
            function (params) {
                permission_resource.has_namespace_permission(params,
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function hasNamespaceEnvPermission(appId, env, namespaceName, permissionType) {
        var d = $q.defer();
        attachCurrentUserId({
                appId: appId,
                namespaceName: namespaceName,
                permissionType: permissionType,
                env: env
            },
            function (params) {
                permission_resource.has_namespace_env_permission(params,
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function hasClusterNsPermission(appId, env, clusterName, permissionType) {
        var d = $q.defer();
        attachCurrentUserId({
                appId: appId,
                env: env,
                clusterName: clusterName,
                permissionType: permissionType
            },
            function (params) {
                permission_resource.has_cluster_ns_permission(params,
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function assignNamespaceRoleToUser(appId, namespaceName, roleType, user) {
        var d = $q.defer();
        attachOperator({
                appId: appId,
                namespaceName: namespaceName,
                roleType: roleType,
                userId: user
            },
            function (params) {
                permission_resource.assign_namespace_role_to_user(params, {},
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function assignNamespaceEnvRoleToUser(appId, env, namespaceName, roleType, user) {
        var d = $q.defer();
        attachOperator({
                appId: appId,
                namespaceName: namespaceName,
                roleType: roleType,
                env: env,
                userId: user
            },
            function (params) {
                permission_resource.assign_namespace_env_role_to_user(params, {},
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function removeNamespaceRoleFromUser(appId, namespaceName, roleType, user) {
        var d = $q.defer();
        attachOperator({
                appId: appId,
                namespaceName: namespaceName,
                roleType: roleType,
                userId: user
            },
            function (params) {
                permission_resource.remove_namespace_role_from_user(params,
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function removeNamespaceEnvRoleFromUser(appId, env, namespaceName, roleType, user) {
        var d = $q.defer();
        attachOperator({
                appId: appId,
                namespaceName: namespaceName,
                roleType: roleType,
                userId: user,
                env: env
            },
            function (params) {
                permission_resource.remove_namespace_env_role_from_user(params,
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function assignClusterNsRoleToUser(appId, env, clusterName, roleType, user) {
        var d = $q.defer();
        attachOperator({
                appId: appId,
                env: env,
                clusterName: clusterName,
                roleType: roleType,
                userId: user
            },
            function (params) {
                permission_resource.assign_cluster_ns_role_to_user(params, {},
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    function removeClusterNsRoleFromUser(appId, env, clusterName, roleType, user) {
        var d = $q.defer();
        attachOperator({
                appId: appId,
                env: env,
                clusterName: clusterName,
                roleType: roleType,
                userId: user
            },
            function (params) {
                permission_resource.remove_cluster_ns_role_from_user(params,
                    function (result) {
                        d.resolve(result);
                    }, function (result) {
                        d.reject(result);
                    });
            }, function (result) {
                d.reject(result);
            });
        return d.promise;
    }

    return {
        init_app_namespace_permission: function (appId, namespace) {
            return initAppNamespacePermission(appId, namespace);
        },
        init_cluster_ns_permission: function (appId, env, clusterName) {
            return initClusterNsPermission(appId, env, clusterName);
        },
        has_manage_app_master_permission: function (appId) {
            return hasAppPermission(appId, 'ManageAppMaster');
        },
        has_create_namespace_permission: function (appId) {
            return hasAppPermission(appId, 'CreateNamespace');
        },
        has_create_cluster_permission: function (appId) {
            return hasAppPermission(appId, 'CreateCluster');
        },
        has_assign_user_permission: function (appId) {
            return hasAppPermission(appId, 'AssignRole');
        },
        has_modify_namespace_permission: function (appId, namespaceName) {
            return hasNamespacePermission(appId, namespaceName, 'ModifyNamespace');
        },
        has_modify_namespace_env_permission: function (appId, env, namespaceName) {
            return hasNamespaceEnvPermission(appId, env, namespaceName, 'ModifyNamespace');
        },
        has_release_namespace_permission: function (appId, namespaceName) {
            return hasNamespacePermission(appId, namespaceName, 'ReleaseNamespace');
        },
        has_release_namespace_env_permission: function (appId, env, namespaceName) {
            return hasNamespaceEnvPermission(appId, env, namespaceName, 'ReleaseNamespace');
        },
        has_root_permission: function () {
            var d = $q.defer();
            attachCurrentUserId({},
                function (params) {
                    permission_resource.has_root_permission(params,
                        function (result) {
                            d.resolve(result);
                        }, function (result) {
                            d.reject(result);
                        });
                }, function (result) {
                    d.reject(result);
                });
            return d.promise;
            
        },
        assign_modify_namespace_role: function (appId, namespaceName, user) {
            return assignNamespaceRoleToUser(appId, namespaceName, 'ModifyNamespace', user);
        },
        assign_modify_namespace_env_role: function (appId, env, namespaceName, user) {
            return assignNamespaceEnvRoleToUser(appId, env, namespaceName, 'ModifyNamespace', user);
        },
        assign_release_namespace_role: function (appId, namespaceName, user) {
            return assignNamespaceRoleToUser(appId, namespaceName, 'ReleaseNamespace', user);
        },
        assign_release_namespace_env_role: function (appId, env, namespaceName, user) {
            return assignNamespaceEnvRoleToUser(appId, env, namespaceName, 'ReleaseNamespace', user);
        },
        remove_modify_namespace_role: function (appId, namespaceName, user) {
            return removeNamespaceRoleFromUser(appId, namespaceName, 'ModifyNamespace', user);
        },
        remove_modify_namespace_env_role: function (appId, env, namespaceName, user) {
            return removeNamespaceEnvRoleFromUser(appId, env, namespaceName, 'ModifyNamespace', user);
        },
        remove_release_namespace_role: function (appId, namespaceName, user) {
            return removeNamespaceRoleFromUser(appId, namespaceName, 'ReleaseNamespace', user);
        },
        remove_release_namespace_env_role: function (appId, env, namespaceName, user) {
            return removeNamespaceEnvRoleFromUser(appId, env, namespaceName, 'ReleaseNamespace', user);
        },
        get_namespace_role_users: function (appId, namespaceName) {
            var d = $q.defer();
            permission_resource.get_namespace_role_users({
                                                              appId: appId,
                                                              namespaceName: namespaceName,
                                                          },
                                                         function (result) {
                                                              d.resolve(result);
                                                          }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        },
        get_namespace_env_role_users: function (appId, env, namespaceName) {
            var d = $q.defer();
            permission_resource.get_namespace_env_role_users({
                    appId: appId,
                    namespaceName: namespaceName,
                    env: env
                },
                function (result) {
                    d.resolve(result);
                }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        },
        get_app_role_users: function (appId) {
            var d = $q.defer();
            permission_resource.get_app_role_users({
                                                        appId: appId
                                                    },
                                                   function (result) {
                                                        d.resolve(result);
                                                    }, function (result) {
                    d.reject(result);
                });
            return d.promise;    
        },
        assign_master_role: function (appId, user) {
            var d = $q.defer();
            attachOperator({
                    appId: appId,
                    roleType: 'Master',
                    userId: user
                },
                function (params) {
                    permission_resource.assign_app_role_to_user(params, {},
                        function (result) {
                            d.resolve(result);
                        }, function (result) {
                            d.reject(result);
                        });
                }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        },
        remove_master_role: function (appId, user) {
            var d = $q.defer();
            attachOperator({
                    appId: appId,
                    roleType: 'Master',
                    userId: user
                },
                function (params) {
                    permission_resource.remove_app_role_from_user(params,
                        function (result) {
                            d.resolve(result);
                        }, function (result) {
                            d.reject(result);
                        });
                }, function (result) {
                    d.reject(result);
                });
            return d.promise;
        },
        has_open_manage_app_master_role_limit: function () {
            var d = $q.defer();
            permission_resource.has_open_manage_app_master_role_limit({},
                function (result) {
                    d.resolve(result);
                },
                function (result) {
                    d.reject(result);
                });
            return d.promise;
        },
        has_modify_cluster_ns_permission: function (appId, env, clusterName) {
            return hasClusterNsPermission(appId, env, clusterName, 'ModifyNamespacesInCluster');
        },
        has_release_cluster_ns_permission: function (appId, env, clusterName) {
            return hasClusterNsPermission(appId, env, clusterName, 'ReleaseNamespacesInCluster');
        },
        get_cluster_ns_role_users: function (appId, env, clusterName) {
            var d = $q.defer();
            permission_resource.get_cluster_ns_role_users({
                appId: appId, env: env, clusterName: clusterName
            }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        assign_modify_cluster_ns_role: function (appId, env, clusterName, user) {
            return assignClusterNsRoleToUser(appId, env, clusterName, 'ModifyNamespacesInCluster', user);
        },
        assign_release_cluster_ns_role: function (appId, env, clusterName, user) {
            return assignClusterNsRoleToUser(appId, env, clusterName, 'ReleaseNamespacesInCluster', user);
        },
        remove_modify_cluster_ns_role: function (appId, env, clusterName, user) {
            return removeClusterNsRoleFromUser(appId, env, clusterName, 'ModifyNamespacesInCluster', user);
        },
        remove_release_cluster_ns_role: function (appId, env, clusterName, user) {
            return removeClusterNsRoleFromUser(appId, env, clusterName, 'ReleaseNamespacesInCluster', user);
        },
    }
}]);
