namespace_module.controller("LinkNamespaceController",
    ['$scope', '$location', '$window', '$translate', 'toastr', 'AppService', 'AppUtil', 'NamespaceService',
        'PermissionService', 'CommonService',
        function ($scope, $location, $window, $translate, toastr, AppService, AppUtil, NamespaceService,
            PermissionService, CommonService) {

            var params = AppUtil.parseParams($location.$$url);
            $scope.appId = params.appid;
            $scope.type = 'link';

            $scope.step = 1;

            $scope.submitBtnDisabled = false;
            $scope.appendNamespacePrefix = true;

            PermissionService.has_root_permission().then(function (result) {
                $scope.hasRootPermission = result.hasPermission;
            });

            CommonService.getPageSetting().then(function (setting) {
                $scope.pageSetting = setting;
            });

            NamespaceService.find_public_namespaces().then(function (result) {
                var publicNamespaces = [];
                result.forEach(function (item) {
                    var namespace = {};
                    namespace.id = item.name;
                    namespace.text = item.name;
                    publicNamespaces.push(namespace);
                });
                $('#namespaces').select2({
                    placeholder: $translate.instant('Namespace.PleaseChooseNamespace'),
                    width: '100%',
                    data: publicNamespaces
                });
                $(".apollo-container").removeClass("hidden");
            }, function (result) {
                toastr.error(AppUtil.errorMsg(result), $translate.instant('Namespace.LoadingPublicNamespaceError'));
            });

            AppService.load($scope.appId).then(function (result) {
                $scope.appBaseInfo = result;
                $scope.appBaseInfo.namespacePrefix = result.orgId + '.';
            }, function (result) {
                toastr.error(AppUtil.errorMsg(result), $translate.instant('Namespace.LoadingAppInfoError'));
            });

            $scope.appNamespace = {
                appId: $scope.appId,
                name: '',
                comment: '',
                isPublic: true,
                format: 'properties'
            };

            $scope.switchNSType = function (type) {
                $scope.appNamespace.isPublic = type;
            };

            $scope.concatNamespace = function () {
                if (!$scope.appBaseInfo) {
                    return '';
                }
                var appNamespaceName = $scope.appNamespace.name ? $scope.appNamespace.name : '';
                if (shouldAppendNamespacePrefix()) {
                    return $scope.appBaseInfo.namespacePrefix + appNamespaceName;
                }
                return appNamespaceName;
            };

            function shouldAppendNamespacePrefix() {
                 return $scope.appNamespace.isPublic ? $scope.appendNamespacePrefix : false;
            }

            var selectedClusters = [];
            $scope.collectSelectedClusters = function (data) {
                selectedClusters = data;
            };
            $scope.createNamespace = function () {
                if ($scope.type == 'link') {
                    if (selectedClusters.length == 0) {
                        toastr.warning($translate.instant('Namespace.PleaseChooseCluster'));
                        return;
                    }

                    if ($scope.namespaceType == 1) {
                        var selectedNamespaceName = $('#namespaces').select2('data')[0].id;
                        if (!selectedNamespaceName) {
                            toastr.warning($translate.instant('Namespace.PleaseChooseNamespace'));
                            return;
                        }

                        $scope.namespaceName = selectedNamespaceName;
                    }

                    var namespaceCreationModels = [];
                    selectedClusters.forEach(function (cluster) {
                        namespaceCreationModels.push({
                            env: cluster.env,
                            namespace: {
                                appId: $scope.appId,
                                clusterName: cluster.clusterName,
                                namespaceName: $scope.namespaceName
                            }
                        });
                    });

                    $scope.submitBtnDisabled = true;
                    NamespaceService.createNamespace($scope.appId, namespaceCreationModels)
                        .then(function (result) {
                            toastr.success($translate.instant('Common.Created'));
                            $scope.step = 2;
                            setInterval(function () {
                                $scope.submitBtnDisabled = false;
                                $window.location.href =
                                AppUtil.prefixPath() + '/namespace/role.html?#appid=' + $scope.appId
                                    + "&namespaceName=" + $scope.namespaceName;
                            }, 1000);
                        }, function (result) {
                            $scope.submitBtnDisabled = false;
                            toastr.error(AppUtil.errorMsg(result));
                        });
                } else {

                    var namespaceNameLength = $scope.concatNamespace().length;
                    if (namespaceNameLength > 32) {
                        var errorTip = $translate.instant('Namespace.CheckNamespaceNameLengthTip', {
                            departmentLength: namespaceNameLength - $scope.appNamespace.name.length,
                            namespaceLength: $scope.appNamespace.name.length
                        });
                        toastr.error(errorTip);
                        return;
                    }

                    // public namespaces only allow properties format
                    if ($scope.appNamespace.isPublic) {
                        $scope.appNamespace.format = 'properties';
                    }

                    $scope.submitBtnDisabled = true;
                    //only append namespace prefix for public app namespace
                    var appendNamespacePrefix = shouldAppendNamespacePrefix();
                    NamespaceService.createAppNamespace($scope.appId, $scope.appNamespace, appendNamespacePrefix).then(
                        function (result) {
                            $scope.step = 2;
                            setTimeout(function () {
                                $scope.submitBtnDisabled = false;
                                $window.location.href =
                                AppUtil.prefixPath() + "/namespace/role.html?#/appid=" + $scope.appId
                                    + "&namespaceName=" + result.name;
                            }, 1000);
                        }, function (result) {
                            $scope.submitBtnDisabled = false;
                            toastr.error(AppUtil.errorMsg(result), $translate.instant('Common.CreateFailed'));
                        });
                }

            };

            $scope.namespaceType = 1;
            $scope.selectNamespaceType = function (type) {
                $scope.namespaceType = type;
            };

            $scope.back = function () {
                $window.location.href = AppUtil.prefixPath() + '/config.html?#appid=' + $scope.appId;
            };

            $scope.switchType = function (type) {
                $scope.type = type;
            };
        }]);

