/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
import 'angular-material-data-table/dist/md-data-table.min.css';
import './relation-table.scss';

/* eslint-disable import/no-unresolved, import/default */

import relationTableTemplate from './relation-table.tpl.html';
import addRelationTemplate from './add-relation-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import AddRelationController from './add-relation-dialog.controller';

/*@ngInject*/
export default function RelationTable() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            entityId: '=',
            entityType: '@'
        },
        controller: RelationTableController,
        controllerAs: 'vm',
        templateUrl: relationTableTemplate
    };
}

/*@ngInject*/
function RelationTableController($scope, $q, $mdDialog, $document, $translate, $filter, utils, types, entityRelationService) {

    let vm = this;

    vm.relations = [];
    vm.relationsCount = 0;
    vm.allRelations = [];
    vm.selectedRelations = [];

    vm.query = {
        order: 'typeName',
        limit: 5,
        page: 1,
        search: null
    };

    vm.enterFilterMode = enterFilterMode;
    vm.exitFilterMode = exitFilterMode;
    vm.onReorder = onReorder;
    vm.onPaginate = onPaginate;
    vm.addRelation = addRelation;
    vm.editRelation = editRelation;
    vm.deleteRelation = deleteRelation;
    vm.deleteRelations = deleteRelations;
    vm.reloadRelations = reloadRelations;
    vm.updateRelations = updateRelations;


    $scope.$watch("vm.entityId", function(newVal, prevVal) {
        if (newVal && !angular.equals(newVal, prevVal)) {
            reloadRelations();
        }
    });

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateRelations();
        }
    });

    function enterFilterMode () {
        vm.query.search = '';
    }

    function exitFilterMode () {
        vm.query.search = null;
        updateRelations();
    }

    function onReorder () {
        updateRelations();
    }

    function onPaginate () {
        updateRelations();
    }

    function addRelation($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var from = {
            id: vm.entityId,
            entityType: vm.entityType
        };
        $mdDialog.show({
            controller: AddRelationController,
            controllerAs: 'vm',
            templateUrl: addRelationTemplate,
            parent: angular.element($document[0].body),
            locals: { from: from },
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
            reloadRelations();
        }, function () {
        });
    }

    function editRelation($event, /*relation*/) {
        if ($event) {
            $event.stopPropagation();
        }
        //TODO:
    }

    function deleteRelation($event, /*relation*/) {
        if ($event) {
            $event.stopPropagation();
        }
        //TODO:
    }

    function deleteRelations($event) {
        if ($event) {
            $event.stopPropagation();
        }
        //TODO:
    }

    function reloadRelations () {
        vm.allRelations.length = 0;
        vm.relations.length = 0;
        vm.relationsPromise = entityRelationService.findInfoByFrom(vm.entityId, vm.entityType);
        vm.relationsPromise.then(
            function success(allRelations) {
                allRelations.forEach(function(relation) {
                    relation.typeName = $translate.instant('relation.relation-type.' + relation.type);
                    relation.toEntityTypeName = $translate.instant(utils.entityTypeName(relation.to.entityType));
                });
                vm.allRelations = allRelations;
                vm.selectedRelations = [];
                vm.updateRelations();
                vm.relationsPromise = null;
            },
            function fail() {
                vm.allRelations = [];
                vm.selectedRelations = [];
                vm.updateRelations();
                vm.relationsPromise = null;
            }
        )
    }

    function updateRelations () {
        vm.selectedRelations = [];
        var result = $filter('orderBy')(vm.allRelations, vm.query.order);
        if (vm.query.search != null) {
            result = $filter('filter')(result, {$: vm.query.search});
        }
        vm.relationsCount = result.length;
        var startIndex = vm.query.limit * (vm.query.page - 1);
        vm.relations = result.slice(startIndex, startIndex + vm.query.limit);
    }

}
