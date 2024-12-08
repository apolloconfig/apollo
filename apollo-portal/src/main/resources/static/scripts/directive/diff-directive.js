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
directive_module.directive('apollodiff',
    function ($compile, $window, AppUtil) {
        return {
            restrict: 'E',
            templateUrl: AppUtil.prefixPath() + '/views/component/diff.html',
            transclude: true,
            replace: true,
            scope: {
                oldStr: '=',
                newStr: '=',
                apolloId: '='
            },
            link: function (scope, element, attrs) {

                scope.$watch('oldStr', makeDiff);
                scope.$watch('newStr', makeDiff);



                function makeDiff() {
                    var displayArea = document.getElementById(scope.apolloId);
                    if (!displayArea) {
                        return;
                    }
                    //clear
                    displayArea.innerHTML = '';

                    var color = '',
                        span = null,
                        pre = '';

                    var oldStr = scope.oldStr == undefined ? '' : scope.oldStr;
                    var newStr = scope.newStr == undefined ? '' : scope.newStr;

                    var oldStrRes = oldStr.replace(/\r/g, "");
                    var newStrRes = newStr.replace(/\r/g, "");
                    var diff = JsDiff.diffLines(oldStrRes, newStrRes),
                        fragment = document.createDocumentFragment();

                    diff.forEach(function (part) {
                        // green for additions, red for deletions
                        // grey for common parts
                        color = part.added ? 'green' :
                            part.removed ? 'red' : 'grey';
                        span = document.createElement('span');
                        span.style.color = color;
                        span.style.display = 'block';
                        pre = part.added ? '+' :
                            part.removed ? '-' : '';
                        span.appendChild(document.createTextNode(pre + part.value));
                        fragment.appendChild(span);
                    });

                    displayArea.appendChild(fragment);

                }

            }
        }

    });
