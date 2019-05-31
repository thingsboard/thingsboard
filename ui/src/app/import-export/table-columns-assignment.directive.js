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
// import './timeinterval.scss';

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
    vm.entityField = {};

    switch (vm.entityType) {
        case types.entityType.device:
            vm.columnTypes.sharedAttribute = types.entityGroup.columnType.sharedAttribute;
            vm.columnTypes.serverAttribute = types.entityGroup.columnType.serverAttribute;
            vm.columnTypes.timeseries = types.entityGroup.columnType.timeseries;
            vm.columnTypes.entityField = types.entityGroup.columnType.entityField;
            vm.columnTypes.accessToken = types.entityGroup.columnType.accessToken;
            break;
    }

    vm.entityField.name = types.entityGroup.entityField.name;

    switch (vm.entityType) {
        case types.entityType.device:
            vm.entityField.type = types.entityGroup.entityField.type;
            // vm.entityField.assigned_customer = types.entityGroup.entityField.assigned_customer;
            break;
    }

    $scope.$watch('vm.columns', function(newVal){
        if (newVal) {
            var isSelectName = false;
            var isSelectType = false;
            var isSelectCredentials = false;
            for (var i = 0; i < newVal.length; i++) {
                if (newVal[i].type === types.entityGroup.columnType.entityField.value &&
                    newVal[i].key === types.entityGroup.entityField.name.value) {
                    isSelectName = true;
                }
                if (newVal[i].type === types.entityGroup.columnType.entityField.value &&
                    newVal[i].key === types.entityGroup.entityField.type.value) {
                    isSelectType = true;
                }
                if (newVal[i].type === types.entityGroup.columnType.accessToken.value) {
                    isSelectCredentials = true;
                }
            }
            $timeout(function () {
                vm.entityField.name.disable = isSelectName;
                vm.entityField.type.disable = isSelectType;
                vm.columnTypes.accessToken.disable = isSelectCredentials;
            });
            if(isSelectName && isSelectType) {
                vm.theForm.$setDirty();
            } else {
                vm.theForm.$setPristine();
            }
        }
    }, true);
}
