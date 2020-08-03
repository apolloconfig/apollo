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
var server_config_module = angular.module('server_config', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//setting
var setting_module = angular.module('setting', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar', 'valdr']);
//role
var role_module = angular.module('role', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar']);
//cluster
var cluster_module = angular.module('cluster', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar', 'valdr']);
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