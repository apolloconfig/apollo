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
var prefixPath = window.localStorage.getItem("prefixPath") || "";

/**utils*/
var appUtil = angular.module('app.util', ['toastr', 'ngCookies', 'pascalprecht.translate'])
    .constant("prefixLocation", prefixPath)      // 前缀路径
    .filter('prefixPath',['prefixLocation', function(prefixLocation) {   // 前缀路径过滤器
        return function(text) {
            return prefixLocation + text;
        }
    }])
    .config(['$translateProvider','prefixLocation', function ($translateProvider,prefixLocation) {

        $translateProvider.useSanitizeValueStrategy(null); // disable sanitization by default
        $translateProvider.useCookieStorage();
        $translateProvider.useStaticFilesLoader({
            prefix: prefixLocation + '/i18n/',
            suffix: '.json'
        });
        $translateProvider.registerAvailableLanguageKeys(['en', 'zh-CN'], {
                              'zh-*': 'zh-CN',
                              'zh': 'zh-CN',
                              'en-*': 'en',
                              "*": "en"
                            })
        $translateProvider.uniformLanguageTag('bcp47').determinePreferredLanguage();
    }]);


/**service module 定义*/
var appService = angular.module('app.service', ['ngResource', 'app.util'])


/** directive */
var directive_module = angular.module('apollo.directive', ['app.service', 'app.util', 'toastr', 'pascalprecht.translate']);

/** page module 定义*/
// 首页
var index_module = angular.module('index', ['toastr', 'app.service', 'apollo.directive', 'app.util', 'angular-loading-bar', 'pascalprecht.translate']);
//项目主页
var application_module = angular.module('application', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar', 'valdr', 'ui.ace', 'ngSanitize']);
//创建项目页面
var app_module = angular.module('create_app', ['apollo.directive', 'toastr', 'app.service', 'app.util', 'angular-loading-bar', 'valdr','pascalprecht.translate']);
//配置同步页面
var sync_item_module = angular.module('sync_item', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
// 比较页面
var diff_item_module = angular.module('diff_item', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//namespace
var namespace_module = angular.module('namespace', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar', 'valdr']);
//server config
var server_config_manage_module = angular.module('server_config_manage', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
// Value的全局检索
var global_search_value_module = angular.module('global_search_value', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar', 'ngSanitize']);
//setting
var setting_module = angular.module('setting', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar', 'valdr']);
//role
var role_module = angular.module('role', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//cluster
var cluster_module = angular.module('cluster', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar', 'valdr']);
//manage_cluster
var manage_cluster_module = angular.module('manage_cluster', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar', 'valdr']);
//release history
var release_history_module = angular.module('release_history', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//open manage
var open_manage_module = angular.module('open_manage', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//user
var user_module = angular.module('user', ['apollo.directive', 'toastr', 'app.service', 'app.util', 'angular-loading-bar', 'valdr']);
//login
var login_module = angular.module('login', ['app.service', 'toastr', 'app.util', 'pascalprecht.translate']);
//delete app cluster namespace
var delete_app_cluster_namespace_module = angular.module('delete_app_cluster_namespace', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//system info
var system_info_module = angular.module('system_info', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//access secretKey
var access_key_module = angular.module('access_key', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//config export
var config_export_module = angular.module('config_export', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//audit log menu
var audit_log_menu_module = angular.module('audit_log', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//audit log trace detail
var audit_log_trace_detail_module = angular.module('audit_log_trace_detail', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
