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
login_module.controller('LoginController',
    ['$scope', '$window', '$location', '$translate', 'toastr', 'AppUtil',
        LoginController]);

function LoginController($scope, $window, $location, $translate, toastr, AppUtil) {
    if ($location.$$url) {
        var params = AppUtil.parseParams($location.$$url);
        if (params.error) {
            $translate('Login.UserNameOrPasswordIncorrect').then(function(result)  {
                $scope.info = result;
            });
        }
        if (params.logout) {
            $translate('Login.LogoutSuccessfully').then(function(result)  {
                $scope.info = result;
            });
        }
    }

}
