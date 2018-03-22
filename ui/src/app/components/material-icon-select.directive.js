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
import './material-icon-select.scss';

import MaterialIconsDialogController from './material-icons-dialog.controller';

/* eslint-disable import/no-unresolved, import/default */

import materialIconSelectTemplate from './material-icon-select.tpl.html';
import materialIconsDialogTemplate from './material-icons-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.materialIconSelect', [])
    .controller('MaterialIconsDialogController', MaterialIconsDialogController)
    .directive('tbMaterialIconSelect', MaterialIconSelect)
    .name;

/*@ngInject*/
function MaterialIconSelect($compile, $templateCache, $document, $mdDialog) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(materialIconSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.icon = null;

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.icon);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.icon = ngModelCtrl.$viewValue;
            }
            if (!scope.icon || !scope.icon.length) {
                scope.icon = 'more_horiz';
            }
        }

        scope.$watch('icon', function () {
            scope.updateView();
        });

        scope.openIconDialog = function($event) {
            if ($event) {
                $event.stopPropagation();
            }
            $mdDialog.show({
                controller: 'MaterialIconsDialogController',
                controllerAs: 'vm',
                templateUrl: materialIconsDialogTemplate,
                parent: angular.element($document[0].body),
                locals: {icon: scope.icon},
                skipHide: true,
                fullscreen: true,
                targetEvent: $event
            }).then(function (icon) {
                scope.icon = icon;
            });
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            tbRequired: '=?',
        }
    };
}
