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
audit_log_trace_detail_module.controller('AuditLogTraceDetailController',
    ['$scope', '$location', '$window', '$translate', 'toastr', 'AppService', 'AppUtil', 'EventManager', 'AuditLogService',
        auditLogTraceDetailController]
);

function auditLogTraceDetailController($scope, $location, $window, $translate, toastr, AppService, AppUtil, EventManager, AuditLogService) {
    var params = AppUtil.parseParams($location.$$url);
    $scope.traceId = params.traceId;

    $scope.traceDetailsTree = [];
    $scope.showingDetail = {};
    $scope.dataInfluenceEntities = [];
    $scope.relatedDataInfluences = [];
    $scope.relatedDataInfluencePage = 0;
    $scope.relatedDataInfluenceHasLoadAll = true;
    var RelatedDataInfluencePageSize = 10;
    $scope.showText = showText;
    $scope.findMoreRelatedDataInfluence = findMoreRelatedDataInfluence;
    $scope.showRelatedDataInfluence = showRelatedDataInfluence;
    $scope.refreshDataInfluenceEntities = refreshDataInfluenceEntities;

    init();

    function init() {
        buildTraceDetailsTree();
    }

    function buildTraceDetailsTree() {
        AuditLogService.find_trace_details($scope.traceId).then(
            function (result) {
                $scope.traceDetails = result;
                $scope.traceDetailsTree = buildTree($scope.traceDetails);
                // 初始化 Bootstrap Treeview
                $(document).ready(function () {
                    $('#treeview').treeview({
                        color: "#252525",
                        showBorder: false,
                        data: $scope.traceDetailsTree,
                        levels: 99,
                        showTags: true,
                        onNodeSelected: function (event, data) {
                            changeShowingDetail(data.metaDetail);
                        }
                    });

                });
            }
        );

        function buildTree(data) {
            // 构建 spanId 到节点的映射
            var nodeMap = new Map();
            data.forEach(function (item) {
                nodeMap.set(item.logDTO.spanId, item);
            });

            // 构建图的根节点列表
            var roots = [];

            data.forEach(function (item) {
                var log = item.logDTO;
                var parentSpanId = log.parentSpanId;

                if (parentSpanId && nodeMap.has(parentSpanId)) {
                    var parent = nodeMap.get(parentSpanId);
                    if (!parent.children) {
                        parent.children = [];
                    }
                    parent.children.push(item);
                } else {
                    roots.push(item);
                }
            });

            // 递归生成 Treeview 格式的节点
            function buildTreeNode(node) {
                var log = node.logDTO;
                var treeNode = {
                    text: log.opName,
                    nodes: [],
                    metaDetail: node
                };
                if (node.children) {
                    node.children.forEach(function (child) {
                        treeNode.nodes.push(buildTreeNode(child));
                    });
                }
                if (treeNode.nodes.length === 0) {
                    delete treeNode.nodes;
                }
                return treeNode;
            }

            return roots.map(function (root) {
                return buildTreeNode(root);
            });
        }

        function changeShowingDetail(data) {
            $scope.showingDetail = data;
            refreshDataInfluenceEntities();
        }
    }

    function showRelatedDataInfluence(entityName, entityId, fieldName) {
        $scope.entityNameOfFindRelated = entityName;
        $scope.entityIdOfFindRelated = entityId;
        $scope.fieldNameOfFindRelated = fieldName;

        if (entityId === 'AnyMatched') {
            return;
        }

        AuditLogService.find_dataInfluences_by_field(
            $scope.entityNameOfFindRelated,
            $scope.entityIdOfFindRelated,
            $scope.fieldNameOfFindRelated,
            $scope.relatedDataInfluencePage,
            RelatedDataInfluencePageSize
        ).then(function (result) {
            if (!result || result.length < RelatedDataInfluencePageSize) {
                $scope.relatedDataInfluenceHasLoadAll = true;
                $scope.relatedDataInfluences = result;
                return;
            }
            if (result.length === 0) {
                return;
            }
            $scope.relatedDataInfluenceHasLoadAll = false;
            $scope.relatedDataInfluences = result;
        });
    }

    function findMoreRelatedDataInfluence() {
        $scope.relatedDataInfluencePage = $scope.relatedDataInfluencePage + 1;
        AuditLogService.find_dataInfluences_by_field(
            $scope.entityNameOfFindRelated,
            $scope.entityIdOfFindRelated,
            $scope.fieldNameOfFindRelated,
            $scope.relatedDataInfluencePage,
            RelatedDataInfluencePageSize
        ).then(function (result) {
            if (!result || result.length < RelatedDataInfluencePageSize) {
                $scope.relatedDataInfluenceHasLoadAll = true;
            }
            if (result.length === 0) {
                return;
            }
            $scope.relatedDataInfluences = $scope.relatedDataInfluences.concat(result);

        });
    }

    function refreshDataInfluenceEntities() {
        var entityMap = new Map();
        $scope.showingDetail.dataInfluenceDTOList.forEach(function (dto) {
            var key = {
                name: dto.influenceEntityName,
                id: dto.influenceEntityId
            };
            var keyString = JSON.stringify(key);
            var value = {
                name: dto.influenceEntityName,
                id: dto.influenceEntityId,
                dtoList: []
            };
            if (!entityMap.has(keyString)) {
                entityMap.set(keyString, value);
            }
            entityMap.get(keyString).dtoList.push(dto);
        });
        $scope.dataInfluenceEntities = Array.from(entityMap);
    }

    function showText(text) {
        $scope.text = text;
        AppUtil.showModal("#showTextModal");
    }
}