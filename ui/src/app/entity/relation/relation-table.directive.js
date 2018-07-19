/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
import relationTemplate from './relation-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import RelationController from './relation-dialog.controller';

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
function RelationTableController($scope, $q, $mdDialog, $document, $translate, $filter, $timeout, utils, types, entityRelationService) {

    let vm = this;

    vm.types = types;

    vm.direction = vm.types.entitySearchDirection.from;

    vm.relations = [];
    vm.relationsCount = 0;
    vm.allRelations = [];
    vm.selectedRelations = [];

    vm.query = {
        order: 'type',
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

    $scope.$watch("vm.direction", function(newVal, prevVal) {
        if (newVal && !angular.equals(newVal, prevVal)) {
            reloadRelations();
        }
    });

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateRelations();
        }
    });

    function enterFilterMode (event) {
        let $button = angular.element(event.currentTarget);
        let $toolbarsContainer = $button.closest('.toolbarsContainer');

        vm.query.search = '';

        $timeout(()=>{
            $toolbarsContainer.find('.searchInput').focus();
        })
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
        openRelationDialog($event);
    }

    function editRelation($event, relation) {
        if ($event) {
            $event.stopPropagation();
        }
        openRelationDialog($event, relation);
    }

    function openRelationDialog($event, relation) {
        if ($event) {
            $event.stopPropagation();
        }
        var isAdd = false;
        if (!relation) {
            isAdd = true;
            var entityId = {
                id: vm.entityId,
                entityType: vm.entityType
            };
            relation = {};
            if (vm.direction == vm.types.entitySearchDirection.from) {
                relation.from = entityId;
            } else {
                relation.to = entityId;
            }
        }
        var onShowingCallback = {
            onShowing: function(){}
        }
        $mdDialog.show({
            controller: RelationController,
            controllerAs: 'vm',
            templateUrl: relationTemplate,
            parent: angular.element($document[0].body),
            locals: { isAdd: isAdd,
                      direction: vm.direction,
                      relation: relation,
                      showingCallback: onShowingCallback},
            targetEvent: $event,
            fullscreen: true,
            skipHide: true,
            onShowing: function(scope, element) {
                onShowingCallback.onShowing(scope, element);
            }
        }).then(function () {
            reloadRelations();
        }, function () {
        });
    }

    function deleteRelation($event, relation) {
        if ($event) {
            $event.stopPropagation();
        }
        if (relation) {
            var title;
            var content;
            if (vm.direction == vm.types.entitySearchDirection.from) {
                title = $translate.instant('relation.delete-to-relation-title', {entityName: relation.toName});
                content = $translate.instant('relation.delete-to-relation-text', {entityName: relation.toName});
            } else {
                title = $translate.instant('relation.delete-from-relation-title', {entityName: relation.fromName});
                content = $translate.instant('relation.delete-from-relation-text', {entityName: relation.fromName});
            }

            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title(title)
                .htmlContent(content)
                .ariaLabel(title)
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(function () {
                entityRelationService.deleteRelation(
                    relation.from.id,
                    relation.from.entityType,
                    relation.type,
                    relation.to.id,
                    relation.to.entityType).then(
                    function success() {
                        reloadRelations();
                    }
                );
            });
        }
    }

    function deleteRelations($event) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.selectedRelations && vm.selectedRelations.length > 0) {
            var title;
            var content;
            if (vm.direction == vm.types.entitySearchDirection.from) {
                title = $translate.instant('relation.delete-to-relations-title', {count: vm.selectedRelations.length}, 'messageformat');
                content = $translate.instant('relation.delete-to-relations-text');
            } else {
                title = $translate.instant('relation.delete-from-relations-title', {count: vm.selectedRelations.length}, 'messageformat');
                content = $translate.instant('relation.delete-from-relations-text');
            }
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title(title)
                .htmlContent(content)
                .ariaLabel(title)
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(function () {
                var tasks = [];
                for (var i=0;i<vm.selectedRelations.length;i++) {
                    var relation = vm.selectedRelations[i];
                    tasks.push( entityRelationService.deleteRelation(
                        relation.from.id,
                        relation.from.entityType,
                        relation.type,
                        relation.to.id,
                        relation.to.entityType));
                }
                $q.all(tasks).then(function () {
                    reloadRelations();
                });

            });
        }
    }

    function reloadRelations () {
        vm.allRelations.length = 0;
        vm.relations.length = 0;
        vm.relationsPromise;
        if (vm.direction == vm.types.entitySearchDirection.from) {
            vm.relationsPromise = entityRelationService.findInfoByFrom(vm.entityId, vm.entityType);
        } else {
            vm.relationsPromise = entityRelationService.findInfoByTo(vm.entityId, vm.entityType);
        }
        vm.relationsPromise.then(
            function success(allRelations) {
                allRelations.forEach(function(relation) {
                    if (vm.direction == vm.types.entitySearchDirection.from) {
                        relation.toEntityTypeName = $translate.instant(types.entityTypeTranslations[relation.to.entityType].type);
                    } else {
                        relation.fromEntityTypeName = $translate.instant(types.entityTypeTranslations[relation.from.entityType].type);
                    }
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
