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
directive_module.directive('publishdenymodal', publishDenyDirective);

function publishDenyDirective(AppUtil, EventManager) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/publish-deny-modal.html',
        transclude: true,
        replace: true,
        scope: {
            env: "="
        },
        link: function (scope) {
            var MODAL_ID = "#publishDenyModal";

            EventManager.subscribe(EventManager.EventType.PUBLISH_DENY, function (context) {
                scope.toReleaseNamespace = context.namespace;
                scope.mergeAndPublish = !!context.mergeAndPublish;
                AppUtil.showModal(MODAL_ID);
            });

            scope.emergencyPublish = emergencyPublish;

            function emergencyPublish() {
                AppUtil.hideModal(MODAL_ID);

                EventManager.emit(EventManager.EventType.EMERGENCY_PUBLISH,
                    {
                        mergeAndPublish: scope.mergeAndPublish,
                        namespace: scope.toReleaseNamespace
                    });

            }
        }
    }
}


