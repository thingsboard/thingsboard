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
/* eslint-enable import/no-unresolved, import/default */

import AttributeDialogEditJsonController from "./attribute-dialog-edit-json.controller";
import attributeDialogEditJsonTemplate from "./attribute-dialog-edit-json.tpl.html";

/*@ngInject*/
export default function EditAttributeValueController($scope, $mdDialog, $document, $q, $element, types, attributeValue, save) {

    $scope.valueTypes = types.valueType;

    $scope.model = {};

    $scope.model.value = attributeValue;

    $scope.editJson = editJson;

    if ($scope.model.value === true || $scope.model.value === false) {
        $scope.valueType = types.valueType.boolean;
    } else if (angular.isNumber($scope.model.value)) {
        if ($scope.model.value.toString().indexOf('.') == -1) {
            $scope.valueType = types.valueType.integer;
        } else {
            $scope.valueType = types.valueType.double;
        }
    } else if (angular.isObject($scope.model.value)) {
        $scope.model.viewJsonStr = angular.toJson($scope.model.value);
        $scope.valueType = types.valueType.json;
    } else {
        $scope.valueType = types.valueType.string;
    }

    $scope.submit = submit;
    $scope.dismiss = dismiss;

    function dismiss() {
        $element.remove();
    }

    function update() {
        if ($scope.editDialog.$invalid) {
            return $q.reject();
        }
        if (angular.isFunction(save)) {
            return $q.when(save($scope.model));
        }
        return $q.resolve();
    }

    function submit() {
        update().then(function () {
            $scope.dismiss();
        });
    }


    $scope.$watch('valueType', function (newVal, prevVal) {
        if (newVal != prevVal) {
            if ($scope.valueType === types.valueType.boolean) {
                $scope.model.value = false;
            } else {
                $scope.model.value = null;
            }
        }
    });

    function editJson($event, jsonValue, readOnly) {
        showJsonDialog($event, jsonValue, readOnly).then((response) => {
            $scope.hideDialog = false;
            if (response || response === null) {
                if (!angular.equals(response, $scope.model.value)) {
                    $scope.editDialog.$setDirty();
                }

                if (response === null) {
                    $scope.model.viewJsonStr = null;
                    $scope.model.value = null;
                } else {
                    $scope.model.value = angular.fromJson(response);
                    $scope.model.viewJsonStr = response;
                }
            }
        })
    }

    function showJsonDialog($event, jsonValue, readOnly) {

        if (jsonValue) {
            jsonValue = angular.toJson(angular.fromJson(jsonValue));
        }
        if ($event) {
            $event.stopPropagation();
        }
        $scope.hideDialog = true;
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
