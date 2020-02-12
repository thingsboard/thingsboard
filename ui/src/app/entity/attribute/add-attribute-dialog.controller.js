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

import attributeDialogEditJsonTemplate from './attribute-dialog-edit-json.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import AttributeDialogEditJsonController from './attribute-dialog-edit-json.controller';

/*@ngInject*/
export default function AddAttributeDialogController($scope, $mdDialog, $document, $q, types, attributeService, entityType, entityId, attributeScope) {

    let vm = this;

    vm.attribute = {};

    vm.valueTypes = types.valueType;

    vm.valueType = types.valueType.string;

    vm.add = add;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function add() {
        $scope.theForm.$setPristine();
        if (vm.valueType===types.valueType.json) {
            vm.attribute.value = angular.fromJson(vm.attribute.value);
        }
        attributeService.saveEntityAttributes(entityType, entityId, attributeScope, [vm.attribute]).then(
            function success() {
                $mdDialog.hide();
            }
        );
    }

    $scope.$watch('vm.valueType', function() {
        if (vm.valueType === types.valueType.boolean) {
            vm.attribute.value = false;
        }
        else if (vm.valueType === types.valueType.json) {
            vm.attribute.value = null;
            vm.attribute.viewJsonStr =  null;
        } else {
            vm.attribute.value = null;
        }
    });

    vm.addJson = ($event, jsonValue, readOnly) => {
        showJsonDialog($event, jsonValue, readOnly).then((response) => {
            if (response || response === null) {
                vm.attribute.value = response;
                if (response === null) {
                    vm.attribute.viewJsonStr = null;
                } else {
                    vm.attribute.viewJsonStr = angular.toJson(vm.attribute.value);
                }
            }
        })
    };

    function showJsonDialog($event, jsonValue, readOnly) {
        if ($event) {
            $event.stopPropagation();
        }
        const promis = $mdDialog.show({
            controller: AttributeDialogEditJsonController,
            controllerAs: 'vm',
            templateUrl: attributeDialogEditJsonTemplate,
            parent: angular.element($document[0].body),
            locals: {
                jsonValue: jsonValue,
                readOnly: readOnly
            },
            targetEvent: $event,
            fullscreen: true,
            multiple: true,
        });

        return promis;
    }
}
