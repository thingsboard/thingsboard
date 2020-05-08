/*
 * Copyright © 2016-2020 The Thingsboard Authors
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

import tableColumnsAssignment from './table-columns-assignment.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function TableColumnsAssignment() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            theForm: '=?',
            columns: '=',
            entityType: '=',
        },
        templateUrl: tableColumnsAssignment,
        controller: TableColumnsAssignmentController,
        controllerAs: 'vm'
    };
}

/*@ngInject*/
function TableColumnsAssignmentController($scope, types, $timeout) {
    var vm = this;

    vm.columnTypes = {};

    vm.columnTypes.name = types.importEntityColumnType.name;
    vm.columnTypes.type = types.importEntityColumnType.type;
    vm.columnTypes.label = types.importEntityColumnType.label;
    vm.columnTypes.description = types.importEntityColumnType.description;

    switch (vm.entityType) {
        case types.entityType.device:
            vm.columnTypes.sharedAttribute = types.importEntityColumnType.sharedAttribute;
            vm.columnTypes.serverAttribute = types.importEntityColumnType.serverAttribute;
            vm.columnTypes.timeseries = types.importEntityColumnType.timeseries;
            vm.columnTypes.accessToken = types.importEntityColumnType.accessToken;
            vm.columnTypes.gateway = types.importEntityColumnType.isGateway;
            break;
        case types.entityType.asset:
            vm.columnTypes.serverAttribute = types.importEntityColumnType.serverAttribute;
            vm.columnTypes.timeseries = types.importEntityColumnType.timeseries;
            break;
    }

    $scope.isColumnTypeDiffers = function(columnType) {
       return columnType !== types.importEntityColumnType.name.value &&
            columnType !== types.importEntityColumnType.type.value &&
            columnType !== types.importEntityColumnType.label.value &&
            columnType !== types.importEntityColumnType.accessToken.value&&
            columnType !== types.importEntityColumnType.isGateway.value&&
            columnType !== types.importEntityColumnType.description.value;
    };

    $scope.$watch('vm.columns', function(newVal){
        if (newVal) {
            var isSelectName = false;
            var isSelectType = false;
            var isSelectLabel = false;
            var isSelectCredentials = false;
            var isSelectGateway = false;
            var isSelectDescription = false;
            for (var i = 0; i < newVal.length; i++) {
                switch (newVal[i].type) {
                    case types.importEntityColumnType.name.value:
                        isSelectName = true;
                        break;
                    case types.importEntityColumnType.type.value:
                        isSelectType = true;
                        break;
                    case types.importEntityColumnType.label.value:
                        isSelectLabel = true;
                        break;
                    case types.importEntityColumnType.accessToken.value:
                        isSelectCredentials = true;
                        break;
                    case types.importEntityColumnType.isGateway.value:
                        isSelectGateway = true;
                        break;
                    case types.importEntityColumnType.description.value:
                        isSelectDescription = true;
                }
            }
            if (isSelectName && isSelectType) {
                vm.theForm.$setDirty();
            } else {
                vm.theForm.$setPristine();
            }
            $timeout(function () {
                vm.columnTypes.name.disable = isSelectName;
                vm.columnTypes.type.disable = isSelectType;
                vm.columnTypes.label.disable = isSelectLabel;
                vm.columnTypes.gateway.disable = isSelectGateway;
                vm.columnTypes.description.disable = isSelectDescription;
                if (angular.isDefined(vm.columnTypes.accessToken)) {
                    vm.columnTypes.accessToken.disable = isSelectCredentials;
                }
            });
        }
    }, true);
}
