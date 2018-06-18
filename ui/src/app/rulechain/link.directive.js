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

import './link.scss';

/* eslint-disable import/no-unresolved, import/default */

import linkFieldsetTemplate from './link-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function LinkDirective($compile, $templateCache, $filter) {
    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(linkFieldsetTemplate);
        element.html(template);

        scope.selectedLabel = null;
        scope.labelSearchText = null;

        scope.ngModelCtrl = ngModelCtrl;

        var labelsList = [];

        scope.transformLinkLabelChip = function (chip) {
            var res = $filter('filter')(labelsList, {name: chip}, true);
            var result;
            if (res && res.length) {
                result = angular.copy(res[0]);
            } else {
                result = {
                    name: chip,
                    value: chip
                };
            }
            return result;
        };

        scope.labelsSearch = function (searchText) {
            var labels = searchText ? $filter('filter')(labelsList, {name: searchText}) : labelsList;
            return labels.map((label) => label.name);
        };

        scope.createLinkLabel = function (event, chipsId) {
            var chipsChild = angular.element(chipsId, element)[0].firstElementChild;
            var el = angular.element(chipsChild);
            var chipBuffer = el.scope().$mdChipsCtrl.getChipBuffer();
            event.preventDefault();
            event.stopPropagation();
            el.scope().$mdChipsCtrl.appendChip(chipBuffer.trim());
            el.scope().$mdChipsCtrl.resetChipBuffer();
        };


        ngModelCtrl.$render = function () {
            labelsList.length = 0;
            for (var label in scope.allowedLabels) {
                var linkLabel = {
                    name: scope.allowedLabels[label].name,
                    value: scope.allowedLabels[label].value
                };
                labelsList.push(linkLabel);
            }

            var link = ngModelCtrl.$viewValue;
            var labels = [];
            if (link && link.labels) {
                for (var i = 0; i < link.labels.length; i++) {
                    label = link.labels[i];
                    if (scope.allowedLabels[label]) {
                        labels.push(angular.copy(scope.allowedLabels[label]));
                    } else {
                        labels.push({
                            name: label,
                            value: label
                        });
                    }
                }
            }
            scope.labels = labels;
            scope.$watch('labels', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    updateLabels();
                }
            }, true);
        };

        function updateLabels() {
            if (ngModelCtrl.$viewValue) {
                var labels = [];
                for (var i = 0; i < scope.labels.length; i++) {
                    labels.push(scope.labels[i].value);
                }
                ngModelCtrl.$viewValue.labels = labels;
                ngModelCtrl.$viewValue.label = labels.join(' / ');
                updateValidity();
            }
        }

        function updateValidity() {
            var valid = ngModelCtrl.$viewValue.labels &&
            ngModelCtrl.$viewValue.labels.length ? true : false;
            ngModelCtrl.$setValidity('linkLabels', valid);
        }

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            allowedLabels: '=',
            allowCustom: '=',
            isEdit: '=',
            isReadOnly: '='
        }
    };
}
