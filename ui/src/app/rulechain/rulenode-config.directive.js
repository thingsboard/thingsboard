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

/* eslint-disable import/no-unresolved, import/default */

import ruleNodeConfigTemplate from './rulenode-config.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleNodeConfigDirective($compile, $templateCache, $injector, $translate) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(ruleNodeConfigTemplate);
        element.html(template);

        scope.$watch('configuration', function (newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        });

        ngModelCtrl.$render = function () {
            scope.configuration = ngModelCtrl.$viewValue;
        };

        scope.useDefinedDirective = function() {
            return scope.nodeDefinition &&
                scope.nodeDefinition.configDirective && !scope.definedDirectiveError;
        };

        scope.$watch('nodeDefinition', () => {
            if (scope.nodeDefinition) {
                validateDefinedDirective();
            }
        });

        function validateDefinedDirective() {
            if (scope.nodeDefinition.uiResourceLoadError && scope.nodeDefinition.uiResourceLoadError.length) {
                scope.definedDirectiveError = scope.nodeDefinition.uiResourceLoadError;
            } else {
                var definedDirective = scope.nodeDefinition.configDirective;
                if (definedDirective && definedDirective.length) {
                    if (!$injector.has(definedDirective + 'Directive')) {
                        scope.definedDirectiveError = $translate.instant('rulenode.directive-is-not-loaded', {directiveName: definedDirective});
                    }
                }
            }
        }

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            ruleNodeId:'=',
            nodeDefinition:'=',
            required:'=ngRequired',
            readonly:'=ngReadonly'
        },
        link: linker
    };

}
