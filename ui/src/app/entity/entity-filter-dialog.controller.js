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

/*@ngInject*/
export default function EntityFilterDialogController($scope, $mdDialog, $q, entityService, types, isAdd, allowedEntityTypes, filter) {

    var vm = this;

    vm.types = types;
    vm.isAdd = isAdd;
    vm.allowedEntityTypes = allowedEntityTypes;
    vm.filter = filter;

    vm.cancel = cancel;
    vm.save = save;

    $scope.$watch('vm.filter.type', function (newType, prevType) {
        if (newType && newType != prevType) {
            updateFilter();
        }
    });

    $scope.$watch('theForm.$pristine', function() {
        if ($scope.theForm && !$scope.theForm.$pristine) {
            $scope.theForm.$setValidity('entityFilter', true);
        }
    });

    function updateFilter() {
        var filter = {};
        filter.type = vm.filter.type;
        filter.resolveMultiple = vm.filter.resolveMultiple;
        switch (filter.type) {
            case types.aliasFilterType.entityList.value:
                filter.entityType = null;
                filter.entityList = [];
                filter.stateEntity = false;
                break;
            case types.aliasFilterType.entityName.value:
                filter.entityType = null;
                filter.entityNameFilter = '';
                break;
            //TODO:
        }
        vm.filter = filter;
    }

    function validate() {
        var deferred = $q.defer();
        var validationResult = {
            entity: null,
            stateEntity: false
        }
        entityService.resolveAliasFilter(vm.filter).then(
            function success(result) {
                validationResult.stateEntity = result.stateEntity;
                var entities = result.entities;
                if (entities.length) {
                    validationResult.entity = entities[0];
                }
                deferred.resolve(validationResult);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        $scope.theForm.$setPristine();
        validate().then(
            function success(validationResult) {
                $mdDialog.hide({
                    filter: vm.filter,
                    entity: validationResult.entity,
                    stateEntity: validationResult.stateEntity
                });
            },
            function fail() {
                $scope.theForm.$setValidity('entityFilter', false);
            }
        )
    }

}

