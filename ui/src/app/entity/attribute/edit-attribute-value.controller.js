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
export default function EditAttributeValueController($scope, $q, $element, types, attributeValue, save) {

    $scope.valueTypes = types.valueType;

    $scope.model = {};

    $scope.model.value = attributeValue;

    if ($scope.model.value === true || $scope.model.value === false) {
        $scope.valueType = types.valueType.boolean;
    } else if (angular.isNumber($scope.model.value)) {
        if ($scope.model.value.toString().indexOf('.') == -1) {
            $scope.valueType = types.valueType.integer;
        } else {
            $scope.valueType = types.valueType.double;
        }
    } else {
        $scope.valueType = types.valueType.string;
    }

    $scope.submit = submit;
    $scope.dismiss = dismiss;

    function dismiss() {
        $element.remove();
    }

    function update() {
        if($scope.editDialog.$invalid) {
            return $q.reject();
        }

        if(angular.isFunction(save)) {
            return $q.when(save($scope.model));
        }

        return $q.resolve();
    }

    function submit() {
        update().then(function () {
            $scope.dismiss();
        });
    }

    $scope.$watch('valueType', function(newVal, prevVal) {
        if (newVal != prevVal) {
            if ($scope.valueType === types.valueType.boolean) {
                $scope.model.value = false;
            } else {
                $scope.model.value = null;
            }
        }
    });
}
