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
/* eslint-disable import/no-unresolved, import/default */

import componentTemplate from './component.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ComponentDirective($compile, $templateCache, $document, $mdDialog, componentDialogService, componentDescriptorService) {

    var linker = function (scope, element) {

        var template = $templateCache.get(componentTemplate);

        element.html(template);

        scope.componentTypeName = '';

        scope.loadComponentTypeName = function () {
            componentDescriptorService.getComponentDescriptorByClazz(scope.component.clazz).then(
                function success(component) {
                    scope.componentTypeName = component.name;
                },
                function fail() {}
            );
        }

        scope.$watch('component', function(newVal) {
                if (newVal) {
                    scope.loadComponentTypeName();
                }
            }
        );

        scope.openComponent = function($event) {
            componentDialogService.openComponentDialog($event, false,
                scope.readOnly, scope.title, scope.type, scope.pluginClazz,
                angular.copy(scope.component)).then(
                    function success(component) {
                        scope.component = component;
                    },
                    function fail() {}
            );
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            component: '=',
            type: '=',
            pluginClazz: '=',
            title: '@',
            readOnly: '=',
            onRemoveComponent: '&'
        }
    };
}
