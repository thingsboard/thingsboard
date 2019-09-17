/*
 * Copyright © 2016-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* eslint-disable import/no-unresolved, import/default */

import addEdgeTemplate from './add-edge.tpl.html';
import edgeCard from './edge-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function EdgeCardController(types) {

    var vm = this;

    vm.types = types;
}


/*@ngInject*/
export function EdgeController($rootScope, userService, edgeService, $state, $stateParams,
                               $document, $mdDialog, $q, $translate, types, securityTypes, userPermissionsService) {

    var edgeActionsList = [];

    var edgeGroupActionsList = [];

    var vm = this;

    vm.types = types;

    vm.edgeGridConfig = {

        resource: securityTypes.resource.edge,

        deleteItemTitleFunc: deleteEdgeTitle,
        deleteItemContentFunc: deleteEdgeText,
        deleteItemsTitleFunc: deleteEdgesTitle,
        deleteItemsActionTitleFunc: deleteEdgesActionTitle,
        deleteItemsContentFunc: deleteEdgesText,

        saveItemFunc: saveEdge,

        getItemTitleFunc: getEdgeTitle,

        itemCardController: 'EdgeCardController',
        itemCardTemplateUrl: edgeCard,
        parentCtl: vm,

        actionsList: edgeActionsList,
        groupActionsList: edgeGroupActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addEdgeTemplate,

        addItemText: function() { return $translate.instant('edge.add-edge-text') },
        noItemsText: function() { return $translate.instant('edge.no-edges-text') },
        itemDetailsText: function() { return $translate.instant('edge.edge-details') }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.edgeGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.edgeGridConfig.topIndex = $stateParams.topIndex;
    }

    initController();

    function initController() {
        var fetchEdgesFunction = function (pageLink, edgeType) {
            return edgeService.getEdges(pageLink, true, edgeType);
        };
        var deleteEdgeFunction = function (edgeId) {
            return edgeService.deleteEdge(edgeId);
        };
        var refreshEdgesParamsFunction = function() {
            return {"topIndex": vm.topIndex};
        };

        edgeActionsList.push(
            {
                onAction: function ($event, item) {
                    vm.grid.deleteItem($event, item);
                },
                name: function() { return $translate.instant('action.delete') },
                details: function() { return $translate.instant('edge.delete') },
                icon: "delete",
                isEnabled: function() {
                    return userPermissionsService.hasGenericPermission(securityTypes.resource.edge, securityTypes.operation.delete);
                }
            }
        );

        edgeGroupActionsList.push(
            {
                onAction: function ($event) {
                    vm.grid.deleteItems($event);
                },
                name: function() { return $translate.instant('edge.delete-edges') },
                details: deleteEdgesActionTitle,
                icon: "delete"
            }
        );
        vm.edgeGridConfig.refreshParamsFunc = refreshEdgesParamsFunction;
        vm.edgeGridConfig.fetchItemsFunc = fetchEdgesFunction;
        vm.edgeGridConfig.deleteItemFunc = deleteEdgeFunction;

    }

    function deleteEdgeTitle(edge) {
        return $translate.instant('edge.delete-edge-title', {edgeName: edge.name});
    }

    function deleteEdgeText() {
        return $translate.instant('edge.delete-edge-text');
    }

    function deleteEdgesTitle(selectedCount) {
        return $translate.instant('edge.delete-edges-title', {count: selectedCount}, 'messageformat');
    }

    function deleteEdgesActionTitle(selectedCount) {
        return $translate.instant('edge.delete-edges-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteEdgesText () {
        return $translate.instant('edge.delete-edges-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function getEdgeTitle(edge) {
        return edge ? edge.name : '';
    }

    function saveEdge(edge) {
        var deferred = $q.defer();
        edgeService.saveEdge(edge).then(
            function success(savedEdge) {
                $rootScope.$broadcast('edgeSaved');
                deferred.resolve(savedEdge);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }
}
