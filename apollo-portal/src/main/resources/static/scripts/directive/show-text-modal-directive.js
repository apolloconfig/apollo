/*
 * Copyright 2021 Apollo Authors
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
directive_module.directive('showtextmodal', showTextModalDirective);

function showTextModalDirective(AppUtil) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/show-text-modal.html',
        transclude: true,
        replace: true,
        scope: {
            text: '='
        },
        link: function (scope) {
            scope.$watch('text', init);

            function init() {
                scope.jsonObject = undefined;
                if (isJsonText(scope.text)) {
                    scope.jsonObject = JSON.parse(scope.text);
                }
            }

            function isJsonText(text) {
                try {
                    return typeof JSON.parse(text) === "object";
                } catch (e) {
                    return false;
                }
            }
        }
    }
}


