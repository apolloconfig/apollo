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
directive_module.directive('mergeandpublishmodal', mergeAndPublishDirective);

function mergeAndPublishDirective(AppUtil, EventManager) {
    return {
        restrict: 'E',
        templateUrl: AppUtil.prefixPath() + '/views/component/merge-and-publish-modal.html',
        transclude: true,
        replace: true,
        scope: {
            appId: '=',
            env: '=',
            cluster: '='
        },
        link: function (scope) {

            scope.showReleaseModal = showReleaseModal;

            EventManager.subscribe(EventManager.EventType.MERGE_AND_PUBLISH_NAMESPACE,
                function (context) {
                    var branch = context.branch;
                    scope.toReleaseNamespace = branch;
                    scope.toDeleteBranch = branch;
                    scope.isEmergencyPublish =
                        context.isEmergencyPublish ? context.isEmergencyPublish : false;

                    var branchStatusMerge = 2;
                    branch.branchStatus = branchStatusMerge;
                    branch.mergeAndPublish = true;

                    AppUtil.showModal('#mergeAndPublishModal');
                });

            function showReleaseModal() {
                EventManager.emit(EventManager.EventType.PUBLISH_NAMESPACE,
                    {
                        namespace: scope.toReleaseNamespace,
                        isEmergencyPublish: scope.isEmergencyPublish
                    });
            }

        }
    }
}


